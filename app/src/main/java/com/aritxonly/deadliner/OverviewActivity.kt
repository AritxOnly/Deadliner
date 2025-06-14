package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aritxonly.deadliner.composable.BarChartCompletionTimeStats
import com.aritxonly.deadliner.composable.DailyBarChart
import com.aritxonly.deadliner.composable.DashboardScreen
import com.aritxonly.deadliner.composable.MonthlyTrendChart
import com.aritxonly.deadliner.composable.NewPieChart
import com.aritxonly.deadliner.composable.WeeklyBarChart
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.OverviewUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import kotlin.collections.component1
import kotlin.collections.component2

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
            DeadlinerTheme {
                val items = databaseHelper.getDDLsByType(DeadlineType.TASK)



                    OverviewScreen(
                        items = items,
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

@SuppressLint("SetTextI18n")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    items: List<DDLItem>,
    colorScheme: AppColorScheme,
    onClose: () -> Unit
) {
    // 数据准备
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

    val activeStats = mapOf(
        "已完成" to completedItems.size,
        "未完成" to incompleteItems.size,
        "逾期" to overdueItems.size
    )
    val historyStats = mapOf(
        "已完成" to historyCompleted.size,
        "未完成" to historyIncomplete.size,
        "逾期" to historyOverdue.size
    )

    // 完成时间段统计：针对所有完成的任务，按时间段统计
    val completionTimeStats = historyCompleted.groupBy { extractTimeBucket(it.completeTime) }
        .mapValues { it.value.size }
        .toList()
        .sortedBy { timeBucketOrder[it.first] ?: Int.MAX_VALUE }


    // UI准备
    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(Color(colorScheme.surfaceContainer), CircleShape)
        .padding(8.dp)

    var showSettings by rememberSaveable { mutableStateOf(false) }

    val tabs = listOf("概览统计", "趋势分析", "上月总结")
    val mapIcon = mapOf(
        "概览统计" to painterResource(R.drawable.ic_analytics),
        "趋势分析" to painterResource(R.drawable.ic_monitor),
        "上月总结" to painterResource(R.drawable.ic_dashboard)
    )
    var selectedTab by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = contentColorFor(Color.Transparent),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(
                    "概览",
                    color = Color(colorScheme.onSurface),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)
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
                    Row {
                        IconButton(
                            onClick = { showSettings = true },
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_pref),
                                contentDescription = "更多",
                                tint = Color(colorScheme.onSurface),
                                modifier = expressiveTypeModifier
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color(colorScheme.surface),
                    scrolledContainerColor = Color(colorScheme.surface)
                ),
                modifier = Modifier
                    .background(Color(colorScheme.surface))
                    .padding(horizontal = 8.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .background(Color(colorScheme.surface))
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                divider = { HorizontalDivider(color = Color(colorScheme.surface)) }
            ) {
                tabs.forEachIndexed { i, title ->
                    val icon = mapIcon[title] ?: painterResource(R.drawable.ic_info)
                    Tab(
                        selected = i == selectedTab,
                        onClick = { selectedTab = i },
                        icon = { Icon(icon, contentDescription = title) },
                        text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
            when (selectedTab) {
                0 ->
                    OverviewStatsScreen(
                        activeStats,
                        historyStats,
                        completionTimeStats,
                        Modifier
                            .background(Color(colorScheme.surface)),
                        colorScheme
                    )
                1 ->
                    TrendAnalysisScreen(
                        items,
                        Modifier
                            .background(Color(colorScheme.surface)),
                        colorScheme
                    )
                2 ->
                    DashboardScreen(
                        items,
                        colorScheme,
                        Modifier
                            .background(Color(colorScheme.surface))
                    )
            }
        }

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                confirmButton = {
                    TextButton(onClick = { showSettings = false }) {
                        Text("确认")
                    }
                },
                text = {
                    // 用 AndroidView 加载我们刚才写的 XML 布局
                    AndroidView(
                        factory = { context ->
                            LayoutInflater.from(context)
                                .inflate(R.layout.dialog_overview_settings, null)
                                .apply {
                                    val seek = findViewById<Slider>(R.id.seekbar_monthly)
                                    val tv = findViewById<TextView>(R.id.text_monthly_value)
                                    val sw = findViewById<MaterialSwitch>(R.id.switch_overdue)

                                    // 初始化控件状态
                                    seek.value = (GlobalUtils.OverviewSettings.monthlyCount).toFloat()
                                    tv.text = "${GlobalUtils.OverviewSettings.monthlyCount} 个月"
                                    sw.isChecked = GlobalUtils.OverviewSettings.showOverdueInDaily

                                    // 监听用户操作，实时更新 SharedPreferences
                                    seek.addOnChangeListener { _, _, _ ->
                                        GlobalUtils.OverviewSettings.monthlyCount = seek.value.toInt()
                                        tv.text = "${GlobalUtils.OverviewSettings.monthlyCount} 个月"
                                    }
                                    sw.setOnCheckedChangeListener { _, isChecked ->
                                        GlobalUtils.OverviewSettings.showOverdueInDaily = isChecked
                                    }
                                }
                        },
                        update = { /* nothing to do */ }
                    )
                }
            )
        }
    }
}

