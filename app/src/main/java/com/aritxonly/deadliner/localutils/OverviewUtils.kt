package com.aritxonly.deadliner.localutils

import com.aritxonly.deadliner.model.DDLItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

object OverviewUtils {

    /**
     * 返回过去 n 天（含 today）每天完成的任务数列表。
     * 结果按日期升序：[(2025-06-08, 3), (2025-06-09, 5), …, (2025-06-14, 4)]
     */
    fun computeDailyCompletedCounts(
        items: List<DDLItem>,
        days: Int = 7
    ): List<Pair<LocalDate, Int>> {
        val completedDates = items
            .filter { it.isCompleted && it.completeTime.isNotBlank() }
            .mapNotNull {
                runCatching { GlobalUtils.parseDateTime(it.completeTime)?.toLocalDate() }
                    .getOrNull()
            }

        val today = LocalDate.now()
        // 对过去 days 天逐日计数
        return (0 until days).map { offset ->
            val date = today.minusDays((days - 1 - offset).toLong())
            val count = completedDates.count { it == date }
            date to count
        }
    }

    /**
     * 过去 days 天内，每天「到期日 = that day 且未完成」的任务数。
     */
    fun computeDailyOverdueCounts(
        items: List<DDLItem>,
        days: Int = 7
    ): List<Pair<LocalDate, Int>> {
        val today = LocalDate.now()

        return (0 until days).map { offset ->
            val date = today.minusDays((days - 1 - offset).toLong())
            // 把 endTime parse 成 LocalDate，再筛选
            val count = items.count { item ->
                runCatching {
                    GlobalUtils.parseDateTime(item.completeTime)?.toLocalDate()
                }.getOrNull()?.let { endDate ->
                    !item.isCompleted && endDate == date
                } ?: false
            }
            date to count
        }
    }

    /**
     * 过去 days 天内，每天完成率（0.0–1.0）。
     */
    fun computeDailyCompletionRate(
        items: List<DDLItem>,
        days: Int = 7
    ): List<Pair<LocalDate, Double>> {
        val today = LocalDate.now()

        return (0 until days).map { offset ->
            val date = today.minusDays((days - 1 - offset).toLong())
            // 当天的任务：endDate == date
            val dailyTasks = items.filter {
                runCatching { GlobalUtils.parseDateTime(it.completeTime)?.toLocalDate() }
                    .getOrNull() == date
            }
            val completed = dailyTasks.count { it.isCompleted }
            val rate = if (dailyTasks.isNotEmpty()) completed.toDouble() / dailyTasks.size else 0.0
            date to rate
        }
    }

    /**
     * 过去 n 周，每周完成总数。返回 [(2025-W23, 12), (2025-W24, 15), …]
     */
    fun computeWeeklyCompletedCounts(
        items: List<DDLItem>,
        weeks: Int = 4
    ): List<Pair<String, Int>> {
        val weekOfYear = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
        val today = LocalDate.now()

        // 先统计每周数
        val weekBuckets = items
            .filter { it.isCompleted }
            .mapNotNull { item ->
                runCatching {
                    GlobalUtils.safeParseDateTime(item.completeTime).toLocalDate()
                        .let { date ->
                            date.get(weekOfYear) to date.year
                        }
                }.getOrNull()
            }
            .groupingBy { (week, year) -> "$year-W$week" }
            .eachCount()

        return (0 until weeks).map { offset ->
            val date = today.minusWeeks((weeks - 1 - offset).toLong())
            val key = "${date.year}-W${date.get(weekOfYear)}"
            key to (weekBuckets[key] ?: 0)
        }
    }
}