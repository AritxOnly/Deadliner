package com.aritxonly.deadliner.localutils

import android.content.Context
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.ui.overview.ContributionDay
import com.aritxonly.deadliner.ui.overview.DailyStat
import com.aritxonly.deadliner.ui.overview.Metric
import com.aritxonly.deadliner.ui.overview.MonthlyStat
import com.aritxonly.deadliner.ui.overview.OverviewSnapshot
import com.aritxonly.deadliner.ui.overview.WeeklyStat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

object OverviewUtils {

    private data class ParsedItem(
        val item: DDLItem,
        val startTime: LocalDateTime?,
        val endTime: LocalDateTime?,
        val completeTime: LocalDateTime?,
    )

    fun buildSnapshot(
        context: Context,
        items: List<DDLItem>,
        now: LocalDateTime = LocalDateTime.now(),
    ): OverviewSnapshot {
        val parsedItems = items.map { item ->
            ParsedItem(
                item = item,
                startTime = parseDateTimeOrNull(item.startTime),
                endTime = parseDateTimeOrNull(item.endTime),
                completeTime = parseDateTimeOrNull(item.completeTime),
            )
        }
        val activeItems = parsedItems.filter { !it.item.isArchived }
        val today = now.toLocalDate()

        val todayCompletedCount = activeItems.count { parsed ->
            parsed.item.isCompleted && parsed.completeTime?.toLocalDate() == today
        }
        val todayTodoCount = activeItems.count { parsed ->
            !parsed.item.isCompleted &&
                !parsed.item.isAbandonedLike() &&
                parsed.endTime?.let { !it.isBefore(now) } == true
        }
        val todayOverdueCount = activeItems.count { parsed ->
            !parsed.item.isCompleted &&
                !parsed.item.isAbandonedLike() &&
                parsed.endTime?.toLocalDate() == today &&
                parsed.endTime.isBefore(now)
        }
        val activeAbandonedCount = activeItems.count { it.item.isAbandonedLike() }

        val currentTodoCount = parsedItems.count { !it.item.isCompleted && !it.item.isAbandonedLike() }
        val historyCompletedCount = parsedItems.count { it.item.isCompleted }
        val historyAbandonedCount = parsedItems.count { it.item.isAbandonedLike() }
        val cumulativeOverdueItems = activeItems.filter { parsed ->
            !parsed.item.isCompleted &&
                !parsed.item.isAbandonedLike() &&
                parsed.endTime?.isBefore(now) == true
        }.sortedBy { it.endTime }

        val activeStats = linkedMapOf(
            context.getString(R.string.today_completed) to todayCompletedCount,
            context.getString(R.string.pending_tasks) to todayTodoCount,
            context.getString(R.string.today_overdue) to todayOverdueCount,
            context.getString(R.string.abandoned) to activeAbandonedCount,
        )
        val historyStats = linkedMapOf(
            context.getString(R.string.cumulative_completed) to historyCompletedCount,
            context.getString(R.string.current_pending) to currentTodoCount,
            context.getString(R.string.cumulative_abandoned) to historyAbandonedCount,
            context.getString(R.string.cumulative_overdue) to cumulativeOverdueItems.size,
        )

        val completionTimeStats = buildCompletionTimeStats(context, parsedItems)
        val dailyStats = computeDailyStats(parsedItems, days = 7, now = now)
        val monthlyStats = computeMonthlyStats(parsedItems, months = 12, now = now)
        val weeklyStats = computeWeeklyStats(parsedItems, weeks = 4, now = now)
        val contributionStats = computeContributionStats(parsedItems, days = 150, now = now)
        val lastMonthDailyStats = computeLastMonthDailyStats(parsedItems, now = now)
        val lastMonthName = formatLastMonthName(now, Locale.getDefault())
        val lastMonthKey = YearMonth.from(now.minusMonths(1)).toString()
        val metrics = computeMetrics(context, parsedItems, now = now)
        val completedTaskNames = collectLastMonthCompletedTaskNames(parsedItems, now = now)
        val metricsSummary = metrics.joinToString(separator = "\n") { metric ->
            buildString {
                append(metric.label)
                append(": ")
                append(metric.value)
                metric.change?.let { change ->
                    if (change.isNotBlank()) {
                        append(" (")
                        append(change)
                        append(")")
                    }
                }
            }
        }

        return OverviewSnapshot(
            activeStats = activeStats,
            historyStats = historyStats,
            completionTimeStats = completionTimeStats,
            overdueItems = cumulativeOverdueItems.map { it.item },
            dailyStats = dailyStats,
            monthlyStats = monthlyStats,
            weeklyStats = weeklyStats,
            contributionStats = contributionStats,
            lastMonthDailyStats = lastMonthDailyStats,
            metrics = metrics,
            lastMonthName = lastMonthName,
            lastMonthKey = lastMonthKey,
            completedTaskNames = completedTaskNames,
            metricsSummary = metricsSummary,
        )
    }

