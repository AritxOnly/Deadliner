package com.aritxonly.deadliner.ai

import com.aritxonly.deadliner.ai.ChatRequest
import com.aritxonly.deadliner.ai.ChatResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** 负责把 ChatRequest 发出去、拿 ChatResponse 回来 */
interface LlmTransport {
    suspend fun chat(request: ChatRequest): ChatResponse
}

/** 直连供应商（老版本）：Authorization: Bearer <apiKey> */
class DirectBearerTransport(
    private val baseUrl: String,
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
) : LlmTransport {
    override suspend fun chat(request: ChatRequest): ChatResponse = withContext(Dispatchers.IO) {
        val body = gson.toJson(request).toRequestBody(MEDIA_JSON.toMediaType())
        val http = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", MEDIA_JSON)
            .post(body)
            .build()
        client.newCall(http).execute().use { resp ->
            if (!resp.isSuccessful) error("API 调用失败：${resp.code} ${resp.message}")
            val text = resp.body?.string() ?: error("API 返回空")
            gson.fromJson(text, ChatResponse::class.java)
        }
    }
    private companion object { const val MEDIA_JSON = "application/json; charset=utf-8" }
}

/** Cloudflare 代理（新版）：X-Deadliner-Key + X-Deadliner-Device */
class DeadlinerProxyTransport(
    private val apiBase: String,
    private val appSecret: String,
    private val deviceId: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
) : LlmTransport {

    override suspend fun chat(request: ChatRequest): ChatResponse = withContext(Dispatchers.IO) {
        val patched = if (request.stream == true) request.copy(stream = false) else request
        val body = gson.toJson(patched).toRequestBody(MEDIA_JSON.toMediaType())

        val http = Request.Builder()
            .url(join(apiBase, "/chat/completions"))
            .addHeader("Content-Type", MEDIA_JSON)
            .addHeader("X-Deadliner-Key", appSecret)
            .addHeader("X-Deadliner-Device", deviceId)
            .post(body)
            .build()

        client.newCall(http).execute().use { resp ->
            if (!resp.isSuccessful) {
                val txt = resp.body?.string()
                error("Deadliner Proxy 调用失败：${resp.code} ${resp.message} ${txt ?: ""}")
            }
            val text = resp.body?.string() ?: error("Proxy 返回空")
            gson.fromJson(text, ChatResponse::class.java)
        }
    }

    private fun join(base: String, path: String): String =
        if (base.endsWith("/")) base.dropLast(1) + path else base + path

    private companion object { const val MEDIA_JSON = "application/json; charset=utf-8" }
}