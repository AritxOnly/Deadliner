package com.aritxonly.deadliner.model

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
import kotlinx.serialization.Serializable
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
    val usage: Usage
)
data class Choice(
    val message: Message
)
data class Usage(
    @SerializedName("prompt_cache_hit_tokens") val promptCacheHitTokens: Int,
    @SerializedName("prompt_cache_miss_tokens") val promptCacheMissTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
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