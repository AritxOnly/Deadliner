package com.aritxonly.deadliner.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class HabitRecordStatus {
    COMPLETED,
    SKIPPED
}

data class HabitRecord(
    val id: Long = 0L,
    val habitId: Long,                   // 对应 habits.id
    val date: LocalDate,                 // 只存日期
    val count: Int = 1,
    val status: HabitRecordStatus = HabitRecordStatus.COMPLETED,
    val createdAt: LocalDateTime = LocalDateTime.now()
)