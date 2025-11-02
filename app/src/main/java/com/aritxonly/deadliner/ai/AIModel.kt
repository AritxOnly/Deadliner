package com.aritxonly.deadliner.ai

import android.os.Parcelable
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Message(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<Message>,
    val stream: Boolean = false
)

data class ChatResponse(
    val choices: List<Choice>,
    val usage: Usage?
)

data class Choice(
    val message: Message
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int? = null,
    @SerializedName("completion_tokens") val completionTokens: Int? = null,
    @SerializedName("total_tokens") val totalTokens: Int? = null,

    @SerializedName("prompt_cache_hit_tokens") val promptCacheHitTokens: Int? = null,
    @SerializedName("prompt_cache_miss_tokens") val promptCacheMissTokens: Int? = null
)

@Parcelize
data class GeneratedDDL(
    val name: String,
    val dueTime: LocalDateTime,
    val note: String
) : Parcelable

class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    // 序列化：LocalDateTime -> JSON 字符串
    override fun serialize(
        src: LocalDateTime?,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonPrimitive(src?.format(formatter))
    }

    // 反序列化：JSON 字符串 -> LocalDateTime
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LocalDateTime {
        val str = json?.asString
            ?: throw JsonParseException("dueTime 字段为空")
        return LocalDateTime.parse(str, formatter)
    }
}

data class LlmPreset(
    val id: String,          // 唯一标识（建议 UUID 或稳定的 slug）
    val name: String,        // 展示名：如 "DeepSeek（官方）"
    val model: String,       // 传参用的模型名：如 "deepseek-chat"
    val endpoint: String     // API 接入点：如 "https://api.deepseek.com/v1/chat/completions"
)

val defaultLlmPreset = LlmPreset(
    id = "deadliner_official",
    name = "Deadliner AI",
    model = "deepseek-chat",
    endpoint = "https://deadliner.aritxonly.top/api"
)

/** 后端类型：保持可扩展（将来可加 HMAC） */
enum class BackendType { DirectBearer, DeadlinerProxy }

/** 你的 LlmPreset 可以增加这几个字段（或用已有字段映射） */
data class BackendPreset(
    val type: BackendType,
    val endpoint: String,
    val model: String,
)

object LlmTransportFactory {
    fun create(
        preset: BackendPreset,
        bearerKey: String,     // Direct 模式下使用
        appSecret: String,     // Proxy 模式下使用
        deviceId: String       // Proxy 模式下使用
    ): LlmTransport = when (preset.type) {
        BackendType.DirectBearer ->
            DirectBearerTransport(preset.endpoint, bearerKey)
        BackendType.DeadlinerProxy ->
            DeadlinerProxyTransport(preset.endpoint, appSecret, deviceId)
    }
}