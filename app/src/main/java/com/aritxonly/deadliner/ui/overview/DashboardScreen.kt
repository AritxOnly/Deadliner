package com.aritxonly.deadliner.ui.overview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.OverviewActivity
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.AppColorScheme
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.AnimatedItem
import com.aritxonly.deadliner.ui.TintedGradientImage
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.main.simplified.fadingTopEdge
import com.aritxonly.deadliner.ui.poster.ExportDashboardData
import com.aritxonly.deadliner.ui.poster.ShareDashboardPoster
import com.aritxonly.deadliner.ui.poster.renderPosterToCacheUri
import com.aritxonly.deadliner.ui.poster.saveImageToGallery
import com.aritxonly.deadliner.ui.poster.shareImage
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    items: List<DDLItem>,
    colorScheme: AppColorScheme,
    activity: OverviewActivity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
        TextStyle.FULL, Locale.getDefault()
    )

    val metrics = listOf(
        Metric(
            label = stringResource(R.string.year_format, lastMonthYearMonth.year),
            value = "$monthName"
        ),
        Metric(
            label = stringResource(R.string.total_tasks_numbers),
            value = lastStats.total.toString(),
            change = totalChange,
            isDown = totalDown
        ),
        Metric(
            label = stringResource(R.string.completed_number),
            value = lastStats.completed.toString(),
            change = completedChange,
            isDown = completedDown
        ),
        Metric(
            label = stringResource(R.string.overdue_number),
            value = lastStats.overdue.toString(),
            change = overdueChange,
            isDown = overdueDown
        ),
        Metric(
            label = stringResource(R.string.complete_rate),
            value = formatRate(lastStats.completed, lastStats.total),
            change = rateChange,
            isDown = rateDown
        ),
        Metric(
            label = stringResource(R.string.overdue_rate),
            value = formatRate(lastStats.overdue, lastStats.total),
            change = overdueRateChange,
            isDown = overdueRateDown
        ),
        Metric(
            label = stringResource(R.string.average_ddl_time),
            value = formatDuration(lastStats.avgCompletionTime, context)
        )
    )

    var exporting by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var chosenTone by remember { mutableStateOf(TextTone.Light) }
    var chosenBg by remember { mutableStateOf<Bitmap?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(colorScheme.surface))
            .padding(16.dp, 0.dp)
    ) {
        SummaryGrid(metrics, colorScheme) {
            if (!exporting) showExportDialog = true
        }
    }

    ExportOptionsDialog(
        open = showExportDialog,
        onDismiss = { showExportDialog = false },
        onConfirm = { tone, bg ->
            showExportDialog = false
            chosenTone = tone
            chosenBg = bg

            if (exporting) return@ExportOptionsDialog
            exporting = true
            scope.launch {
                try {
                    val generatedTime = LocalDateTime.now()
                    val exportData = ExportDashboardData(
                        monthText = context.getString(R.string.summary_with_text, monthName),
                        metrics = metrics.drop(1),
                        generatedAt = generatedTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    )
                    val uri = renderPosterToCacheUri(
                        activity = activity,
                        data = exportData,
                        widthPx = 1080,
                        backgroundBitmap = chosenBg,
                        textTone = chosenTone
                    )
                    val saved = saveImageToGallery(context, uri, "Deadliner_summary_${generatedTime}")
                    Toast.makeText(context, R.string.saved_to_gallery, Toast.LENGTH_SHORT).show()
                    shareImage(activity, saved)
                } finally {
                    exporting = false
                }
            }
        }
    )
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

private fun formatDuration(duration: Duration, context: Context): String {
    val days = duration.toDaysPart()
    val hours = duration.toHoursPart()
    return when {
        days > 0 -> context.getString(R.string.duration_days_hours, days, hours)
        hours > 0 -> context.getString(R.string.duration_hours, hours)
        else -> context.getString(R.string.duration_less_than_hour)
    }
}

// 瀑布流网格
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SummaryGrid(
    metrics: List<Metric>,
    colorScheme: AppColorScheme,
    onShare: () -> Unit,
) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibleState.targetState = true }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fadingTopEdge(height = 4.dp)
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
                        contentDescription = stringResource(R.string.background)
                    )
                    Text(
                        text = stringResource(R.string.last_month_summary),
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                        fontWeight = FontWeight.Black,
                        color = Color(colorScheme.onPrimary),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    )
                }
            }
        }

        itemsIndexed(metrics) { index, metric ->
            AnimatedItem(delayMillis = (index + 1) * 100L) {
                SummaryCard(metric, colorScheme)
            }
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.textButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Row {
                            Icon(
                                iconResource(R.drawable.ic_share),
                                contentDescription = stringResource(R.string.share),
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.CenterVertically)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(stringResource(R.string.share))
                        }
                    }
                }
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
                            if (isDown) ImageVector.vectorResource(R.drawable.ic_arrow_down) else ImageVector.vectorResource(R.drawable.ic_arrow_up)
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

data class Metric(
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

enum class TextTone {
    Light, Dark
}

@Composable
fun ExportOptionsDialog(
    open: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (TextTone, Bitmap?) -> Unit
) {
    if (!open) return

    val context = LocalContext.current
    var tone by remember { mutableStateOf(TextTone.Light) }
    var bgBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            bgBitmap = runCatching {
                decodeUriAsSoftwareBitmap(context, uri, maxWidth = 1440)
            }.getOrNull()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.text_tone))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterChip(
                        selected = tone == TextTone.Light,
                        onClick = { tone = TextTone.Light },
                        label = { Text(stringResource(R.string.light_text)) }
                    )
                    FilterChip(
                        selected = tone == TextTone.Dark,
                        onClick = { tone = TextTone.Dark },
                        label = { Text(stringResource(R.string.dark_text)) }
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.background_img))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { pickImage.launch("image/*") }) { Text(stringResource(R.string.pick_from_gallery)) }
                    OutlinedButton(
                        onClick = { bgBitmap = null },
                        enabled = bgBitmap != null
                    ) { Text(stringResource(R.string.clear)) }
                }

                // 简易预览（可选）
                Spacer(Modifier.height(12.dp))
                Surface(
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 小尺寸预览，避免内存压力
                    ShareDashboardPoster(
                        data = ExportDashboardData(
                            monthText = stringResource(R.string.preview), metrics = emptyList(),
                            brand = "Deadliner", generatedAt = "—"
                        ),
                        backgroundBitmap = bgBitmap,
                        textTone = tone,
                        widthPx = 720 // 预览缩小
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tone, bgBitmap) }) { Text(stringResource(R.string.accept)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

fun decodeUriAsSoftwareBitmap(context: Context, uri: Uri, maxWidth: Int? = null): Bitmap? {
    return if (Build.VERSION.SDK_INT >= 28) {
        val src = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            // 关键：强制软件分配，避免 Config.HARDWARE
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
            // 可选：降采样，避免超大图 OOM
            maxWidth?.let { mw ->
                val w = info.size.width
                if (w > mw) decoder.setTargetSize(mw, (info.size.height * mw / w.toFloat()).toInt())
            }
        }
    } else {
        @Suppress("DEPRECATION")
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, opts)
        }
    }
}