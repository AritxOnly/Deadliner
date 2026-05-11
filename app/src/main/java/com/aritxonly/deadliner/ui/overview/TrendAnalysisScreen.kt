package com.aritxonly.deadliner.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.AnimatedItem
import com.aritxonly.deadliner.ui.main.shared.mainListContainerClip

@Composable
fun TrendAnalysisScreen(
    snapshot: OverviewSnapshot,
    modifier: Modifier = Modifier,
) {
    val trendItems = listOf<@Composable () -> Unit>(
        { ContributionHeatmapCard(snapshot.contributionStats) },
        { DailyCompletedCard(snapshot.dailyStats) },
        { MonthlyTrendCard(snapshot.monthlyStats) },
        { PrevWeeksCard(snapshot.weeklyStats) },
    )

    LazyColumn(
        modifier = modifier.mainListContainerClip(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(trendItems) { index, itemComposable ->
            AnimatedItem(delayMillis = index * 100L) {
                itemComposable()
            }
        }

        item {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.overview_bottom_safe_area)))
        }
    }
}

@Composable
private fun ContributionHeatmapCard(
    contributionStats: List<ContributionDay>,
) {
    OverviewSurfaceCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.contribution_heatmap),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.contribution_heatmap_days, contributionStats.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ResponsiveContributionHeatmapGrid(contributionStats = contributionStats)

            Spacer(modifier = Modifier.height(12.dp))

            HeatLegend(
                lowLabel = stringResource(R.string.heatmap_less),
                highLabel = stringResource(R.string.heatmap_more),
                levels = List(5) { level -> colorForContribution(level) },
            )
        }
    }
}

@Composable
private fun ResponsiveContributionHeatmapGrid(
    contributionStats: List<ContributionDay>,
) {
    val columns = contributionStats.chunked(7)
    val horizontalSpacing = 3.dp
    val verticalSpacing = 3.dp
    val cellMaxSize = dimensionResource(R.dimen.overview_contribution_heatmap_cell_max_size)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columnCount = columns.size.coerceAtLeast(1)
        val cellSize = ((maxWidth - horizontalSpacing * (columnCount - 1)) / columnCount)
            .coerceAtMost(cellMaxSize)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            columns.forEachIndexed { columnIndex, week ->
                Column(verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
                    repeat(7) { rowIndex ->
                        val day = week.getOrNull(rowIndex)
                        if (day != null) {
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(dimensionResource(R.dimen.overview_corner_radius_xs)))
                                    .background(colorForContribution(day.count))
                            )
                        } else {
                            Spacer(modifier = Modifier.size(cellSize))
                        }
                    }
                }
                if (columnIndex < columns.lastIndex) {
                    Spacer(modifier = Modifier.width(horizontalSpacing))
                }
            }
        }
    }
}

@Composable
private fun DailyCompletedCard(
    dailyStats: List<DailyStat>,
) {
    OverviewSurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Text(
                text = stringResource(R.string.weekly_completed),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            DailyBarChart(
                data = dailyStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                barColor = colorResource(R.color.chart_green),
                overdueColor = colorResource(R.color.chart_red)
            )
        }
    }
}

@Composable
private fun MonthlyTrendCard(
    monthlyStats: List<MonthlyStat>,
) {
    OverviewSurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Text(
                text = stringResource(R.string.monthly_trend),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            MonthlyTrendChart(
                data = monthlyStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                totalColor = colorResource(R.color.chart_blue),
                completedColor = colorResource(R.color.chart_green),
                overdueColor = colorResource(R.color.chart_orange)
            )
        }
    }
}

@Composable
private fun PrevWeeksCard(
    weeklyStats: List<WeeklyStat>,
) {
    OverviewSurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Text(
                text = stringResource(R.string.prev4weeks),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            WeeklyBarChart(
                data = weeklyStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                barColor = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun OverviewSurfaceCard(
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
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
        ) {
            content()
        }
    }
}

@Composable
private fun HeatLegend(
    lowLabel: String,
    highLabel: String,
    levels: List<Color>,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = lowLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.padding(horizontal = 2.dp))
        levels.forEach { color ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.overview_corner_radius_xs)))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.padding(horizontal = 2.dp))
        Text(
            text = highLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun colorForContribution(count: Int): Color {
    return when {
        count <= 0 -> MaterialTheme.colorScheme.surfaceVariant
        count < 2 -> colorResource(R.color.chart_green).copy(alpha = 0.30f)
        count < 4 -> colorResource(R.color.chart_green).copy(alpha = 0.50f)
        count < 6 -> colorResource(R.color.chart_green).copy(alpha = 0.72f)
        else -> colorResource(R.color.chart_green)
    }
}
