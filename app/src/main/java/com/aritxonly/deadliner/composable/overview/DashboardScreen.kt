package com.aritxonly.deadliner.composable.overview

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.AppColorScheme
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.composable.AnimatedItem
import com.aritxonly.deadliner.composable.TintedGradientImage
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    items: List<DDLItem>,
    colorScheme: AppColorScheme,
    modifier: Modifier = Modifier
) {
    val now = LocalDateTime.now()
    // 计算上一个日历月的起始和结束时间
    val lastMonthYearMonth = YearMonth.from(now).minusMonths(1)
    val lastMonthStart = lastMonthYearMonth.atDay(1).atStartOfDay()
    val lastMonthEnd = lastMonthYearMonth.atEndOfMonth().atTime(23, 59, 59)
    // 计算前上一个日历月的起始和结束时间
    val prevMonthYearMonth = lastMonthYearMonth.minusMonths(1)
    val prevMonthStart = prevMonthYearMonth.atDay(1).atStartOfDay()
    val prevMonthEnd = prevMonthYearMonth.atEndOfMonth().atTime(23, 59, 59)

    // 计算特定月份统计
    val lastStats = collectStatsInRange(items, lastMonthStart, lastMonthEnd)
    val prevStats = collectStatsInRange(items, prevMonthStart, prevMonthEnd)

    // 指标列表
    val (totalChange, totalDown) = computeChange(lastStats.total, prevStats.total)
    val (completedChange, completedDown) = computeChange(lastStats.completed, prevStats.completed)
    val (overdueChange, overdueDown) = computeChange(lastStats.overdue, prevStats.overdue)

    val (rateChange, rateDown) = formatRateChange(
        lastStats.completed, lastStats.total,
        prevStats.completed, prevStats.total
    )
    val (overdueRateChange, overdueRateDown) = formatRateChange(
        lastStats.overdue, lastStats.total,
        prevStats.overdue, prevStats.total
    )

    val monthName = lastMonthYearMonth.month.getDisplayName(
        TextStyle.FULL, Locale.CHINESE
    )

    val metrics = listOf(
        Metric(
            label = "${lastMonthYearMonth.year}年",
            value = "$monthName"
        ),
        Metric(
            label = "任务总数",
            value = lastStats.total.toString(),
            change = totalChange,
            isDown = totalDown
        ),
        Metric(
            label = "完成数",
            value = lastStats.completed.toString(),
            change = completedChange,
            isDown = completedDown
        ),
        Metric(
            label = "逾期数",
            value = lastStats.overdue.toString(),
            change = overdueChange,
            isDown = overdueDown
        ),
        Metric(
            label = "完成率",
            value = formatRate(lastStats.completed, lastStats.total),
            change = rateChange,
            isDown = rateDown
        ),
        Metric(
            label = "逾期率",
            value = formatRate(lastStats.overdue, lastStats.total),
            change = overdueRateChange,
            isDown = overdueRateDown
        ),
        Metric(
            label = "平均完成时长",
            value = formatDuration(lastStats.avgCompletionTime)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(colorScheme.surface))
            .padding(16.dp, 0.dp)
    ) {
        SummaryGrid(metrics, colorScheme)
    }
}

// 数据模型
private data class MonthStats(
    val total: Int,
    val completed: Int,
    val overdue: Int,
    val avgCompletionTime: Duration
)

private fun collectStatsInRange(
    items: List<DDLItem>,
    start: LocalDateTime,
    end: LocalDateTime
): MonthStats {
    val filtered = items.mapNotNull { item ->
        val ctStr = item.completeTime.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val ct = GlobalUtils.safeParseDateTime(ctStr)
        Pair(item, ct)
    }.filter { (_, ct) ->
        !ct.isBefore(start) && !ct.isAfter(end)
    }

    val total = filtered.size
    val completed = filtered.count { it.first.isCompleted }
    val overdue = filtered.count { isOverdue(it.first) }

    val durations = filtered.mapNotNull { (item, ct) ->
        GlobalUtils.safeParseDateTime(item.startTime).let { st ->
            Duration.between(st, ct)
        }
    }
    val avgDuration = if (durations.isNotEmpty()) durations.reduce { a, b -> a.plus(b) }.dividedBy(durations.size.toLong())
    else Duration.ZERO

    return MonthStats(total, completed, overdue, avgDuration)
}

