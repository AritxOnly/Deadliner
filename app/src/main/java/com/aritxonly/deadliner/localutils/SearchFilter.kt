package com.aritxonly.deadliner.localutils

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
}