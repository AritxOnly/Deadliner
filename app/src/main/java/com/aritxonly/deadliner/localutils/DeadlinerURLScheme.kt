package com.aritxonly.deadliner.localutils

import android.net.Uri
import android.util.Base64
import com.aritxonly.deadliner.model.DDLItem
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.util.zip.Deflater
import java.util.zip.Inflater
import androidx.core.net.toUri

/**
 * 口令派生模式 Deadliner URL 工具（AES-GCM + Deflate + Base64URL）。
 * URL 形如：
 * deadliner://share?v=1&k=1&m=pass&s=<salt_b64url>&i=<iters>&d=<payload>
 */
object DeadlinerURLScheme {
    const val DEADLINER_URL_SCHEME_PREFIX = "https://www.aritxonly.top/deadliner/share"
    const val DEADLINER_URL_SCHEME_PREFIX_LEGACY = "deadliner://share"

    // —— 协议常量 —— //
    private const val HTTPS_SCHEME = "https"
    private const val HTTPS_HOST = "www.aritxonly.top"
    private const val HTTPS_PATH_PREFIX = "/deadliner/share"

    private const val LEGACY_SCHEME = "deadliner"
    private const val LEGACY_AUTHORITY = "share"

    private const val PARAM_V = "v"
    private const val PARAM_K = "k"
    private const val PARAM_M = "m"
    private const val PARAM_S = "s"
    private const val PARAM_I = "i"
    private const val PARAM_D = "d"

    private const val PROTO_VERSION: Int = 1
    private const val DEFAULT_KID: Int = 1

    private const val AES_KEY_LEN = 32      // 256-bit
    private const val GCM_IV_LEN = 12       // 96-bit
    private const val GCM_TAG_BITS = 128

    private const val DEFAULT_ITERS = 200_000
    private const val DEFAULT_SALT_BYTES = 16

    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
    private val rng = SecureRandom()

    // —— 公开 API（口令派生模式）—— //

    /** 用口令派生密钥并生成 deadliner:// URL；跨设备共享仅需同一口令 */
    fun encodeWithPassphrase(
        item: DDLItem,
        passphrase: CharArray,
        iterations: Int = DEFAULT_ITERS,
        saltBytes: Int = DEFAULT_SALT_BYTES,
        kid: Int = DEFAULT_KID,
    ): String {
        val salt = genSalt(saltBytes)
        val key = deriveKey(passphrase, salt, iterations)

        val json = gson.toJson(item).encodeToByteArray()
        val compressed = deflate(json)

        val saltB64 = b64urlEncode(salt)
        val aad = "v=$PROTO_VERSION&k=$kid&m=pass&s=$saltB64&i=$iterations".encodeToByteArray()

        val env = aesGcmEncrypt(compressed, key, aad)
        val token = b64urlEncode(env)
        key.fill(0)

        // 构造 query 部分
        val queryUri = Uri.Builder()
            .appendQueryParameter(PARAM_V, PROTO_VERSION.toString())
            .appendQueryParameter(PARAM_K, kid.toString())
            .appendQueryParameter(PARAM_M, "pass")
            .appendQueryParameter(PARAM_S, saltB64)
            .appendQueryParameter(PARAM_I, iterations.toString())
            .appendQueryParameter(PARAM_D, token)
            .build()

        // queryUri.toString() 形如 "?v=1&k=1&m=pass&..."
        return DEADLINER_URL_SCHEME_PREFIX + queryUri.toString()
    }

