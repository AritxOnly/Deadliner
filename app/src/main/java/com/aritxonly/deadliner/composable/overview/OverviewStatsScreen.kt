package com.aritxonly.deadliner.composable.overview

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.viewinterop.AndroidView
import com.aritxonly.deadliner.model.AppColorScheme
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.composable.AnimatedItem
import com.aritxonly.deadliner.hashColor
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import java.time.Duration
import java.time.LocalDateTime
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun OverviewStatsScreen(
    activeStats: Map<String, Int>,
    historyStats: Map<String, Int>,
    completionTimeStats: List<Pair<String, Int>>,
    overdueItems: List<DDLItem>,
    modifier: Modifier,
    colorScheme: AppColorScheme
) {
    val overviewItems = listOf<@Composable () -> Unit>(
        { ActiveStatsCard(colorScheme, activeStats, overdueItems) },
        { CompletionTimeCard(colorScheme, completionTimeStats) },
        { HistoryStatsCard(colorScheme, historyStats) }
    )

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(overviewItems) { index, itemContent ->
            AnimatedItem(delayMillis = index * 100L) {
                itemContent()
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
fun HistoryStatsCard(
    colorScheme: AppColorScheme,
    historyStats: Map<String, Int>,
) {
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
                text = stringResource(R.string.task_status_summary),
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
                            color = Color(colorScheme.onSurface),
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            modifier = Modifier.basicMarquee()
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

@Composable
fun CompletionTimeCard(
    colorScheme: AppColorScheme,
    completionTimeStats: List<Pair<String, Int>>,
) {
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
                .padding(16.dp)
                .background(Color(colorScheme.surfaceContainer))
        ) {
            Text(
                text = stringResource(R.string.task_complete_time_summary),
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

@Composable
fun ActiveStatsCard(
    colorScheme: AppColorScheme,
    activeStats: Map<String, Int>,
    overdueItems: List<DDLItem>
) {
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
                text = stringResource(R.string.today_task_summary),
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
                            color = Color(colorScheme.onSurface),
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            modifier = Modifier.basicMarquee()
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
            val overdueCount = activeStats[stringResource(R.string.today_overdue)] ?: 0
            if (overdueCount > 0 && overdueItems.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.today_overdue),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,   // 错误色强调
                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                )

                // 用 Column 或 LazyColumn 渲染每个逾期条目
                Column {
                    overdueItems.forEach { item ->
                        DeadlineItemXmlRow(item)
                    }
                }
            }
        }
    }
}

@SuppressLint("SetTextI18n")
@Composable
fun DeadlineItemXmlRow(item: DDLItem, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        factory = { context ->
            // 1. Inflate XML
            val view = LayoutInflater.from(context)
                .inflate(R.layout.deadline_item, null, false)

            // 2. 找到子 View
            val titleTv = view.findViewById<TextView>(R.id.item_title)
            val progressTv = view.findViewById<TextView>(R.id.item_progress_text)
            val progressBar = view.findViewById<ProgressBar>(R.id.item_progress)

            view.setOnClickListener {
                val intent = DeadlineDetailActivity.newIntent(context, item)
                context.startActivity(intent)
            }

            // 3. 返回根 View
            view
        },
        update = { view ->
            // 4. 每次 Compose 重组时，更新数据
            val titleTv = view.findViewById<TextView>(R.id.item_title)
            val progressTv = view.findViewById<TextView>(R.id.item_progress_text)
            val progressBar = view.findViewById<ProgressBar>(R.id.item_progress)

            titleTv.text = item.name

            // 计算进度百分比
            val start = GlobalUtils.safeParseDateTime(item.startTime)
            val end = GlobalUtils.safeParseDateTime(item.endTime)
            val total = Duration.between(start, end).toMillis().coerceAtLeast(1L)
            val elapsed = Duration.between(start, LocalDateTime.now()).toMillis().coerceIn(0, total)
            val percent = (elapsed * 100 / total).toInt()

            val remaining = Duration.between(LocalDateTime.now(), end).toMinutes().toFloat() / 60

            progressBar.progress = if (GlobalUtils.progressDir) percent else (100 - percent)
            progressTv.text = "%.1f".format(remaining) + "h $percent%"
        }
    )
}