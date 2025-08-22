package com.aritxonly.deadliner.composable.overview

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.OverviewUtils.MonthlyStat
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.extensions.format
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.PopupProperties
import java.time.LocalDate


internal val DefaultLabelProperties
    @Composable get() = LabelProperties(enabled = true,
        textStyle = TextStyle.Default.copy(color = MaterialTheme.colorScheme.onSurface)
    )

private val DefaultIndicatorProperties
    @Composable get() = HorizontalIndicatorProperties(enabled = true,
        textStyle = TextStyle.Default.copy(color = MaterialTheme.colorScheme.onSurface)
    )

internal val DefaultLabelHelperProperties
    @Composable get() = LabelHelperProperties(enabled = true,
        textStyle = TextStyle.Default.copy(color = MaterialTheme.colorScheme.onSurface)
    )

internal val DefaultPopupProperties
    @Composable get() = PopupProperties(
        enabled = true,
        animationSpec = tween(300),
        duration = 2000L,
        textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onTertiaryContainer),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        cornerRadius = 8.dp,
        contentHorizontalPadding = 4.dp,
        contentVerticalPadding = 2.dp
    )

@Composable
fun DailyBarChart(
    data: List<Triple<LocalDate, Int, Int>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    overdueColor: Color = MaterialTheme.colorScheme.error
) {
    if (data.isEmpty()) {
        Text(
            stringResource(com.aritxonly.deadliner.R.string.no_data_yet),
            modifier = modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface
        )
        return
    }

    // 转换为 ComposeCharts 的 Bars 格式
    val completedText = stringResource(com.aritxonly.deadliner.R.string.completed)
    val overdueDoneText = stringResource(com.aritxonly.deadliner.R.string.overdue_done)

    val chartData = remember(data) {
        data.map { (date, count, overdue) ->
            Bars(
                label = date.dayOfMonth.toString(),
                values = if (GlobalUtils.OverviewSettings.showOverdueInDaily) listOf(
                    Bars.Data(value = count.toDouble(), color = Brush.verticalGradient(
                        listOf<Color>(barColor, barColor.copy(alpha = 0.5f))
                    ), label = completedText),
                    Bars.Data(value = overdue.toDouble(), color = Brush.verticalGradient(
                        listOf<Color>(overdueColor, overdueColor.copy(alpha = 0.5f))
                    ), label = overdueDoneText)
                ) else listOf(
                    Bars.Data(value = count.toDouble(), color = Brush.verticalGradient(
                        listOf<Color>(barColor, barColor.copy(alpha = 0.5f))
                    ), label = overdueDoneText)
                )
            )
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
        ColumnChart(
            data = chartData,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            barProperties = BarProperties(
                cornerRadius = Bars.Data.Radius.Rectangle(topRight = 4.dp, topLeft = 4.dp),
                spacing = 1.dp,
                thickness = if (GlobalUtils.OverviewSettings.showOverdueInDaily) 15.dp else 28.dp
            ),
            animationMode = AnimationMode.Together(delayBuilder = { it * 100L }),
            popupProperties = DefaultPopupProperties.copy(
                contentBuilder = { dataIndex, valueIndex, value ->
                    "${data[dataIndex].first}: ${value.format(0)}"
                }
            ),
            indicatorProperties = HorizontalIndicatorProperties(enabled = false),
            labelProperties = DefaultLabelProperties,
            gridProperties = GridProperties(
                enabled = false,
            ),
            labelHelperProperties = DefaultLabelHelperProperties,
        )
    }
}

@Composable
fun MonthlyTrendChart(
    data: List<MonthlyStat>,
    modifier: Modifier = Modifier,
    totalColor: Color = MaterialTheme.colorScheme.primary,
    completedColor: Color = MaterialTheme.colorScheme.secondary,
    overdueColor: Color = MaterialTheme.colorScheme.tertiary
) {
    // 获取三维度统计
    val stats by remember { mutableStateOf(data) }
    if (stats.isEmpty()) {
        Text(stringResource(com.aritxonly.deadliner.R.string.no_data_yet), modifier = modifier.padding(16.dp))
        return
    }

    val totalTasksText = stringResource(com.aritxonly.deadliner.R.string.total_tasks)
    val completedText = stringResource(com.aritxonly.deadliner.R.string.completed)
    val overdueDoneText = stringResource(com.aritxonly.deadliner.R.string.overdue_done)

    // 构造 3 条折线数据
    val series = remember(stats) {
        listOf(
            Line(
                label = totalTasksText,
                values = stats.map { it.total.toDouble() },
                color = Brush.verticalGradient(
                    listOf<Color>(totalColor, totalColor.copy(alpha = .5f))
                ),
                firstGradientFillColor = totalColor.copy(alpha = .5f),
                secondGradientFillColor = Color.Transparent,
                strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                gradientAnimationDelay = 1000,
                drawStyle = DrawStyle.Stroke(width = 2.dp),
                dotProperties = DotProperties(
                    enabled = true,
                    color = SolidColor(Color.White),
                    strokeColor = SolidColor(totalColor),
                )
            ),
            Line(
                label = completedText,
                values = stats.map { it.completed.toDouble() },
                color = Brush.verticalGradient(
                    listOf<Color>(completedColor, completedColor.copy(alpha = .5f))
                ),
                firstGradientFillColor = completedColor.copy(alpha = .5f),
                secondGradientFillColor = Color.Transparent,
                strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                gradientAnimationDelay = 1000,
                drawStyle = DrawStyle.Stroke(width = 2.dp),
                dotProperties = DotProperties(
                    enabled = true,
                    color = SolidColor(Color.White),
                    strokeColor = SolidColor(completedColor),
                )
            ),
            Line(
                label = overdueDoneText,
                values = stats.map { it.overdueCompleted.toDouble() },
                color = Brush.verticalGradient(
                    listOf<Color>(overdueColor, overdueColor.copy(alpha = .5f))
                ),
                firstGradientFillColor = overdueColor.copy(alpha = .5f),
                secondGradientFillColor = Color.Transparent,
                strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                gradientAnimationDelay = 1000,
                drawStyle = DrawStyle.Stroke(width = 2.dp),
                dotProperties = DotProperties(
                    enabled = true,
                    color = SolidColor(Color.White),
                    strokeColor = SolidColor(overdueColor),
                )
            )
        )
    }

    Column(modifier = modifier.padding(16.dp)) {
        LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            data = series,
            animationMode = AnimationMode.Together(delayBuilder = { it * 300L }),
            gridProperties = GridProperties(enabled = false),
            popupProperties = DefaultPopupProperties.copy(
                contentBuilder = { dataIndex, valueIndex, value ->
                    "${data[valueIndex].month}: ${value.format(0)}"
                }
            ),
            indicatorProperties = DefaultIndicatorProperties,
            labelProperties = DefaultLabelProperties,
            labelHelperProperties = DefaultLabelHelperProperties,
        )
    }
}

