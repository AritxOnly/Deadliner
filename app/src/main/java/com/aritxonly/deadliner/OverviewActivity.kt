package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import java.time.LocalDateTime
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aritxonly.deadliner.composable.overview.DashboardScreen
import com.aritxonly.deadliner.composable.overview.OverviewStatsScreen
import com.aritxonly.deadliner.composable.overview.TrendAnalysisScreen
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.AppColorScheme
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import okhttp3.internal.toHexString
import java.time.LocalDate

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
fun extractTimeBucket(context: Context, completeTime: String): String {
    return try {
        val time = GlobalUtils.safeParseDateTime(completeTime) // LocalDateTime
        val hour = time.hour
        when (hour) {
            in 0 until 6  -> context.getString(R.string.time_bucket_late_night)
            in 6 until 12 -> context.getString(R.string.time_bucket_morning)
            in 12 until 18-> context.getString(R.string.time_bucket_afternoon)
            in 18..23     -> context.getString(R.string.time_bucket_evening)
            else          -> context.getString(R.string.time_bucket_unknown)
        }
    } catch (_: Exception) {
        context.getString(R.string.time_bucket_unknown)
    }
}

fun buildTimeBucketOrder(context: Context): Map<String, Int> = mapOf(
    context.getString(R.string.time_bucket_late_night) to 0,
    context.getString(R.string.time_bucket_morning)    to 1,
    context.getString(R.string.time_bucket_afternoon)  to 2,
    context.getString(R.string.time_bucket_evening)    to 3
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

    private class ColorSchemeHelper(val context: Context) {
        val defaultColorScheme = AppColorScheme(
            primary = getThemeColor(androidx.appcompat.R.attr.colorPrimary),
            onPrimary = getMaterialThemeColor(com.google.android.material.R.attr.colorOnPrimary),
            primaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorPrimaryContainer),
            surface = getMaterialThemeColor(com.google.android.material.R.attr.colorSurface),
            onSurface = getMaterialThemeColor(com.google.android.material.R.attr.colorOnSurface),
            surfaceContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorSurfaceContainer),
            secondary = getMaterialThemeColor(com.google.android.material.R.attr.colorSecondary),
            onSecondary = getMaterialThemeColor(com.google.android.material.R.attr.colorOnSecondary),
            secondaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorSecondaryContainer),
            onSecondaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorOnSecondaryContainer),
            tertiary = getMaterialThemeColor(com.google.android.material.R.attr.colorTertiary),
            onTertiary = getMaterialThemeColor(com.google.android.material.R.attr.colorOnTertiary),
            tertiaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorTertiaryContainer),
            onTertiaryContainer = getMaterialThemeColor(com.google.android.material.R.attr.colorOnTertiaryContainer),
        )

        /**
         * 获取主题颜色
         * @param attributeId 主题属性 ID
         * @return 颜色值
         */
        private fun getThemeColor(attributeId: Int): Int {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(attributeId, typedValue, true)
            Log.d("ThemeColor", "getColor $attributeId: ${typedValue.data.toHexString()}")
            return typedValue.data
        }

        private fun getMaterialThemeColor(attributeId: Int): Int {
            return MaterialColors.getColor(
                ContextWrapper(context),
                attributeId,
                android.graphics.Color.WHITE
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        window.isNavigationBarContrastEnforced = false

        super.onCreate(savedInstanceState)

        val appColorScheme = intent.getParcelableExtra<AppColorScheme>("EXTRA_APP_COLOR_SCHEME")
            ?: ColorSchemeHelper(this).defaultColorScheme

        setContent {
            DeadlinerTheme {
                val items = DDLRepository().getDDLsByType(DeadlineType.TASK)

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
        stringResource(R.string.today_completed), stringResource(R.string.completed) -> colorResource(id = R.color.chart_green)
        stringResource(R.string.incomplete) -> colorResource(id = R.color.chart_orange)
        stringResource(R.string.today_overdue), stringResource(R.string.overdue) -> colorResource(id = R.color.chart_red)
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
    val context = LocalContext.current

    // 数据准备
    // 当前任务：未归档
    val activeItems = items.filter { !it.isArchived }
    val completedItems = activeItems.filter {
        val completeTime = GlobalUtils.parseDateTime(it.completeTime) ?: return@filter false
        val completeDateIsToday = (completeTime.toLocalDate() == LocalDate.now())
        it.isCompleted && completeDateIsToday
    }
    val incompleteItems = activeItems.filter { !it.isCompleted && !isOverdue(it) }
    val overdueItems = activeItems.filter {
        Log.d("OverviewDebug", "I reach here with ${it.endTime}")
        val endTime = GlobalUtils.parseDateTime(it.endTime) ?: return@filter false
        val endDateIsToday = (endTime.toLocalDate() == LocalDate.now())
        Log.d("OverviewDebug", endDateIsToday.toString())
        endDateIsToday && !it.isCompleted
    }

    val historyCompleted = items.filter { it.isCompleted }
    val historyIncomplete = items.filter { !it.isCompleted }
    val historyOverdue = activeItems.filter { isOverdue(it) }

    val activeStats = mapOf(
        stringResource(R.string.today_completed) to completedItems.size,
        stringResource(R.string.incomplete) to incompleteItems.size,
        stringResource(R.string.today_overdue) to overdueItems.size
    )
    val historyStats = mapOf(
        stringResource(R.string.completed) to historyCompleted.size,
        stringResource(R.string.incomplete) to historyIncomplete.size,
        stringResource(R.string.overdue) to historyOverdue.size
    )

    // 完成时间段统计：针对所有完成的任务，按时间段统计
    val timeBucketOrder = buildTimeBucketOrder(context)

    val completionTimeStats = historyCompleted.groupBy { extractTimeBucket(context, it.completeTime) }
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

    val tabs = listOf(
        stringResource(R.string.tab_overview),
        stringResource(R.string.tab_trend),
        stringResource(R.string.tab_summary)
    )
    val mapIcon = mapOf(
        stringResource(R.string.tab_overview) to painterResource(R.drawable.ic_analytics),
        stringResource(R.string.tab_trend) to painterResource(R.drawable.ic_monitor),
        stringResource(R.string.tab_summary) to painterResource(R.drawable.ic_dashboard)
    )
    var selectedTab by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = contentColorFor(Color.Transparent),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(
                    stringResource(R.string.title_activity_overview),
                    color = Color(colorScheme.onSurface),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)
                ) },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.close),
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
                                contentDescription = stringResource(R.string.settings_more),
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
                        overdueItems,
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
                        Text(stringResource(R.string.accept))
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
                                    tv.text = context.getString(R.string.xx_months, GlobalUtils.OverviewSettings.monthlyCount)
                                    sw.isChecked = GlobalUtils.OverviewSettings.showOverdueInDaily

                                    // 监听用户操作，实时更新 SharedPreferences
                                    seek.addOnChangeListener { _, _, _ ->
                                        GlobalUtils.OverviewSettings.monthlyCount = seek.value.toInt()
                                        tv.text = context.getString(R.string.xx_months, GlobalUtils.OverviewSettings.monthlyCount)
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
            colorScheme = AppColorScheme(
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