@Composable
fun TrendAnalysisScreen(
    items: List<DDLItem>,
    modifier: Modifier = Modifier,
    colorScheme: AppColorScheme
) {
    key(GlobalUtils.OverviewSettings.monthlyCount, GlobalUtils.OverviewSettings.showOverdueInDaily) {

        val dailyCompleted  = OverviewUtils.computeDailyCompletedCounts(items)
        val dailyOverdue    = OverviewUtils.computeDailyOverdueCounts(items)
        val monthlyStat     = OverviewUtils.computeMonthlyTaskStats(items, months = GlobalUtils.OverviewSettings.monthlyCount)
        val weeklyCompleted = OverviewUtils.computeWeeklyCompletedCounts(items)

        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 最近7天完成量
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(colorScheme.surfaceContainer)
                    ),
                    modifier = Modifier
                        .padding(16.dp, 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimensionResource(id = R.dimen.item_corner_radius)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(Color(colorScheme.surfaceContainer))
                    ) {
                        Text(
                            text = "周完成量",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color(colorScheme.onSurface)
                        )
                        Spacer(Modifier.height(8.dp))
                        DailyBarChart(
                            data = dailyCompleted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            barColor = hashColor(""),
                            overdueColor = hashColor("逾期")
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
                        .padding(16.dp, 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimensionResource(id = R.dimen.item_corner_radius)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(Color(colorScheme.surfaceContainer))
                    ) {
                        Text(
                            text = "月度趋势",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color(colorScheme.onSurface)
                        )
                        Spacer(Modifier.height(8.dp))
                        MonthlyTrendChart(
                            data = monthlyStat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            totalColor = colorResource(R.color.chart_blue),
                            completedColor = colorResource(R.color.chart_green),
                            overdueColor = colorResource(R.color.chart_red)
                        )
                    }
                }
            }

            // 最近4周完成量
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(colorScheme.surfaceContainer)
                    ),
                    modifier = Modifier
                        .padding(16.dp, 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimensionResource(id = R.dimen.item_corner_radius)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(Color(colorScheme.surfaceContainer))
                    ) {
                        Text(
                            text = "最近4周完成量",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color(colorScheme.onSurface)
                        )
                        Spacer(Modifier.height(8.dp))
                        WeeklyBarChart(
                            data = weeklyCompleted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            barColor = Color(colorScheme.secondary)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun OverviewStatsScreen(
    activeStats: Map<String, Int>,
    historyStats: Map<String, Int>,
    completionTimeStats: List<Pair<String, Int>>,
    modifier: Modifier,
    colorScheme: AppColorScheme
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(colorScheme.surfaceContainer)
                ),
                modifier = Modifier
                    .padding(16.dp, 8.dp)
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
                    .padding(16.dp, 8.dp)
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
                    .padding(16.dp, 8.dp)
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
                        NewPieChart(statistics = historyStats)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Preview(showBackground = true)
@Composable
fun OverviewPreview() {
    DeadlinerTheme {
        val items = listOf<DDLItem>(
            DDLItem(
                id = 1,
                name = "提交报告",
                startTime = "2025-06-07 09:00",
                endTime = "2025-06-07 12:00",
                isCompleted = true,
                completeTime = "2025-06-07 11:30",
                note = "按时完成",
                type = DeadlineType.TASK
            ),
            DDLItem(
                id = 2,
                name = "团队会议准备",
                startTime = "2025-06-08 14:00",
                endTime = "2025-06-08 16:00",
                isCompleted = true,
                completeTime = "2025-06-08 15:45",
                note = "幻灯片已更新",
                type = DeadlineType.TASK
            ),
            DDLItem(
                id = 3,
                name = "代码重构",
                startTime = "2025-06-10 10:00",
                endTime = "2025-06-10 18:00",
                isCompleted = false,
                completeTime = "",
                note = "进行中",
                type = DeadlineType.TASK
            ),
            DDLItem(
                id = 4,
                name = "发布新版本",
                startTime = "2025-06-12 08:00",
                endTime = "2025-06-12 12:00",
                isCompleted = true,
                completeTime = "2025-06-12 11:50",
                note = "已部署至服务器",
                type = DeadlineType.TASK
            ),
            DDLItem(
                id = 5,
                name = "撰写周报",
                startTime = "2025-06-13 17:00",
                endTime = "2025-06-13 19:00",
                isCompleted = false,
                completeTime = "",
                note = "待完成",
                type = DeadlineType.TASK
            ),
            // 逾期任务示例
            DDLItem(
                id = 6,
                name = "更新文档",
                startTime = "2025-06-05 09:00",
                endTime = "2025-06-09 17:00",
                isCompleted = false,
                completeTime = "",
                note = "过期未完成",
                type = DeadlineType.TASK
            )
        )
        OverviewScreen(
            items = items,
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