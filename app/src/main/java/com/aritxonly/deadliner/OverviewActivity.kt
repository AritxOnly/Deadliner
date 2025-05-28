package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import java.time.Duration

// 辅助函数：判断任务是否逾期
// 假设 endTime 格式为 "yyyy-MM-dd HH:mm"
fun isOverdue(item: DDLItem): Boolean {
    return try {
        val end = GlobalUtils.safeParseDateTime(item.endTime)
        val now = LocalDateTime.now()
        !item.isCompleted && now.isAfter(end)
    } catch (e: Exception) {
        false
    }
}

// 辅助函数：根据完成时间提取时间段（00-06, 06-12, 12-18, 18-24）
fun extractTimeBucket(completeTime: String): String {
    return try {
        val time = GlobalUtils.safeParseDateTime(completeTime)
        val hour = time.hour
        when (hour) {
            in 0 until 6 -> "凌晨"
            in 6 until 12 -> "上午"
            in 12 until 18 -> "下午"
            else -> "晚上"
        }
    } catch (e: Exception) {
        "未知"
    }
}

val timeBucketOrder = mapOf(
    "凌晨" to 0,
    "上午" to 1,
    "下午" to 2,
    "晚上" to 3
)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
class OverviewActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APP_COLOR_SCHEME = "EXTRA_APP_COLOR_SCHEME"
        fun newIntent(context: Context, colorScheme: AppColorScheme): Intent {
            return Intent(context, OverviewActivity::class.java).apply {
                putExtra(EXTRA_APP_COLOR_SCHEME, colorScheme)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        val appColorScheme = intent.getParcelableExtra<AppColorScheme>("EXTRA_APP_COLOR_SCHEME")
            ?: throw IllegalArgumentException("Missing AppColorScheme")

        val databaseHelper = DatabaseHelper.getInstance(applicationContext)

        setContent {
            CustomDeadlinerTheme(appColorScheme = appColorScheme) {
                val items = databaseHelper.getAllDDLs()

                // 当前任务：未归档
                val activeItems = items.filter { !it.isArchived }
                val completedItems = activeItems.filter { it.isCompleted }
                val incompleteItems = activeItems.filter { !it.isCompleted && !isOverdue(it) }
                val overdueItems = activeItems.filter { isOverdue(it) }

                // 历史任务：所有任务
                val historyCompleted = items.filter { it.isCompleted }
                val historyIncomplete = items.filter { !it.isCompleted }
                // 对于历史任务，逾期可以按业务需求定义，此处简单统计未完成为逾期
                val historyOverdue = overdueItems

                // 完成时间段统计：针对所有完成的任务，按时间段统计
                val completionTimeStats = historyCompleted.groupBy { extractTimeBucket(it.completeTime) }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedBy { timeBucketOrder[it.first] ?: Int.MAX_VALUE }

                    OverviewScreen(
                        activeStats = mapOf(
                            "已完成" to completedItems.size,
                            "未完成" to incompleteItems.size,
                            "逾期" to overdueItems.size
                        ),
                        historyStats = mapOf(
                            "已完成" to historyCompleted.size,
                            "未完成" to historyIncomplete.size,
                            "逾期" to historyOverdue.size
                        ),
                        completionTimeStats = completionTimeStats,
                        colorScheme = appColorScheme
                    ) {
                        finish()
                    }
            }
        }
    }
}

