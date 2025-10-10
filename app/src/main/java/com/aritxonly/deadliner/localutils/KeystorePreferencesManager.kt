package com.aritxonly.deadliner.localutils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import androidx.core.content.edit

object KeystorePreferenceManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "deepseek_api_key_alias"
    private const val PREFS_NAME = "secure_prefs_keystore"
    private const val PREFS_KEY_CIPHERTEXT = "api_key_ciphertext"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /** 1. 在 Keystore 中生成一个 AES/GCM 密钥（仅生成一次） */
    fun createKeyIfNeeded() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // 建议：只在设备解锁时可用
                .setUserAuthenticationRequired(false)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    /** 2. 用 Keystore 中的密钥加密 plaintext，返回 base64(ciphertext||iv) */
    fun encryptAndStore(context: Context, plaintext: String) {
        createKeyIfNeeded()

        // 2.1 取出密钥
        val secretKey = (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        // 2.2 初始化 Cipher
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey)
        }
        // 2.3 执行加密
        val iv = cipher.iv                           // GCM 模式需要保存 IV
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // 2.4 将 iv + ciphertext 合并并 Base64 编码保存
        val combined = ByteBuffer.allocate(iv.size + ciphertext.size)
            .put(iv)
            .put(ciphertext)
            .array()
        val base64 = Base64.encodeToString(combined, Base64.NO_WRAP)

        // 2.5 存入普通 SharedPreferences
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(PREFS_KEY_CIPHERTEXT, base64)
            }
    }

    /** 3. 从 SharedPreferences 取出密文并解密，返回明文 */
    fun retrieveAndDecrypt(context: Context): String? {
        val base64 = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREFS_KEY_CIPHERTEXT, null) ?: return null

        // 3.1 Base64 解码
        val combined = Base64.decode(base64, Base64.NO_WRAP)
        // 前 12 字节是 GCM 默认 IV 长度
        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)

        // 3.2 取出 Key
        val secretKey = (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        // 3.3 初始化 Cipher
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(128, iv)
            )
        }
        // 3.4 执行解密
        val plainBytes = cipher.doFinal(ciphertext)
        return String(plainBytes, Charsets.UTF_8)
    }

    fun reset(context: Context) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit { remove(PREFS_KEY_CIPHERTEXT) }
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS)
        } catch (_: Throwable) {}
    }
}