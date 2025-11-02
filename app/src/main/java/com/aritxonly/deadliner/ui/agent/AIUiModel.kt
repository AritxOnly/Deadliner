package com.aritxonly.deadliner.ui.agent

import com.aritxonly.deadliner.ai.IntentType

// 展示层卡片类型
sealed class UiCard {
    data class TaskCard(
        val title: String,
        val due: String,                 // "yyyy-MM-dd HH:mm"
        val note: String,
    ) : UiCard()

    data class PlanBlockCard(
        val title: String,
        val start: String,               // "yyyy-MM-dd HH:mm"
        val end: String,                 // "yyyy-MM-dd HH:mm"
        val location: String?,
        val energy: String?,
        val linkTask: String?
    ) : UiCard()

    data class StepsCard(
        val title: String,
        val checklist: List<String>
    ) : UiCard()
}

// 顶部筛选
enum class ResultFilter { All, Tasks, Plan, Steps }

// 页面状态
data class ResultState(
    val intent: IntentType? = null,     // 自动识别到的主意图（用于默认tab）
    val cards: List<UiCard> = emptyList(),
    val filter: ResultFilter = ResultFilter.All,
    val isLoading: Boolean = false,
    val error: String? = null
)