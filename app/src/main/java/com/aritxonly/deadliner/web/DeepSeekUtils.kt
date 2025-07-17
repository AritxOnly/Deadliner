package com.aritxonly.deadliner.web

import android.content.Context
import android.util.Log
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.KeystorePreferenceManager
import com.aritxonly.deadliner.model.ChatRequest
import com.aritxonly.deadliner.model.ChatResponse
import com.aritxonly.deadliner.model.GeneratedDDL
import com.aritxonly.deadliner.model.LocalDateTimeAdapter
import com.aritxonly.deadliner.model.Message
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDateTime

object DeepSeekUtils {
    private const val BASE_URL = "https://api.deepseek.com/v1/chat/completions"
    private const val MEDIA_TYPE_JSON = "application/json; charset=utf-8"

    private lateinit var apiKey: String
    private val gson = Gson()
    private val client by lazy { OkHttpClient() }

    /** 初始化：在 Application 或首次使用时调用 */
    fun init(context: Context) {
        apiKey = KeystorePreferenceManager.retrieveAndDecrypt(context)?:""
    }

    /**
     * 发送一次无状态的 Prompt 请求，
     * 仅使用 OkHttp + Gson。
     */
    private suspend fun sendPrompt(messages: List<Message>): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey.isBlank()) return@withContext ""

        val requestObj = ChatRequest(
            messages = messages
        )
        val jsonBody = gson.toJson(requestObj)
        val body = jsonBody.toRequestBody(MEDIA_TYPE_JSON.toMediaType())

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw RuntimeException("DeepSeek API 调用失败：${resp.code} ${resp.message}")
            }
            val respJson = resp.body?.string()
                ?: throw RuntimeException("DeepSeek API 返回空")
            val chatResp = gson.fromJson(respJson, ChatResponse::class.java)
            chatResp.choices
                .firstOrNull()
                ?.message
                ?.content
                ?: throw RuntimeException("DeepSeek API 没有返回消息")
        }
    }

    suspend fun generateDeadline(rawText: String): String = withContext(Dispatchers.IO) {
        val systemPrompt = """
            你是一个个性化任务管理助手。在对话中，用户将会输入一整段文字，你将要从这段文字中提取出有效任务信息，包括任务名(不多于16字)、任务开始的时间和其他信息，并按以下格式输出：
            {
                "name": "任务名称",
                "dueTime": "2025-03-02 11:45",·
                "note": "备注信息"
            }
            并且**仅返回**符合上述结构的JSON，不要添加任何说明文字
        """.trimIndent()

        val messages = listOfNotNull(
            Message("system", systemPrompt),
            GlobalUtils.customPrompt?.let { Message("user", it) },
            Message("user", rawText)
        )

        sendPrompt(messages)
    }

    fun extractJsonFromMarkdown(raw: String): String {
        Log.d("DeepSeek", raw)

        val jsonFenceRegex = Regex("```json\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        jsonFenceRegex.find(raw)?.let { return it.groups[1]!!.value.trim() }

        val anyFenceRegex = Regex("```\\s*([\\s\\S]*?)```")
        anyFenceRegex.find(raw)?.let { return it.groups[1]!!.value.trim() }

        return raw.trim()
    }

    fun parseGeneratedDDL(raw: String): GeneratedDDL {
        val json = extractJsonFromMarkdown(raw)

        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()

        return gson.fromJson(json, GeneratedDDL::class.java)
    }
}