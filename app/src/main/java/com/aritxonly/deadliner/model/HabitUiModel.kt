package com.aritxonly.deadliner.model

import java.time.LocalDate

// 每天整体概览
data class DayOverview(
    val date: LocalDate,
    val completedCount: Int,
    val totalCount: Int
) {
    val completionRatio: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
}

// 单个习惯在某一天的状态
data class HabitWithDailyStatus(
    val habit: Habit,
    val doneCount: Int,
    val targetCount: Int,
    val isCompleted: Boolean
)