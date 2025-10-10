package com.aritxonly.deadliner.web

import android.content.Context
import android.util.Log
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.KeystorePreferenceManager
import com.aritxonly.deadliner.model.ChatRequest
import com.aritxonly.deadliner.model.ChatResponse
import com.aritxonly.deadliner.model.GeneratedDDL
import com.aritxonly.deadliner.model.LlmPreset
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AIUtils {
    private var BASE_URL = "https://api.deepseek.com/v1/chat/completions"
    private var MODEL = "deepseek-chat"
    private const val MEDIA_TYPE_JSON = "application/json; charset=utf-8"

    private lateinit var apiKey: String
    private val gson = Gson()
    private val client by lazy { OkHttpClient() }

    /** 初始化：在 Application 或首次使用时调用 */
    fun init(context: Context) {
        val config = GlobalUtils.getDeadlinerAIConfig()
        val preset = config.getCurrentPreset()

        preset?.let {
            BASE_URL = it.endpoint
            MODEL = it.model
        }

        apiKey = KeystorePreferenceManager.retrieveAndDecrypt(context)?:""
    }

    fun setPreset(preset: LlmPreset) {
        BASE_URL = preset.endpoint
        MODEL = preset.model
    }

    /**
     * 发送一次无状态的 Prompt 请求，
     * 仅使用 OkHttp + Gson。
     */
    private suspend fun sendPrompt(messages: List<Message>): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey.isBlank()) return@withContext ""

        val requestObj = ChatRequest(
            model = MODEL,
            messages = messages
        )
        val jsonBody = gson.toJson(requestObj)
        val body = jsonBody.toRequestBody(MEDIA_TYPE_JSON.toMediaType())

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", MEDIA_TYPE_JSON)
            .post(body)
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw RuntimeException("API 调用失败：${resp.code} ${resp.message}")
            }
            val respJson = resp.body?.string()
                ?: throw RuntimeException("API 返回空")
            val chatResp = gson.fromJson(respJson, ChatResponse::class.java)
            Log.d("AI Tokens", "${chatResp.usage}")
            chatResp.choices
                .firstOrNull()
                ?.message
                ?.content
                ?: throw RuntimeException("API 没有返回消息")
        }
    }

    suspend fun generateDeadline(context: Context, rawText: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey.isBlank()) return@withContext ""

        val langTag = currentLangTag(context) // 当前设备语言
        val timeFormatSpec = "yyyy-MM-dd HH:mm"

        val tzId = java.util.TimeZone.getDefault().id
        val nowLocal = LocalDateTime.now().toString()

        val systemPrompt = """
        你是Deadliner AI，一个任务管理助手。用户会输入一段自然语言文本，请你从中提取任务，并以**纯 JSON**返回，结构如下：
        {
          "name": "任务名称（≤16字符）",
          "dueTime": "截止时间，$timeFormatSpec",
          "note": "备注信息"
        }

        规则要求：
        1) 仅返回 JSON，**不要**额外说明、代码块（```）、尾逗号。
        2) "name" 和 "note" 必须使用与设备语言一致的语言（当前语言：$langTag）。
        3) "dueTime" 必须严格用 $timeFormatSpec（24小时制，零填充，不带时区）。
        4) 如果出现“今天/明天/本周五/下周一/今晚”等相对时间，请基于设备时区 $tzId、当前时间 $nowLocal 推断，最终输出 $timeFormatSpec。
        5) 如果无法精确分钟，可保守推断（如“晚上”=20:00），但**必须给出具体可解析的时间**。
        6) JSON 键固定为 name/dueTime/note，不要新增。
    """.trimIndent()

        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val localeHint = "当前日期：$today；设备语言：$langTag；时区：$tzId。请严格按上述格式输出 JSON。"

        val messages = listOfNotNull(
            Message("system", systemPrompt),
            Message("user", localeHint),
            GlobalUtils.customPrompt?.let { Message("user", it) },
            Message("user", rawText)
        )

        sendPrompt(messages)
    }

    fun extractJsonFromMarkdown(raw: String): String {
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

    private fun currentLangTag(context: Context): String {
        val loc = context.resources.configuration.locales[0]
        return loc.toLanguageTag() // e.g., "zh-CN" / "en-US"
    }
}