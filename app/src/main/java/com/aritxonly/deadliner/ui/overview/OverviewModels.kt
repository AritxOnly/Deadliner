package com.aritxonly.deadliner.ui.overview

import com.aritxonly.deadliner.model.DDLItem
import java.time.LocalDate

data class DailyStat(
    val date: LocalDate,
    val label: String,
    val completedCount: Int,
    val overdueCount: Int
)

data class ContributionDay(
    val date: LocalDate,
    val count: Int
)

data class MonthlyStat(
    val month: String,
    val totalCount: Int,
    val completedCount: Int,
    val overdueCompletedCount: Int
)

data class WeeklyStat(
    val weekLabel: String,
    val completedCount: Int
)

data class Metric(
    val label: String,
    val value: String,
    val change: String? = null,
    val isDown: Boolean? = null
)

data class MonthlyAnalysisResult(
    val month: String,
    val summary: String,
    val keywords: List<String>
)

data class OverviewSnapshot(
    val activeStats: LinkedHashMap<String, Int>,
    val historyStats: LinkedHashMap<String, Int>,
    val completionTimeStats: List<Pair<String, Int>>,
    val overdueItems: List<DDLItem>,
    val dailyStats: List<DailyStat>,
    val monthlyStats: List<MonthlyStat>,
    val weeklyStats: List<WeeklyStat>,
    val contributionStats: List<ContributionDay>,
    val lastMonthDailyStats: List<DailyStat>,
    val metrics: List<Metric>,
    val lastMonthName: String,
    val lastMonthKey: String,
    val completedTaskNames: List<String>,
    val metricsSummary: String
)