    private fun buildCompletionTimeStats(
        context: Context,
        items: List<ParsedItem>,
    ): List<Pair<String, Int>> {
        val labels = listOf(
            context.getString(R.string.time_bucket_late_night),
            context.getString(R.string.time_bucket_morning),
            context.getString(R.string.time_bucket_afternoon),
            context.getString(R.string.time_bucket_evening),
        )
        val counts = linkedMapOf<String, Int>().apply {
            labels.forEach { put(it, 0) }
        }
        items.forEach { parsed ->
            val completeTime = parsed.completeTime ?: return@forEach
            if (!parsed.item.isCompleted) return@forEach
            val bucket = when (completeTime.hour) {
                in 0 until 6 -> labels[0]
                in 6 until 12 -> labels[1]
                in 12 until 18 -> labels[2]
                else -> labels[3]
            }
            counts[bucket] = (counts[bucket] ?: 0) + 1
        }
        return counts.toList()
    }

    private fun computeDailyStats(
        items: List<ParsedItem>,
        days: Int,
        now: LocalDateTime,
    ): List<DailyStat> {
        val completedMap = HashMap<LocalDate, Int>()
        val overdueMap = HashMap<LocalDate, Int>()
        items.forEach { parsed ->
            parsed.completeTime?.takeIf { parsed.item.isCompleted }?.toLocalDate()?.let { date ->
                completedMap[date] = (completedMap[date] ?: 0) + 1
            }
            parsed.endTime?.takeIf { !parsed.item.isCompleted && it.isBefore(now) }?.toLocalDate()?.let { date ->
                overdueMap[date] = (overdueMap[date] ?: 0) + 1
            }
        }

        val today = now.toLocalDate()
        val labelFormatter = DateTimeFormatter.ofPattern("MM-dd")
        return (0 until days).map { index ->
            val date = today.minusDays((days - 1L) - index)
            DailyStat(
                date = date,
                label = date.format(labelFormatter),
                completedCount = completedMap[date] ?: 0,
                overdueCount = overdueMap[date] ?: 0,
            )
        }
    }

    private fun computeMonthlyStats(
        items: List<ParsedItem>,
        months: Int,
        now: LocalDateTime,
    ): List<MonthlyStat> {
        val totalMap = HashMap<YearMonth, Int>()
        val completedMap = HashMap<YearMonth, Int>()
        val overdueCompletedMap = HashMap<YearMonth, Int>()

        items.forEach { parsed ->
            val endTime = parsed.endTime ?: return@forEach
            val bucket = YearMonth.from(endTime)
            totalMap[bucket] = (totalMap[bucket] ?: 0) + 1

            if (parsed.item.isCompleted) {
                completedMap[bucket] = (completedMap[bucket] ?: 0) + 1
                if (parsed.completeTime?.isAfter(endTime) == true) {
                    overdueCompletedMap[bucket] = (overdueCompletedMap[bucket] ?: 0) + 1
                }
            }
        }

        return (0 until months).map { index ->
            val bucket = YearMonth.from(now).minusMonths((months - 1L) - index)
            MonthlyStat(
                month = "${bucket.monthValue}月",
                totalCount = totalMap[bucket] ?: 0,
                completedCount = completedMap[bucket] ?: 0,
                overdueCompletedCount = overdueCompletedMap[bucket] ?: 0,
            )
        }
    }

