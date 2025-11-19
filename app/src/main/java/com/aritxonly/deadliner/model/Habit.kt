package com.aritxonly.deadliner.model

import java.time.LocalDateTime

enum class HabitPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class HabitGoalType {
    PER_PERIOD,
    TOTAL
}

enum class HabitStatus {
    ACTIVE,
    ARCHIVED
}

data class Habit(
    val id: Long = 0L,
    val ddlId: Long,                     // 对应 ddl_items.id
    val name: String,
    val description: String? = null,
    val color: Int? = null,
    val iconKey: String? = null,
    val period: HabitPeriod,
    val timesPerPeriod: Int = 1,
    val goalType: HabitGoalType = HabitGoalType.PER_PERIOD,
    val totalTarget: Int? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val status: HabitStatus = HabitStatus.ACTIVE,
    val sortOrder: Int = 0,
    val alarmTime: String? = null
)