    /** 使用口令解析 URL 并还原 DDLItem */
    fun decodeWithPassphrase(
        url: String,
        passphrase: CharArray,
    ): DDLItem {
        val uri = url.toUri()

        val isHttpsMatch =
            uri.scheme == HTTPS_SCHEME &&
                    uri.host == HTTPS_HOST &&
                    (uri.path ?: "").startsWith(HTTPS_PATH_PREFIX)

        val isLegacyMatch =
            uri.scheme == LEGACY_SCHEME &&
                    uri.authority == LEGACY_AUTHORITY

        require(isHttpsMatch || isLegacyMatch) { "invalid scheme/host/path: $uri" }

        val v = uri.getQueryParameter(PARAM_V)?.toIntOrNull() ?: error("missing v")
        require(v == PROTO_VERSION) { "unsupported version $v" }

        val mode = uri.getQueryParameter(PARAM_M) ?: error("missing m")
        require(mode == "pass") { "not passphrase mode" }

        val kid = uri.getQueryParameter(PARAM_K)?.toIntOrNull() ?: DEFAULT_KID
        val saltB64 = uri.getQueryParameter(PARAM_S) ?: error("missing s")
        val iters = uri.getQueryParameter(PARAM_I)?.toIntOrNull() ?: DEFAULT_ITERS
        val token = uri.getQueryParameter(PARAM_D) ?: error("missing d")

        val salt = b64urlDecode(saltB64)
        val key = deriveKey(passphrase, salt, iters)

        val aad = "v=$v&k=$kid&m=pass&s=$saltB64&i=$iters".encodeToByteArray()
        val env = b64urlDecode(token)
        val compressed = aesGcmDecrypt(env, key, aad)
        val json = inflate(compressed).decodeToString()

        key.fill(0)
        return gson.fromJson(json, DDLItem::class.java)
    }

    // —— PBKDF2（HMAC-SHA256）—— //

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        require(iterations >= 50_000) { "iterations too small" }
        val spec = PBEKeySpec(passphrase, salt, iterations, AES_KEY_LEN * 8)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    private fun genSalt(n: Int): ByteArray = ByteArray(n).apply { rng.nextBytes(this) }

    // —— 压缩 / 解压（Deflate，nowrap=true）—— //

    private fun deflate(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(input)
        deflater.finish()
        val buf = ByteArray(input.size + 256)
        var total = 0
        while (!deflater.finished()) {
            val n = deflater.deflate(buf, total, buf.size - total)
            total += n
            if (total == buf.size) return deflateGrow(input)
        }
        return buf.copyOf(total)
    }

    private fun deflateGrow(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(input)
        deflater.finish()
        val out = ArrayList<Byte>()
        val buf = ByteArray(1024)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            for (i in 0 until n) out.add(buf[i])
        }
        return out.toByteArray()
    }

    private fun inflate(input: ByteArray): ByteArray {
        val inflater = Inflater(true)
        inflater.setInput(input)
        val out = ByteArray(input.size * 6 + 256)
        var total = 0
        while (!inflater.finished()) {
            val n = inflater.inflate(out, total, out.size - total)
            total += n
            if (n == 0 && inflater.needsInput()) break
            if (total == out.size) return inflateGrow(input)
        }
        return out.copyOf(total)
    }

    private fun inflateGrow(input: ByteArray): ByteArray {
        val inflater = Inflater(true)
        inflater.setInput(input)
        val out = ArrayList<Byte>()
        val buf = ByteArray(1024)
        while (!inflater.finished()) {
            val n = inflater.inflate(buf)
            for (i in 0 until n) out.add(buf[i])
            if (n == 0 && inflater.needsInput()) break
        }
        return out.toByteArray()
    }

    // —— AES-GCM —— //

    private fun aesGcmEncrypt(plain: ByteArray, key: ByteArray, aad: ByteArray): ByteArray {
        require(key.size == AES_KEY_LEN) { "key must be 32B" }
        val iv = ByteArray(GCM_IV_LEN).also { rng.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(aad)
        val ct = cipher.doFinal(plain)
        // [version(1) | iv(12) | ct+tag]
        return ByteArray(1 + iv.size + ct.size).apply {
            this[0] = PROTO_VERSION.toByte()
            System.arraycopy(iv, 0, this, 1, iv.size)
            System.arraycopy(ct, 0, this, 1 + iv.size, ct.size)
        }
    }

    private fun aesGcmDecrypt(env: ByteArray, key: ByteArray, aad: ByteArray): ByteArray {
        require(key.size == AES_KEY_LEN) { "key must be 32B" }
        require(env.size > 1 + GCM_IV_LEN) { "envelope too short" }
        val ver = env[0].toInt() and 0xFF
        require(ver == PROTO_VERSION) { "envelope version mismatch" }
        val iv = env.copyOfRange(1, 1 + GCM_IV_LEN)
        val ct = env.copyOfRange(1 + GCM_IV_LEN, env.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(ct)
    }

    // —— Base64URL 无填充 —— //

    private fun b64urlEncode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun b64urlDecode(s: String): ByteArray =
        Base64.decode(s, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}