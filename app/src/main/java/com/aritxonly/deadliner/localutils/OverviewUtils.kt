package com.aritxonly.deadliner.localutils

import android.content.Context
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.DDLItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

object OverviewUtils {

    data class MonthlyStat(
        val month: String,         // 格式 "yyyy-MM"
        val total: Int,            // 当月到期任务数
        val completed: Int,        // 当月完成任务数
        val overdueCompleted: Int  // 当月逾期完成任务数
    )

    /**
     * 返回过去 months 个月的标签，格式 "yyyy-MM"，按时间升序。
     * 默认 12 个月：从 11 个月前 一直到本月。
     */
    fun getLastNMonthLabels(months: Int = 12): List<String> {
        val now = LocalDate.now()
        val fmt = DateTimeFormatter.ofPattern("yy-MM")
        return (0 until months).map { offset ->
            now.minusMonths((months - 1 - offset).toLong()).format(fmt)
        }
    }

    /**
     * 返回过去 12 个月内，每月的任务总数、完成数、逾期完成数。
     * months 参数可改来取 N 个月。
     */
    fun computeMonthlyTaskStats(
        items: List<DDLItem>,
        months: Int = 12
    ): List<MonthlyStat> {
        val now = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        // 构建过去 months 个月的 (year, month) 列表
        val monthList = (0 until months).map { offset ->
            val date = now.minusMonths((months - 1 - offset).toLong())
            date.year to date.monthValue
        }

        return monthList.map { (year, month) ->
            val label = "%04d-%02d".format(year, month)

            // 当月到期任务数
            val total = items.count { item ->
                runCatching { GlobalUtils.parseDateTime(item.endTime)?.toLocalDate() }
                    .getOrNull()?.let { d ->
                        d.year == year && d.monthValue == month
                    } ?: false
            }

            // 当月完成任务数
            val completed = items.count { item ->
                if (!item.isCompleted) return@count false
                runCatching { GlobalUtils.parseDateTime(item.completeTime)?.toLocalDate() }
                    .getOrNull()?.let { d ->
                        d.year == year && d.monthValue == month
                    } ?: false
            }

            // 当月逾期完成数：完成且完成日 > 截止日，并以完成月为主
            val overdueCompleted = items.count { item ->
                if (!item.isCompleted) return@count false
                val completeDT = runCatching { GlobalUtils.parseDateTime(item.completeTime) }
                    .getOrNull()
                val endDT = runCatching { GlobalUtils.parseDateTime(item.endTime) }
                    .getOrNull()

                if (completeDT != null && endDT != null
                    && completeDT.year == year
                    && completeDT.monthValue == month
                    && completeDT.isAfter(endDT)
                ) true else false
            }

            MonthlyStat(label, total, completed, overdueCompleted)
        }
    }

    /**
     * 返回过去 n 天（含 today）每天完成的任务数列表。
     * 结果按日期升序：[(2025-06-08, 3), (2025-06-09, 5), …, (2025-06-14, 4)]
     */
    fun computeDailyCompletedCounts(
        items: List<DDLItem>,
        days: Int = 7
    ): List<Triple<LocalDate, Int, Int>> {
        val completedDates = items
            .filter { it.isCompleted && it.completeTime.isNotBlank() }
            .mapNotNull {
                runCatching { GlobalUtils.parseDateTime(it.completeTime)?.toLocalDate() }
                    .getOrNull()
            }
        val overdueDates = items
            .filter {
                it.isCompleted && it.completeTime.isNotBlank()
                        && GlobalUtils.safeParseDateTime(it.completeTime).isAfter(
                            GlobalUtils.safeParseDateTime(it.endTime)
                        )
            }
            .mapNotNull {
                runCatching { GlobalUtils.parseDateTime(it.completeTime)?.toLocalDate() }
                    .getOrNull()
            }

        val today = LocalDate.now()
        // 对过去 days 天逐日计数
        return (0 until days).map { offset ->
            val date = today.minusDays((days - 1 - offset).toLong())
            val count = completedDates.count { it == date }
            val overdue = overdueDates.count { it == date }
            Triple(date, count, overdue)
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
        context: Context,
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
            .groupingBy { (week, year) -> context.getString(R.string.xx_th_weeks, week) }
            .eachCount()

        return (0 until weeks).map { offset ->
            val date = today.minusWeeks((weeks - 1 - offset).toLong())
            val key = context.getString(R.string.xx_th_weeks, date.get(weekOfYear))
            key to (weekBuckets[key] ?: 0)
        }
    }
}