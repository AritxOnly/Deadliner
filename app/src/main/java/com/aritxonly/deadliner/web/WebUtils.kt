package com.aritxonly.deadliner.web

import android.util.Log
import com.aritxonly.deadliner.localutils.GlobalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class WebUtils(
    private val baseUrl: String,
    private val username: String? = null,
    private val password: String? = null
) {
    private val client = OkHttpClient.Builder().build()

    private fun auth(rb: Request.Builder): Request.Builder {
        if (username != null && password != null) {
            rb.header("Authorization", Credentials.basic(username, password, Charsets.UTF_8))
        }
        return rb
    }

    /** 统一 guard：如果关闭同步，抛异常 */
    private fun ensureSyncEnabled() {
        if (!GlobalUtils.cloudSyncEnable) {
            throw IllegalStateException("Cloud sync is disabled by user.")
        }
    }

    suspend fun head(path: String): Triple<Int,String?,Long?> = withContext(Dispatchers.IO) {
        ensureSyncEnabled()
        val url = joinUrl(baseUrl, path)
        val req = auth(Request.Builder().url(url).head()).build()
        Log.d("WebUtils", req.toString())
        client.newCall(req).execute().use { resp ->
            val etag = resp.header("ETag")
            val len = resp.header("Content-Length")?.toLongOrNull()
            Log.d("WebUtils", resp.toString())
            Triple(resp.code, etag, len)
        }
    }

    suspend fun getBytes(path: String): Pair<ByteArray,String?> = withContext(Dispatchers.IO) {
        ensureSyncEnabled()
        val url = joinUrl(baseUrl, path)
        val req = auth(Request.Builder().url(url).get()).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("GET $path -> ${resp.code}")
            resp.body!!.bytes() to resp.header("ETag")
        }
    }

    suspend fun getRange(path: String, from: Long): Pair<ByteArray, Pair<String?,Long>> =
        withContext(Dispatchers.IO) {
            ensureSyncEnabled()
            val url = joinUrl(baseUrl, path)
            val rb = Request.Builder().url(url).get()
            rb.header("Range", "bytes=$from-")
            auth(rb)
            client.newCall(rb.build()).execute().use { resp ->
                if (resp.code !in listOf(206,200)) throw IOException("GET Range $path -> ${resp.code}")
                val bytes = resp.body!!.bytes()
                val etag = resp.header("ETag")
                val newOffset = from + bytes.size
                bytes to (etag to newOffset)
            }
        }

    class PreconditionFailed: IOException()

    suspend fun putBytes(
        path: String,
        bytes: ByteArray,
        ifMatch: String? = null,
        ifNoneMatchStar: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        ensureSyncEnabled()

        // ✅ 关键：先把父目录建好（Deadliner/）
        val parentOk = ensureParents(path)
        if (!parentOk) throw IOException("MKCOL parents failed for $path")

        val url = joinUrl(baseUrl, path)
        val body = bytes.toRequestBody("application/octet-stream".toMediaType())
        val rb = Request.Builder().url(url).put(body)
        if (ifMatch != null) rb.header("If-Match", ifMatch)
        if (ifNoneMatchStar) rb.header("If-None-Match", "*")
        auth(rb)
        client.newCall(rb.build()).execute().use { resp ->
            // 409 这里一般只有在把目录当文件 PUT 或其他冲突才会发生
            if (resp.code == 412) throw PreconditionFailed()
            if (!resp.isSuccessful) throw IOException("PUT $path -> ${resp.code}")
            resp.header("ETag")
        }
    }

    private fun joinUrl(base: String, path: String): String {
        // 去掉重复斜杠，保留 https:// 的 //
        val b = if (base.endsWith("/")) base.dropLast(1) else base
        val p = path.trimStart('/')
        Log.d("WebUtils", "$base -> $b\n$path -> $p")
        return "$b/$p"
    }

    /** 目录是否存在（优先 HEAD；部分服务对目录 HEAD 不友好就退回 PROPFIND） */
    suspend fun dirExists(dir: String): Boolean = withContext(Dispatchers.IO) {
        val code = runCatching {
            propfind(dir, "0")
        }.getOrDefault(404)

        // 207 是 WebDAV 的标准存在返回码，200/301 也算
        return@withContext code in listOf(200, 207, 301, 302)
    }

    /** MKCOL 创建目录。已存在时部分服务返回 405/409，按“成功”处理。 */
    suspend fun mkcol(dir: String): Boolean = withContext(Dispatchers.IO) {
        val url = joinUrl(baseUrl, dir.trimEnd('/') + "/")
        val rb = Request.Builder().url(url).method("MKCOL", (ByteArray(0))
            .toRequestBody("text/plain".toMediaType()))
        auth(rb)
        client.newCall(rb.build()).execute().use { resp ->
            return@use when (resp.code) {
                201 -> true                    // Created
                405, 409 -> true               // 已存在 / 冲突（很多实现会这么回）
                else -> false
            }
        }
    }

    private suspend fun propfind(path: String, depth: String = "0"): Int = withContext(Dispatchers.IO) {
        ensureSyncEnabled() // 同样加上开关检查
        val url = joinUrl(baseUrl, path.trimEnd('/') + "/") // 目录通常带斜杠

        // OkHttp 允许自定义 Method
        // PROPFIND 不需要 Body，但 OkHttp 的 method() 可能要求 nullable body，这里传 null 即可，或者空 body
        val rb = Request.Builder()
            .url(url)
            .method("PROPFIND", null) // method 字符串大写
            .header("Depth", depth)
        // .header("Content-Type", "application/xml") // 可选

        auth(rb)

        client.newCall(rb.build()).execute().use { resp ->
            return@use resp.code
        }
    }

    /** 确保父目录链存在，比如 Deadliner/changes-2025-08.ndjson -> 逐级保证 Deadliner/ */
    suspend fun ensureParents(filePath: String): Boolean {
        // 只处理目录部分
        val cleaned = filePath.trim().trimStart('/')
        val parts = cleaned.split('/').toMutableList()
        if (parts.isEmpty()) return true
        // 如果最后一段带点（看作文件），去掉它保留父目录；否则当作目录链处理
        if (parts.last().contains('.')) parts.removeLast()
        if (parts.isEmpty()) return true

        var cur = ""
        for (seg in parts) {
            if (seg.isBlank()) continue
            cur = if (cur.isEmpty()) seg else "$cur/$seg"
            val exists = dirExists(cur)
            if (!exists) {
                val ok = mkcol(cur)
                if (!ok) return false
            }
        }
        return true
    }

    suspend fun ensureDir(dirname: String): Boolean = withContext(Dispatchers.IO) {
        if (!GlobalUtils.cloudSyncEnable) return@withContext false

        // 1. 使用 PROPFIND 检测
        // 如果存在（包括坚果云的 207），直接返回 true
        if (dirExists(dirname)) return@withContext true

        // 2. 如果不存在，尝试 MKCOL
        return@withContext runCatching {
            mkcol(dirname)
        }.getOrDefault(false)
    }
}