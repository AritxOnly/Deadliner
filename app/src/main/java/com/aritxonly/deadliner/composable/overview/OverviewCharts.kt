package com.aritxonly.deadliner.composable.overview

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.Pie


@Composable
fun BarChartCompletionTimeStats(
    data: List<Pair<String, Int>>,
    barColor: Color = colorResource(id = R.color.chart_blue),
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    // 找到最大值用于归一化
    val maxCount = data.maxOfOrNull { it.second } ?: 1

    val textMeasurer = rememberTextMeasurer()
    val maxLabelWidth = data.maxOf {
        textMeasurer.measure(AnnotatedString(it.first)).size.width
    }

    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
        data.forEach { (timeBucket, count) ->
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeBucket,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier
                        .width(with(LocalDensity.current) { maxLabelWidth.toDp() })
                        .basicMarquee()
                )
                Spacer(modifier = Modifier.width(16.dp))
                Canvas(modifier = Modifier
                    .height(20.dp)
                    .weight(1f)) {
                    val barWidth = (size.width) * (count / maxCount.toFloat())
                    drawRoundRect(
                        color = barColor,
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = count.toString(), color = textColor,
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}

@Composable
fun NewPieChart(
    statistics: Map<String, Int>,
    size: Dp = 160.dp
) {
    val green = colorResource(id = R.color.chart_green)
    val orange = colorResource(id = R.color.chart_orange)
    val red = colorResource(id = R.color.chart_red)
    val blue = colorResource(id = R.color.chart_blue)

    val initialData = remember(statistics) {
        val total = statistics.values.sum().coerceAtLeast(1)
        statistics.entries.mapIndexed { index, entry ->
            Pie(
                label = entry.key,
                data = entry.value.toDouble(),
                color = when (index) {
                    0 -> green
                    1 -> orange
                    2 -> red
                    else -> blue
                },
            )
        }
    }
    var data by remember { mutableStateOf(initialData) }

    PieChart(
        modifier = Modifier.size(size),
        data = data,
        onPieClick = { pie ->
            val idx = data.indexOf(pie)
            data = data.mapIndexed { i, p -> p.copy(selected = i == idx) }
        },
        selectedScale = 1.1f,
        scaleAnimEnterSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        colorAnimEnterSpec = tween(300),
        colorAnimExitSpec = tween(300),
        scaleAnimExitSpec = tween(300),
        spaceDegreeAnimExitSpec = tween(300),
        style = Pie.Style.Fill
    )
}

@Composable
fun PieChartView(
    statistics: Map<String, Int>,
    size: Dp = 160.dp
) {
    val total = statistics.values.sum().toFloat()
    val colors = listOf(
        colorResource(id = R.color.chart_green),
        colorResource(id = R.color.chart_orange),
        colorResource(id = R.color.chart_red)
    )

    Box(
        modifier = Modifier.width(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            // 获取实际绘制区域尺寸
            val canvasSize = size.toPx()
            val centerX = size.toPx() / 2
            val centerY = size.toPx() / 2
            val radius = minOf(canvasSize, canvasSize) / 2

            var startAngle = -90f // 从12点钟方向开始

            statistics.entries.forEachIndexed { index, entry ->
                val sweepAngle = if (total == 0f) 0f else (entry.value / total) * 360f

                drawArc(
                    color = colors.getOrElse(index) { Color.Gray },
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2)
                )
                startAngle += sweepAngle
            }
        }
    }
}