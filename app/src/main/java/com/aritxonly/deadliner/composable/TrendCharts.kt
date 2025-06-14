package com.aritxonly.deadliner.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun DailyBarChart(
    data: List<Pair<LocalDate, Int>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) {
        Text("暂无数据", modifier = modifier)
        return
    }
    val maxValue = data.maxOf { it.second }.coerceAtLeast(1)

    Column(modifier = modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // 绘制柱状图
            Canvas(modifier = Modifier.fillMaxSize()) {
                val totalW = size.width
                val barW = totalW / data.size
                data.forEachIndexed { i, (_, count) ->
                    val left = i * barW + barW * 0.1f
                    val barH = (count / maxValue.toFloat()) * size.height
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, size.height - barH),
                        size = Size(barW * 0.8f, barH),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }
            // 数值标签行，用 weight 平分宽度
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalAlignment = Alignment.Top
            ) {
                data.forEach { (_, count) ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // 日期标签行，同样用 weight
        Row(modifier = Modifier.fillMaxWidth()) {
            data.forEach { (date, _) ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun DailyLineChart(
    data: List<Pair<LocalDate, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.secondary
) {
    if (data.isEmpty()) {
        Text("暂无数据", modifier = modifier)
        return
    }
    val maxValue = data.maxOf { it.second }.coerceAtLeast(1.0)

    Column(modifier = modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val stepX = w / (data.size - 1).coerceAtLeast(1)
                data.forEachIndexed { idx, (_, value) ->
                    val x = idx * stepX
                    val y = h - (value / maxValue * h).toFloat()
                    if (idx > 0) {
                        val prev = data[idx - 1].second
                        val prevX = (idx - 1) * stepX
                        val prevY = h - (prev / maxValue * h).toFloat()
                        drawLine(
                            color = lineColor,
                            strokeWidth = 3.dp.toPx(),
                            start = Offset(prevX, prevY),
                            end = Offset(x, y)
                        )
                    }
                    // 绘制节点
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
            // 数值标签行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalAlignment = Alignment.Top
            ) {
                data.forEach { (_, value) ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = String.format("%.0f", value),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            data.forEach { (date, _) ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyBarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.tertiary
) {
    if (data.isEmpty()) {
        Text("暂无数据", modifier = modifier)
        return
    }
    val maxValue = data.maxOf { it.second }.coerceAtLeast(1)

    Column(modifier = modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val barW = w / data.size
                data.forEachIndexed { idx, (_, count) ->
                    val left = idx * barW + barW * 0.1f
                    val barH = (count / maxValue.toFloat()) * h
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, h - barH),
                        size = Size(barW * 0.8f, barH),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }
            // 数值标签行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalAlignment = Alignment.Top
            ) {
                data.forEach { (_, count) ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            data.forEach { (label, _) ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
