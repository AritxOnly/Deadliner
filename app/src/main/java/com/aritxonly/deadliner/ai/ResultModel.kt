package com.aritxonly.deadliner.ai

enum class IntentType { ExtractTasks, PlanDay, SplitToSteps }

data class UserProfile(
    val preferredLang: String?,         // 例: "zh-CN"；为空则用设备语言
    val defaultEveningHour: Int = 20,   // “晚上”映射
    val defaultReminderMinutes: List<Int> = listOf(30), // 默认提醒
    val defaultWorkdayStart: String? = null,  // "09:00"
    val defaultWorkdayEnd: String? = null     // "18:00"
)

data class AITask(
    val name: String,
    val dueTime: String,          // "yyyy-MM-dd HH:mm"
    val note: String = "",
    val priority: Int? = null,    // 0/1/2
    val reminders: List<Int>? = null,
    val tags: List<String>? = null,
    val recurrence: String? = null,    // RRULE subset
    val checklist: List<String>? = null,
    val dependsOn: List<String>? = null
)

data class ExtractTasksResult(
    val tasks: List<AITask>,
    val timezone: String,
    val resolvedAt: String
)

data class PlanBlock(
    val title: String,
    val start: String,     // "yyyy-MM-dd HH:mm"
    val end: String,       // "
    val location: String? = null,
    val energy: String? = null,   // low/med/high
    val linkTask: String? = null
)

data class SplitStepsResult(
    val title: String,
    val checklist: List<String>
)

sealed class AIResult {
    data class ExtractTasks(val data: ExtractTasksResult): AIResult()
    data class PlanDay(val blocks: List<PlanBlock>): AIResult()
    data class SplitToSteps(val data: SplitStepsResult): AIResult()
}