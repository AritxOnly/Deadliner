package com.aritxonly.deadliner.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.model.AppColorScheme
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.AnimatedItem
import com.aritxonly.deadliner.hashColor
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.OverviewUtils
import com.aritxonly.deadliner.model.DDLItem
import java.time.LocalDate

@Composable
fun TrendAnalysisScreen(
    items: List<DDLItem>,
    modifier: Modifier = Modifier,
    colorScheme: AppColorScheme
) {
    val context = LocalContext.current

    key(GlobalUtils.OverviewSettings.monthlyCount, GlobalUtils.OverviewSettings.showOverdueInDaily) {

        val dailyCompleted  = OverviewUtils.computeDailyCompletedCounts(items)
        val dailyOverdue    = OverviewUtils.computeDailyOverdueCounts(items)
        val monthlyStat     = OverviewUtils.computeMonthlyTaskStats(items, months = GlobalUtils.OverviewSettings.monthlyCount)
        val weeklyCompleted = OverviewUtils.computeWeeklyCompletedCounts(context, items)

        val trendItems = listOf<@Composable () -> Unit>(
            { DailyCompletedCard(colorScheme, dailyCompleted) },
            { MonthlyTrendCard(colorScheme, monthlyStat) },
            { PrevWeeksCard(colorScheme, weeklyCompleted) }
        )

        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(trendItems) { index, itemComposable ->
                AnimatedItem(delayMillis = index * 100L) {
                    itemComposable()
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PrevWeeksCard(
    colorScheme: AppColorScheme,
    weeklyCompleted: List<Pair<String, Int>>,
) {
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
                text = stringResource(R.string.prev4weeks),
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

@Composable
private fun MonthlyTrendCard(
    colorScheme: AppColorScheme,
    monthlyStat: List<OverviewUtils.MonthlyStat>,
) {
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
                text = stringResource(R.string.monthly_trend),
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

@Composable
private fun DailyCompletedCard(
    colorScheme: AppColorScheme,
    dailyCompleted: List<Triple<LocalDate, Int, Int>>,
) {
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
                text = stringResource(R.string.weekly_completed),
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
                overdueColor = hashColor(stringResource(R.string.overdue))
            )
        }
    }
}