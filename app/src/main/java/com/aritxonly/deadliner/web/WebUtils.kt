package com.aritxonly.deadliner.web

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

    suspend fun head(path: String): Triple<Int,String?,Long?> = withContext(Dispatchers.IO) {
        val req = auth(Request.Builder().url("$baseUrl/$path").head()).build()
        client.newCall(req).execute().use { resp ->
            val etag = resp.header("ETag")
            val len = resp.header("Content-Length")?.toLongOrNull()
            Triple(resp.code, etag, len)
        }
    }

    suspend fun getBytes(path: String): Pair<ByteArray,String?> = withContext(Dispatchers.IO) {
        val req = auth(Request.Builder().url("$baseUrl/$path").get()).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("GET $path -> ${resp.code}")
            resp.body!!.bytes() to resp.header("ETag")
        }
    }

    suspend fun getRange(path: String, from: Long): Pair<ByteArray, Pair<String?,Long>> =
        withContext(Dispatchers.IO) {
            val rb = Request.Builder().url("$baseUrl/$path").get()
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

    suspend fun putBytes(path: String, bytes: ByteArray, ifMatch: String? = null, ifNoneMatchStar: Boolean = false): String? =
        withContext(Dispatchers.IO) {
            val body = bytes.toRequestBody("application/octet-stream".toMediaType())
            val rb = Request.Builder().url("$baseUrl/$path").put(body)
            if (ifMatch != null) rb.header("If-Match", ifMatch)
            if (ifNoneMatchStar) rb.header("If-None-Match", "*")
            auth(rb)
            client.newCall(rb.build()).execute().use { resp ->
                if (resp.code == 412) throw PreconditionFailed()
                if (!resp.isSuccessful) throw IOException("PUT $path -> ${resp.code}")
                resp.header("ETag")
            }
        }
}