private fun computeChange(current: Int, previous: Int): Pair<String?, Boolean?> {
    var isDown: Boolean? = null
    return when {
        previous == 0 && current > 0 -> ""
        previous == 0 -> null
        else -> {
            val diff = current - previous
            isDown = if (diff == 0) null else (diff < 0)
            "${abs(diff)}"
        }
    } to isDown
}

private fun formatRate(part: Int, total: Int): String =
    if (total > 0) "${(part * 100 / total)}%" else "--"

private fun formatRateChange(
    part: Int,
    total: Int,
    prevPart: Int,
    prevTotal: Int
): Pair<String?, Boolean?> {
    val rateNow = if (total > 0) part * 100 / total else return null to null
    val ratePrev = if (prevTotal > 0) prevPart * 100 / prevTotal else return null to null
    val diff = rateNow - ratePrev
    return when {
        diff > 0 -> "${diff}%"
        diff < 0 -> "${-diff}%"
        else -> ""
    } to when {
        diff > 0 -> false
        diff < 0 -> true
        else -> null
    }
}

private fun formatDuration(duration: Duration): String {
    val days = duration.toDaysPart()
    val hours = duration.toHoursPart()
    return when {
        days > 0 -> "${days}天 ${hours}小时"
        hours > 0 -> "${hours}小时"
        else -> "<1小时"
    }
}

// 瀑布流网格
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SummaryGrid(
    metrics: List<Metric>,
    colorScheme: AppColorScheme
) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibleState.targetState = true }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            AnimatedItem(delayMillis = 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(192.dp)
                        .clipToBounds()
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)))
                ) {
                    TintedGradientImage(
                        R.drawable.dashboard_background,
                        tintColor = Color(colorScheme.primary),
                        modifier = Modifier.matchParentSize(),
                        contentDescription = "背景"
                    )
                    Text(
                        text = "上月总结",
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                        fontWeight = FontWeight.Black,
                        color = Color(colorScheme.onPrimary),
                        modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
                    )
                }
            }
        }
        itemsIndexed(metrics) { index, metric ->
            AnimatedItem(delayMillis = (index + 1) * 100L) {
                SummaryCard(metric, colorScheme)
            }
        }
        item(span = StaggeredGridItemSpan.FullLine) { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// 单个卡片
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SummaryCard(
    metric: Metric,
    colorScheme: AppColorScheme
) {
    Card(
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.item_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = Color(colorScheme.surfaceContainer)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = metric.value,
                style = MaterialTheme.typography.headlineMediumEmphasized,
                fontWeight = FontWeight.Bold,
                color = Color(colorScheme.onSurface)
            )
            metric.change?.takeIf { it.isNotEmpty() }?.let { change ->
                val isDown = metric.isDown

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDown != null) {
                        val arrow =
                            if (isDown) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
                        Icon(
                            imageVector = arrow,
                            contentDescription = null,
                            tint = if (isDown) colorResource(R.color.chart_red) else colorResource(R.color.chart_green),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = change,
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (isDown) {
                            true -> colorResource(R.color.chart_red)
                            false -> colorResource(R.color.chart_green)
                            null -> colorResource(R.color.chart_blue)
                        }
                    )
                }
            }
        }
    }
}

private data class Metric(
    val label: String,
    val value: String,
    val change: String? = null,
    val isDown: Boolean? = null
)

// 逾期判断
private fun isOverdue(item: DDLItem): Boolean {
    if (!item.isCompleted) return true
    val end = GlobalUtils.safeParseDateTime(item.endTime)
    val complete = GlobalUtils.safeParseDateTime(item.completeTime)
    return end.isBefore(complete)
}
