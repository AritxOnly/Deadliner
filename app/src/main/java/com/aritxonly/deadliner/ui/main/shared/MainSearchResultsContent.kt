package com.aritxonly.deadliner.ui.main.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.data.HabitViewModel
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLState
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.main.DDLItemCardSimplified
import com.aritxonly.deadliner.ui.main.shared.mainListContainerClip
import com.aritxonly.deadliner.ui.main.simplified.AnimatedItem
import com.aritxonly.deadliner.ui.main.simplified.HabitRow
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun MainSearchResultsContent(
    searchResults: List<DDLItem>,
    selectedPage: DeadlineType,
    habitViewModel: HabitViewModel,
    activity: MainActivity,
    horizontalPadding: Dp = 16.dp,
    mixedResultTypes: Boolean = false,
) {
    val context = LocalContext.current
    val selectedDate by habitViewModel.selectedDate.collectAsState()
    val habitsForSelectedDate by habitViewModel.habitsForSelectedDate.collectAsState()
    val statusByDdlId = habitsForSelectedDate.associateBy { it.habit.ddlId }
    val canToggleOnThisDate = !selectedDate.isAfter(LocalDate.now())

    if (searchResults.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.search_no_result_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.search_no_result_suggestion_prefix))
                    append("\n")
                    appendExample("y2025", R.string.search_example_y)
                    append("\n")
                    appendExample("m10", R.string.search_example_m)
                    append("\n")
                    appendExample("d15", R.string.search_example_d)
                    append("\n")
                    appendExample("h20", R.string.search_example_h)
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(48.dp)
                    .alpha(0.6f)
            )
        }
    }

    if (mixedResultTypes) {
        LazyColumn(
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 96.dp,
                start = horizontalPadding,
                end = horizontalPadding
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .mainListContainerClip(),
        ) {
            itemsIndexed(
                items = searchResults,
                key = { _, it -> "${it.type}-${it.id}" }
            ) { index, item ->
                AnimatedItem(
                    item = item,
                    index = index
                ) {
                    if (item.type == DeadlineType.HABIT) {
                        HabitSearchItem(
                            item = item,
                            activity = activity,
                            canToggle = canToggleOnThisDate,
                            onToggle = {
                                statusByDdlId[item.id]?.let { status ->
                                    habitViewModel.onToggleHabit(status.habit.id)
                                }
                            },
                            statusByDdlId = statusByDdlId
                        )
                    } else {
                        TaskSearchItem(
                            item = item,
                            activity = activity,
                        )
                    }
                }
            }
        }
        return
    }

    when (selectedPage) {
        DeadlineType.TASK -> {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 96.dp,
                    start = horizontalPadding,
                    end = horizontalPadding
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .mainListContainerClip(),
            ) {
                itemsIndexed(
                    items = searchResults,
                    key = { _, it -> it.id }
                ) { index, item ->
                    AnimatedItem(
                        item = item,
                        index = index
                    ) {
                        TaskSearchItem(
                            item = item,
                            activity = activity,
                        )
                    }
                }
            }
        }

        DeadlineType.HABIT -> {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                verticalItemSpacing = 10.dp,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 16.dp,
                    bottom = 16.dp,
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = searchResults,
                    key = { _, it -> it.id }
                ) { index, item ->
                    AnimatedItem(
                        item = item,
                        index = index
                    ) {
                        HabitSearchItem(
                            item = item,
                            activity = activity,
                            canToggle = canToggleOnThisDate,
                            onToggle = {
                                statusByDdlId[item.id]?.let { status ->
                                    habitViewModel.onToggleHabit(status.habit.id)
                                }
                            },
                            statusByDdlId = statusByDdlId
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitSearchItem(
    item: DDLItem,
    activity: MainActivity,
    canToggle: Boolean,
    onToggle: () -> Unit,
    statusByDdlId: Map<Long, com.aritxonly.deadliner.model.HabitWithDailyStatus>,
) {
    val data = statusByDdlId[item.id] ?: return
    val context = LocalContext.current
    val st = GlobalUtils.parseDateTime(item.startTime)
    val et = GlobalUtils.parseDateTime(item.endTime)
    val status = DDLStatus.calculateStatus(st, et, isCompleted = item.isCompleted)
    val remainingText = et?.let {
        GlobalUtils.buildRemainingTime(
            context,
            st,
            et,
            false,
            LocalDateTime.now()
        )
    }

    HabitRow(
        data = data,
        status = status,
        isSelected = false,
        canToggle = canToggle,
        onToggle = onToggle,
        onLongPress = {
            val intent = DeadlineDetailActivity.newIntent(activity, item)
            activity.startActivity(intent)
        },
        remainingText = remainingText
    )
}

@Composable
private fun TaskSearchItem(
    item: DDLItem,
    activity: MainActivity,
) {
    val startTime = GlobalUtils.parseDateTime(item.startTime)
    val endTime = GlobalUtils.parseDateTime(item.endTime)
    val now = LocalDateTime.now()

    val remainingTimeText =
        if (item.state == DDLState.ABANDONED)
            stringResource(R.string.abandoned)
        else if (!item.state.isCompletedFamily())
            GlobalUtils.buildRemainingTime(
                activity,
                startTime,
                endTime,
                true,
                now
            )
        else stringResource(R.string.completed)

    val progress = computeProgress(startTime, endTime, now)
    val status =
        if (item.state.isCompletedFamily() || item.state.isAbandonedFamily()) {
            DDLStatus.COMPLETED
        } else {
            DDLStatus.calculateStatus(
                startTime,
                endTime,
                now,
                false
            )
        }

    DDLItemCardSimplified(
        title = item.name,
        remainingTimeAlt = remainingTimeText,
        note = item.note,
        progress = progress,
        isStarred = item.isStared,
        useDisabledCompletedStyle = item.state.isAbandonedFamily(),
        status = status,
        onClick = {
            val intent = DeadlineDetailActivity.newIntent(activity, item)
            activity.startActivity(intent)
        }
    )
}
