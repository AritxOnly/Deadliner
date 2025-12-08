package com.aritxonly.deadliner.ai

enum class IntentType { ExtractTasks, PlanDay, SplitToSteps }

fun String.toIntentTypeOrDefault(default: IntentType = IntentType.ExtractTasks): IntentType =
    when (this) {
        "PlanDay" -> IntentType.PlanDay
        "SplitToSteps" -> IntentType.SplitToSteps
        "ExtractTasks" -> IntentType.ExtractTasks
        else -> default
    }

data class IntentGuess(
    val intent: IntentType,
    val confidence: Double,   // 0.0 ~ 1.0
    val reason: String        // 调试/打点
)

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
    val note: String = ""
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

data class MixedResult(
    val primaryIntent: String,              // "ExtractTasks" | "PlanDay" | "SplitToSteps"
    val tasks: List<AITask> = emptyList(),  // 允许缺省，Gson 会给空列表
    val planBlocks: List<PlanBlock> = emptyList(),
    val steps: List<SplitStepsResult> = emptyList() // 支持多个 steps 场景；单个也可放在列表中
)