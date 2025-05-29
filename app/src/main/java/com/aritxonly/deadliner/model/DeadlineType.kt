package com.aritxonly.deadliner.model

enum class DeadlineType {
    TASK,
    HABIT;

    // 重写 toString，返回小写字符串
    override fun toString(): String {
        return name.lowercase()
    }

    companion object {
        fun fromString(value: String): DeadlineType {
            return entries.firstOrNull { it.toString() == value.lowercase() }
                ?: throw IllegalArgumentException("Invalid DeadlineType: $value")
        }
    }
}