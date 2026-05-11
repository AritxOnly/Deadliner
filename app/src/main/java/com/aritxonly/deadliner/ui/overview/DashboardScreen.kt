package com.aritxonly.deadliner.ui.overview

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ai.AIUtils
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.AnimatedItem
import com.aritxonly.deadliner.ui.TintedGradientImage
import com.aritxonly.deadliner.ui.base.Button
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.poster.ExportDashboardData
import com.aritxonly.deadliner.ui.poster.ShareDashboardPoster
import com.aritxonly.deadliner.ui.poster.renderPosterToCacheUri
import com.aritxonly.deadliner.ui.poster.saveImageToGallery
import com.aritxonly.deadliner.ui.poster.shareImage
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val overviewAnalysisGson = Gson()

@Composable
fun DashboardScreen(
    snapshot: OverviewSnapshot,
    activity: Activity,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var analysis by remember(snapshot.lastMonthKey) {
        mutableStateOf(loadCachedMonthlyAnalysis(snapshot.lastMonthKey))
    }
    var isAnalyzing by remember(snapshot.lastMonthKey) {
        mutableStateOf(analysis == null)
    }

    LaunchedEffect(snapshot.lastMonthKey, snapshot.metricsSummary, snapshot.completedTaskNames) {
        val cached = loadCachedMonthlyAnalysis(snapshot.lastMonthKey)
        if (cached != null) {
            analysis = cached
            isAnalyzing = false
            return@LaunchedEffect
        }

        isAnalyzing = true
        val generated = runCatching {
            AIUtils.generateMonthlyAnalysis(
                monthKey = snapshot.lastMonthKey,
                monthName = snapshot.lastMonthName,
                metricsSummary = snapshot.metricsSummary,
                completedTaskNames = snapshot.completedTaskNames,
            )
        }.getOrNull()

        if (generated != null && generated.summary.isNotBlank()) {
            analysis = generated
            GlobalUtils.OverviewSettings.monthlyAnalysisJson =
                overviewAnalysisGson.toJson(generated)
            GlobalUtils.OverviewSettings.lastAnalyzedMonth = snapshot.lastMonthKey
        }
        isAnalyzing = false
    }

    var exporting by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var chosenTone by remember { mutableStateOf(TextTone.Light) }
    var chosenBg by remember { mutableStateOf<Bitmap?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp)
    ) {
        item {
            DashboardHeaderSection(monthName = snapshot.lastMonthName)
        }
        item {
            when {
                isAnalyzing -> LoadingInsightCard()
                analysis != null -> AIAnalysisCard(analysis = analysis!!)
                else -> AIUnavailableCard()
            }
        }
        item {
            DashboardMetricsSection(metrics = snapshot.metrics)
        }
        if (snapshot.lastMonthDailyStats.isNotEmpty()) {
            item {
                LastMonthActivityMapCard(
                    monthName = snapshot.lastMonthName,
                    dailyStats = snapshot.lastMonthDailyStats,
                )
            }
        }
        item {
            ShareButtonSection(
                onShare = {
                    if (!exporting) {
                        showExportDialog = true
                    }
                }
            )
        }
        item {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.overview_bottom_safe_area)))
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
                        monthText = context.getString(R.string.summary_with_text, snapshot.lastMonthName),
                        metrics = snapshot.metrics,
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

private fun loadCachedMonthlyAnalysis(monthKey: String): MonthlyAnalysisResult? {
    val json = GlobalUtils.OverviewSettings.monthlyAnalysisJson ?: return null
    val analyzedMonth = GlobalUtils.OverviewSettings.lastAnalyzedMonth
    val cached = runCatching {
        overviewAnalysisGson.fromJson(json, MonthlyAnalysisResult::class.java)
    }.getOrNull() ?: return null
    return cached.takeIf { it.month == monthKey || analyzedMonth == monthKey }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DashboardHeaderSection(
    monthName: String,
) {
    AnimatedItem(delayMillis = 0) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            DashboardHeader(monthName = monthName)
        }
    }
}

@Composable
private fun DashboardMetricsSection(
    metrics: List<Metric>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        metrics.chunked(2).forEachIndexed { rowIndex, rowMetrics ->
            AnimatedItem(delayMillis = rowIndex * 100L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowMetrics.forEach { metric ->
                        SummaryCard(
                            metric = metric,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowMetrics.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShareButtonSection(
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onShare,
            colors = ButtonDefaults.textButtonColors().copy(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                iconResource(R.drawable.ic_share),
                contentDescription = stringResource(R.string.share),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.share))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DashboardHeader(
    monthName: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp)
            .clipToBounds()
            .clip(RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)))
    ) {
        TintedGradientImage(
            R.drawable.dashboard_background,
            tintColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.matchParentSize(),
            contentDescription = stringResource(R.string.background)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                text = monthName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.84f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.last_month_summary),
                style = MaterialTheme.typography.headlineLargeEmphasized,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SummaryCard(
    metric: Metric,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.item_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                color = MaterialTheme.colorScheme.onSurface
            )
            metric.change?.takeIf { it.isNotEmpty() }?.let { change ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    metric.isDown?.let { isDown ->
                        Icon(
                            imageVector = if (isDown) {
                                ImageVector.vectorResource(R.drawable.ic_arrow_down)
                            } else {
                                ImageVector.vectorResource(R.drawable.ic_arrow_up)
                            },
                            contentDescription = null,
                            tint = summaryTrendColor(metric.label, isDown),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = change,
                        style = MaterialTheme.typography.bodyLarge,
                        color = metric.isDown?.let { summaryTrendColor(metric.label, it) }
                            ?: colorResource(R.color.chart_blue)
                    )
                }
            }
        }
    }
}

