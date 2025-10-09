package com.aritxonly.deadliner.localutils

import com.aritxonly.deadliner.model.DDLItem
import java.time.LocalDateTime

data class SearchFilter(
    val query: String,
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val hour: Int? = null
) {
    companion object {
        // 正则表达式匹配字母与数字组合，如 y2025, d20 等
        private val regex = Regex("([ymdh])(\\d+)", RegexOption.IGNORE_CASE)

        fun parse(input: String): SearchFilter {
            // 查找所有匹配的过滤条件
            val matches = regex.findAll(input)
            var year: Int? = null
            var month: Int? = null
            var day: Int? = null
            var hour: Int? = null

            // 将匹配项记录下来，并从原字符串中剔除这些子串
            var plainText = input
            for (match in matches) {
                val key = match.groupValues[1].lowercase()
                val value = match.groupValues[2].toIntOrNull()
                when (key) {
                    "y" -> year = value
                    "m" -> month = value
                    "d" -> day = value
                    "h" -> hour = value
                }
                // 删除匹配到的过滤条件字符串
                plainText = plainText.replace(match.value, "")
            }
            return SearchFilter(plainText.trim(), year, month, day, hour)
        }
    }

    fun matches(ddlItem: DDLItem): Boolean {
        if (ddlItem.isArchived) return false

        // 文本
        val matchesText = ddlItem.name.contains(query, ignoreCase = true) ||
                ddlItem.note.contains(query, ignoreCase = true)
        if (!matchesText) return false

        // 时间
        val startTime = runCatching { GlobalUtils.safeParseDateTime(ddlItem.startTime) }.getOrNull()
        val completeTime = runCatching { GlobalUtils.safeParseDateTime(ddlItem.completeTime) }.getOrNull()

        fun matchesYMDH(
            year: Int? = null, month: Int? = null, day: Int? = null, hour: Int? = null
        ): Boolean {
            fun tOk(t: LocalDateTime?) =
                (year == null || t?.year == year) &&
                        (month == null || t?.monthValue == month) &&
                        (day == null || t?.dayOfMonth == day) &&
                        (hour == null || t?.hour == hour)
            // 任一时间命中即可
            return tOk(startTime) || tOk(completeTime)
        }

        return matchesYMDH(year, month, day, hour)
    }
}