    private fun computeWeeklyStats(
        items: List<ParsedItem>,
        weeks: Int,
        now: LocalDateTime,
    ): List<WeeklyStat> {
        return (0 until weeks).map { offset ->
            val endOfWindow = now.minusDays((offset * 7L))
            val startOfWindow = endOfWindow.minusDays(6).toLocalDate().atStartOfDay()
            val end = endOfWindow.toLocalDate().atTime(23, 59, 59)
            val count = items.count { parsed ->
                parsed.item.isCompleted &&
                    parsed.completeTime?.let { completedAt ->
                        !completedAt.isBefore(startOfWindow) && !completedAt.isAfter(end)
                    } == true
            }
            WeeklyStat(
                weekLabel = if (offset == 0) {
                    "本周"
                } else {
                    "${offset}周前"
                },
                completedCount = count,
            )
        }.reversed()
    }

    private fun computeContributionStats(
        items: List<ParsedItem>,
        days: Int,
        now: LocalDateTime,
    ): List<ContributionDay> {
        val counts = HashMap<LocalDate, Int>()
        items.forEach { parsed ->
            parsed.completeTime?.takeIf { parsed.item.isCompleted }?.toLocalDate()?.let { date ->
                counts[date] = (counts[date] ?: 0) + 1
            }
        }

        val today = now.toLocalDate()
        return (0 until days).map { index ->
            val date = today.minusDays((days - 1L) - index)
            ContributionDay(
                date = date,
                count = counts[date] ?: 0,
            )
        }
    }

    private fun computeLastMonthDailyStats(
        items: List<ParsedItem>,
        now: LocalDateTime,
    ): List<DailyStat> {
        val lastMonth = YearMonth.from(now).minusMonths(1)
        val start = lastMonth.atDay(1)
        val labelFormatter = DateTimeFormatter.ofPattern("d")
        val completedMap = HashMap<LocalDate, Int>()
        val overdueMap = HashMap<LocalDate, Int>()

        items.forEach { parsed ->
            parsed.completeTime?.takeIf { parsed.item.isCompleted }?.toLocalDate()?.let { date ->
                if (YearMonth.from(date) == lastMonth) {
                    completedMap[date] = (completedMap[date] ?: 0) + 1
                }
            }
            parsed.endTime?.takeIf { !parsed.item.isCompleted && it.isBefore(now) }?.toLocalDate()?.let { date ->
                if (YearMonth.from(date) == lastMonth) {
                    overdueMap[date] = (overdueMap[date] ?: 0) + 1
                }
            }
        }

        return (1..lastMonth.lengthOfMonth()).map { day ->
            val date = start.withDayOfMonth(day)
            DailyStat(
                date = date,
                label = date.format(labelFormatter),
                completedCount = completedMap[date] ?: 0,
                overdueCount = overdueMap[date] ?: 0,
            )
        }
    }