@Composable
private fun summaryTrendColor(label: String, isDown: Boolean): Color {
    val isOverdueMetric = label == stringResource(R.string.metric_last_month_overdue)
    return when {
        isOverdueMetric && isDown -> colorResource(R.color.chart_green)
        isOverdueMetric -> colorResource(R.color.chart_red)
        isDown -> colorResource(R.color.chart_red)
        else -> colorResource(R.color.chart_green)
    }
}

@Composable
private fun LoadingInsightCard() {
    InsightCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(
                text = stringResource(R.string.monthly_ai_analyzing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AIUnavailableCard() {
    InsightCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.monthly_ai_insight_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.monthly_ai_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AIAnalysisCard(
    analysis: MonthlyAnalysisResult,
) {
    InsightCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    iconResource(R.drawable.ic_ai),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.monthly_ai_insight_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = analysis.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (analysis.keywords.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    analysis.keywords.forEach { keyword ->
                        Surface(
                            shape = RoundedCornerShape(dimensionResource(R.dimen.overview_corner_radius_pill)),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = keyword,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightCard(
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun LastMonthActivityMapCard(
    monthName: String,
    dailyStats: List<DailyStat>,
) {
    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.last_month_activity_distribution, monthName),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.last_month_activity_frequency),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ResponsiveActivityHeatmapGrid(dailyStats = dailyStats)

            Spacer(modifier = Modifier.height(12.dp))

            HeatLegend(
                lowLabel = stringResource(R.string.heatmap_low_frequency),
                highLabel = stringResource(R.string.heatmap_high_frequency),
                levels = List(5) { level -> colorForLastMonthActivity(level * 2) },
            )
        }
    }
}

@Composable
private fun ResponsiveActivityHeatmapGrid(
    dailyStats: List<DailyStat>,
) {
    val horizontalSpacing = 6.dp
    val verticalSpacing = 6.dp
    val cellMaxSize = dimensionResource(R.dimen.overview_heatmap_cell_max_size)
    val rows = dailyStats.chunked(7)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = ((maxWidth - horizontalSpacing * 6) / 7).coerceAtMost(cellMaxSize)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(7) { index ->
                        val stat = row.getOrNull(index)
                        if (stat != null) {
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(dimensionResource(R.dimen.overview_corner_radius_sm)))
                                    .background(colorForLastMonthActivity(stat.completedCount)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stat.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (stat.completedCount > 0) {
                                        colorResource(R.color.overview_heatmap_text_active)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(cellSize))
                        }

                        if (index < 6) {
                            Spacer(modifier = Modifier.width(horizontalSpacing))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun colorForLastMonthActivity(count: Int): Color {
    return when {
        count <= 0 -> MaterialTheme.colorScheme.surfaceVariant
        count < 2 -> colorResource(R.color.chart_blue).copy(alpha = 0.30f)
        count < 4 -> colorResource(R.color.chart_blue).copy(alpha = 0.50f)
        count < 6 -> colorResource(R.color.chart_blue).copy(alpha = 0.72f)
        else -> colorResource(R.color.chart_blue)
    }
}

@Composable
private fun HeatLegend(
    lowLabel: String,
    highLabel: String,
    levels: List<Color>,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = lowLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(6.dp))
        levels.forEach { color ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.overview_corner_radius_xs)))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = highLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
                    Button(onClick = { pickImage.launch("image/*") }) {
                        Text(stringResource(R.string.pick_from_gallery))
                    }
                    OutlinedButton(
                        onClick = { bgBitmap = null },
                        enabled = bgBitmap != null
                    ) { Text(stringResource(R.string.clear)) }
                }

                Spacer(Modifier.height(12.dp))
                Surface(
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.overview_corner_radius_md)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ShareDashboardPoster(
                        data = ExportDashboardData(
                            monthText = stringResource(R.string.preview),
                            metrics = emptyList(),
                            brand = "Deadliner",
                            generatedAt = "—"
                        ),
                        backgroundBitmap = bgBitmap,
                        textTone = tone,
                        widthPx = 720
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
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
            maxWidth?.let { mw ->
                val width = info.size.width
                if (width > mw) {
                    decoder.setTargetSize(mw, (info.size.height * mw / width.toFloat()).toInt())
                }
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