@Composable
fun WeeklyBarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.secondary
) {
    if (data.isEmpty()) {
        Text(stringResource(com.aritxonly.deadliner.R.string.no_data_yet), modifier = modifier.padding(16.dp))
        return
    }

    // 转换为 ComposeCharts 的 Bars 格式
    val completedText = stringResource(com.aritxonly.deadliner.R.string.completed)

    val chartData = remember(data) {
        data.map { (label, count) ->
            Bars(
                label = label,
                values = listOf(
                    Bars.Data(
                        value = count.toDouble(),
                        color = Brush.verticalGradient(
                            listOf<Color>(barColor, barColor.copy(alpha = 0.5f))
                        ),
                        label = completedText
                    )
                )
            )
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
        ColumnChart(
            data = chartData,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            barProperties = BarProperties(
                cornerRadius = Bars.Data.Radius.Rectangle(topRight = 4.dp, topLeft = 4.dp),
                spacing = 4.dp,
                thickness = 32.dp
            ),
            animationMode = AnimationMode.Together(delayBuilder = { it * 100L }),
            labelHelperProperties = DefaultLabelHelperProperties,
            popupProperties = DefaultPopupProperties.copy(
                contentBuilder = { dataIndex, valueIndex, value ->
                    "${data[valueIndex].first}: ${value.format(0)}"
                }
            ),
            indicatorProperties = HorizontalIndicatorProperties(enabled = false),
            labelProperties = DefaultLabelProperties,
            gridProperties = GridProperties(enabled = false)
        )
    }
}