    private fun computeMetrics(
        context: Context,
        items: List<ParsedItem>,
        now: LocalDateTime,
    ): List<Metric> {
        val lastMonth = YearMonth.from(now).minusMonths(1)
        val previousMonth = lastMonth.minusMonths(1)
        val lastMonthItems = items.filter { parsed -> parsed.endTime?.let { YearMonth.from(it) == lastMonth } == true }
        val previousMonthItems = items.filter { parsed -> parsed.endTime?.let { YearMonth.from(it) == previousMonth } == true }

        data class MonthStats(
            val total: Int,
            val completed: Int,
            val completionRate: Double,
            val overdue: Int,
        )

        fun summarize(source: List<ParsedItem>): MonthStats {
            val total = source.size
            val completed = source.count { it.item.isCompleted }
            val completionRate = if (total > 0) completed.toDouble() / total * 100 else 0.0
            val overdue = source.count { !it.item.isCompleted }
            return MonthStats(
                total = total,
                completed = completed,
                completionRate = completionRate,
                overdue = overdue,
            )
        }

        fun metricChange(curr: Double, prev: Double): Pair<String?, Boolean?> {
            if (prev <= 0.0) return null to null
            val diff = curr - prev
            val percent = abs(diff) / prev * 100
            return String.format(Locale.US, "%.1f%%", percent) to (diff < 0)
        }

        val current = summarize(lastMonthItems)
        val previous = summarize(previousMonthItems)
        val result = mutableListOf<Metric>()

        metricChange(current.total.toDouble(), previous.total.toDouble()).let { (change, isDown) ->
            result += Metric(
                label = context.getString(R.string.metric_last_month_total),
                value = current.total.toString(),
                change = change,
                isDown = isDown,
            )
        }
        metricChange(current.completed.toDouble(), previous.completed.toDouble()).let { (change, isDown) ->
            result += Metric(
                label = context.getString(R.string.metric_last_month_completed),
                value = current.completed.toString(),
                change = change,
                isDown = isDown,
            )
        }
        metricChange(current.completionRate, previous.completionRate).let { (change, isDown) ->
            result += Metric(
                label = context.getString(R.string.metric_last_month_completion_rate),
                value = String.format(Locale.US, "%.1f%%", current.completionRate),
                change = change,
                isDown = isDown,
            )
        }
        metricChange(current.overdue.toDouble(), previous.overdue.toDouble()).let { (change, isDown) ->
            result += Metric(
                label = context.getString(R.string.metric_last_month_overdue),
                value = current.overdue.toString(),
                change = change,
                isDown = isDown,
            )
        }

        val bucketLabels = listOf(
            context.getString(R.string.time_bucket_late_night),
            context.getString(R.string.time_bucket_morning),
            context.getString(R.string.time_bucket_afternoon),
            context.getString(R.string.time_bucket_evening),
        )
        val bucketCounts = linkedMapOf<String, Int>().apply {
            bucketLabels.forEach { put(it, 0) }
        }
        lastMonthItems.filter { it.item.isCompleted }.forEach { parsed ->
            val completeTime = parsed.completeTime ?: return@forEach
            val bucket = when (completeTime.hour) {
                in 0 until 6 -> bucketLabels[0]
                in 6 until 12 -> bucketLabels[1]
                in 12 until 18 -> bucketLabels[2]
                else -> bucketLabels[3]
            }
            bucketCounts[bucket] = (bucketCounts[bucket] ?: 0) + 1
        }
        bucketCounts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.let { topBucket ->
            result += Metric(
                label = context.getString(R.string.metric_most_active_period),
                value = topBucket.key,
                change = context.getString(R.string.metric_bucket_completed_count, topBucket.value),
                isDown = null,
            )
        }

        val durations = lastMonthItems.mapNotNull { parsed ->
            val start = parsed.startTime ?: return@mapNotNull null
            val completed = parsed.completeTime ?: return@mapNotNull null
            if (completed.isBefore(start)) {
                return@mapNotNull null
            }
            Duration.between(start, completed)
        }
        if (durations.isNotEmpty()) {
            val avgSeconds = durations.sumOf { it.seconds } / durations.size
            result += Metric(
                label = context.getString(R.string.metric_average_duration),
                value = formatDuration(context, Duration.ofSeconds(avgSeconds)),
            )
        }

        return result
    }

    private fun collectLastMonthCompletedTaskNames(
        items: List<ParsedItem>,
        now: LocalDateTime,
    ): List<String> {
        val lastMonth = YearMonth.from(now).minusMonths(1)
        return items.filter { parsed ->
            parsed.item.isCompleted &&
                parsed.endTime?.let { YearMonth.from(it) == lastMonth } == true
        }.map { it.item.name }
    }

    private fun formatLastMonthName(
        now: LocalDateTime,
        locale: Locale,
    ): String {
        val lastMonthDate = now.minusMonths(1)
        return if (locale.language.startsWith("zh")) {
            DateTimeFormatter.ofPattern("M月", locale).format(lastMonthDate)
        } else {
            lastMonthDate.month.getDisplayName(TextStyle.FULL, locale)
        }
    }

    private fun formatDuration(
        context: Context,
        duration: Duration,
    ): String {
        val totalMinutes = duration.toMinutes()
        return when {
            totalMinutes < 60 -> context.getString(
                R.string.duration_minutes,
                totalMinutes.coerceAtLeast(1),
            )
            totalMinutes < 24 * 60 -> context.getString(
                R.string.duration_hours,
                totalMinutes / 60,
            )
            else -> context.getString(
                R.string.duration_days_hours,
                totalMinutes / (24 * 60),
                (totalMinutes % (24 * 60)) / 60,
            )
        }
    }

    private fun parseDateTimeOrNull(value: String): LocalDateTime? {
        return runCatching { GlobalUtils.parseDateTime(value) }.getOrNull()
    }

    private fun DDLItem.isAbandonedLike(): Boolean = state.isAbandonedFamily()
}
