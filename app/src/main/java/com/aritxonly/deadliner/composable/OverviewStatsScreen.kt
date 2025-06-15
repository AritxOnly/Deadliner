package com.aritxonly.deadliner.composable

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.AppColorScheme
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.hashColor
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun OverviewStatsScreen(
    activeStats: Map<String, Int>,
    historyStats: Map<String, Int>,
    completionTimeStats: List<Pair<String, Int>>,
    modifier: Modifier,
    colorScheme: AppColorScheme
) {
    val overviewItems = listOf<@Composable () -> Unit>(
        { ActiveStatsCard(colorScheme, activeStats) },
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

@Composable
fun ActiveStatsCard(
    colorScheme: AppColorScheme,
    activeStats: Map<String, Int>
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