@Composable
fun hashColor(key: String) : Color {
    val color = when (key) {
        "已完成" -> colorResource(id = R.color.chart_green)
        "未完成" -> colorResource(id = R.color.chart_orange)
        "逾期" -> colorResource(id = R.color.chart_red)
        else -> colorResource(id = R.color.chart_blue)
    }
    return color
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    activeStats: Map<String, Int>,
    historyStats: Map<String, Int>,
    completionTimeStats: List<Pair<String, Int>>,
    colorScheme: AppColorScheme,
    onClose: () -> Unit
) {
    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(Color(colorScheme.surfaceContainer), CircleShape)
        .padding(8.dp)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val fraction by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction }
    }

    val isCollapsed = fraction > 0.5f

    val titleStyle = if (isCollapsed) {
        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)
    } else {
        MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = contentColorFor(Color.Transparent),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text(
                    "概览",
                    color = Color(colorScheme.onSurface),
                    style = titleStyle
                ) },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_back),
                            contentDescription = "关闭",
                            tint = Color(colorScheme.onSurface),
                            modifier = expressiveTypeModifier
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {}
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_more),
                            contentDescription = "更多",
                            tint = Color(colorScheme.onSurface),
                            modifier = expressiveTypeModifier
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color(colorScheme.surface),
                    scrolledContainerColor = Color(colorScheme.surface)
                ),
                modifier = Modifier
                    .background(Color(colorScheme.surface))
                    .padding(horizontal = 8.dp),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .background(Color(colorScheme.surface))
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy((-8).dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(colorScheme.surfaceContainer)
                    ),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimensionResource(id = R.dimen.item_corner_radius))),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(Color(colorScheme.surfaceContainer))
                    ) {
                        Text(
                            text = "活动任务状态统计",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color(colorScheme.onSurface)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            activeStats.forEach { (key, value) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(80.dp)
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(colorScheme.onSurface)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = value.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = hashColor(key = key),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(colorScheme.surfaceContainer)
                    ),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimensionResource(id = R.dimen.item_corner_radius))),
                ) {
                    Column(modifier = Modifier
                        .padding(16.dp)
                        .background(Color(colorScheme.surfaceContainer))) {
                        Text(
                            text = "任务完成时间段统计",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color(colorScheme.onSurface)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // 使用条形图展示完成时间段统计
                        BarChartCompletionTimeStats(
                            data = completionTimeStats,
                            textColor = Color(colorScheme.onSurface)
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(colorScheme.surfaceContainer)
                    ),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimensionResource(id = R.dimen.item_corner_radius))),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "任务状态统计",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color(colorScheme.onSurface)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            historyStats.forEach { (key, value) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(80.dp)
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(colorScheme.onSurface)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = value.toString(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = hashColor(key = key),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            PieChartView(statistics = historyStats)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BarChartCompletionTimeStats(
    data: List<Pair<String, Int>>,
    barColor: Color = colorResource(id = R.color.chart_blue),
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    // 找到最大值用于归一化
    val maxCount = data.maxOfOrNull { it.second } ?: 1
    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
        data.forEach { (timeBucket, count) ->
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = timeBucket, color = textColor)
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
fun PieChartView(statistics: Map<String, Int>, size: Dp = 160.dp) {
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

@Preview(showBackground = true)
@Composable
fun OverviewPreview() {
    DeadlinerTheme {
        OverviewScreen(
            activeStats = mapOf(
                "已完成" to 10,
                "未完成" to 4,
                "逾期" to 5
            ),
            historyStats = mapOf(
                "已完成" to 9,
                "未完成" to 10,
                "逾期" to 14
            ),
            completionTimeStats = listOf(
                "早上" to 5,
                "上午" to 3,
                "下午" to 6
            ),
            colorScheme =  AppColorScheme(
                primary = MaterialTheme.colorScheme.primary.toArgb(),
                onPrimary = MaterialTheme.colorScheme.onPrimary.toArgb(),
                primaryContainer = MaterialTheme.colorScheme.primaryContainer.toArgb(),
                surface = MaterialTheme.colorScheme.surface.toArgb(),
                onSurface = MaterialTheme.colorScheme.onSurface.toArgb(),
                surfaceContainer = MaterialTheme.colorScheme.surfaceContainer.toArgb(),
                secondary = MaterialTheme.colorScheme.secondary.toArgb(),
                onSecondary = MaterialTheme.colorScheme.onSecondary.toArgb(),
                secondaryContainer = MaterialTheme.colorScheme.secondaryContainer.toArgb(),
                onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer.toArgb(),
                tertiary = MaterialTheme.colorScheme.tertiary.toArgb(),
                onTertiary = MaterialTheme.colorScheme.onTertiary.toArgb(),
                tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer.toArgb(),
                onTertiaryContainer = MaterialTheme.colorScheme.onTertiaryContainer.toArgb(),
            )
        ) {}
    }
}