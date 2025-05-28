package com.aritxonly.deadliner.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

data class HabitMetaData(
    val completedDates: Set<String>,
    val frequencyType: DeadlineFrequency,
    val frequency: Int,
    val total: Int,
    val refreshDate: String
)

enum class DeadlineFrequency { DAILY, WEEKLY, MONTHLY, TOTAL }

fun updateNoteWithDate(habit: DDLItem, newDate: LocalDate): String {
    val gson = Gson()
    val type = object : TypeToken<HabitMetaData>() {}.type
    // 如果 note 为空或无效，返回默认值
    val currentData: HabitMetaData = if (habit.note.isBlank()) {
        HabitMetaData(emptySet(), DeadlineFrequency.DAILY, 1, 0, LocalDate.now().toString())
    } else {
        try {
            gson.fromJson(habit.note, type) ?: HabitMetaData(emptySet(), DeadlineFrequency.DAILY, 1, 0, LocalDate.now().toString())
        } catch (e: Exception) {
            HabitMetaData(emptySet(), DeadlineFrequency.DAILY, 1, 0, LocalDate.now().toString())
        }
    }
    // 添加新日期，并保留原有频率信息
    val updatedDates = currentData.completedDates.toMutableSet().apply { add(newDate.toString()) }
    val updatedData = currentData.copy(completedDates = updatedDates)
    return gson.toJson(updatedData)
}

fun getCompletedDates(habit: DDLItem): Set<LocalDate> {
    if (habit.note.isBlank()) return emptySet()
    val gson = Gson()
    val type = object : TypeToken<HabitMetaData>() {}.type
    val currentData: HabitMetaData = try {
        gson.fromJson(habit.note, type) ?: HabitMetaData(emptySet(), DeadlineFrequency.DAILY, 1, 0, LocalDate.now().toString())
    } catch (e: Exception) {
        HabitMetaData(emptySet(), DeadlineFrequency.DAILY, 1, 0, LocalDate.now().toString())
    }
    // 转换日期字符串为 LocalDate，遇到异常的日期直接忽略
    return currentData.completedDates.mapNotNull { dateStr ->
        try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }.toSet()
}

fun HabitMetaData.toJson(): String {
    return Gson().toJson(this)
}