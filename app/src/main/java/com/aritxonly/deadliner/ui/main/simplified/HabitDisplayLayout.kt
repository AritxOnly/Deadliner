package com.aritxonly.deadliner.ui.main.simplified

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.HabitViewModel
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DayOverview
import com.aritxonly.deadliner.model.HabitWithDailyStatus
import com.aritxonly.deadliner.ui.main.shared.mainListContainerClip
import java.time.LocalDate
import java.time.LocalDateTime

private data class HabitDisplayItem(
    val habit: HabitWithDailyStatus,
    val status: DDLStatus,
    val remainingText: String?,
)

@Composable
fun HabitScreen(
    habitViewModel: HabitViewModel,
    selectionMode: Boolean = false,
    isSelected: (Long) -> Boolean = { false },
    onItemLongPress: (Long) -> Unit = {},
    onItemClickInSelection: (Long) -> Unit = {},
    onToggleHabit: (Long) -> Unit = {}
) {
    val selectedDate by habitViewModel.selectedDate.collectAsState()
    val weekOverview by habitViewModel.weekOverview.collectAsState()
    val habits by habitViewModel.habitsForSelectedDate.collectAsState()
    val searchQuery by habitViewModel.searchQuery.collectAsState()

    HabitDisplayLayout(
        weekOverview, selectedDate, habits, habitViewModel,
        selectionMode = selectionMode,
        isSelected = isSelected,
        onItemLongPress = onItemLongPress,
        onItemClickInSelection = onItemClickInSelection,
        onToggleHabit = onToggleHabit,
        isClassic = true
    )
}

@Composable
fun HabitDisplayLayout(
    weekOverview: List<DayOverview>,
    selectedDate: LocalDate,
    habits: List<HabitWithDailyStatus>,
    habitViewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    isSelected: (Long) -> Boolean = { false },
    onItemLongPress: (Long) -> Unit = {},
    onItemClickInSelection: (Long) -> Unit = {},
    onToggleHabit: (Long) -> Unit = {},
    isClassic: Boolean = false,
    listState: LazyListState = LazyListState()
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        val todayOverview = weekOverview.firstOrNull { it.date == selectedDate }
        val ratio = todayOverview?.completionRatio ?: 0f

        val context = LocalContext.current
        val today = LocalDate.now()
        val canToggleOnThisDate = !selectedDate.isAfter(today)

        if (!isClassic) {
            HabitCircleProgress(
                progress = ratio,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
        } else {
            HabitLinearProgress(
                progress = ratio,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }

        // 本周日期横条
        WeekRow(
            weekOverview = weekOverview,
            selectedDate = selectedDate,
            onSelectDate = habitViewModel::onSelectDate
        )

        val displayHabits = remember(habits) {
            habits.mapNotNull { item ->
                val ddl = DDLRepository().getDDLById(item.habit.ddlId)
                if (ddl == null) {
                    Log.w(
                        "HabitDisplayLayout",
                        "Skipping orphan habit item: habitId=${item.habit.id}, ddlId=${item.habit.ddlId}"
                    )
                    return@mapNotNull null
                }

                val startTime = GlobalUtils.parseDateTime(ddl.startTime)
                val endTime = GlobalUtils.parseDateTime(ddl.endTime)
                val status = DDLStatus.calculateStatus(
                    startTime,
                    endTime,
                    isCompleted = ddl.isCompleted
                )
                val remainingText = endTime?.let {
                    GlobalUtils.buildRemainingTime(
                        context,
                        startTime,
                        endTime,
                        false,
                        LocalDateTime.now()
                    )
                }

                HabitDisplayItem(
                    habit = item,
                    status = status,
                    remainingText = remainingText,
                )
            }
        }

        // 习惯列表
        LazyColumn(
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .mainListContainerClip(),
            state = listState,
        ) {
            itemsIndexed(
                items = displayHabits,
                key = { _, it -> it.habit.habit.id }
            ) { index, item ->
                val selected = isSelected(item.habit.habit.ddlId)

                if (!isClassic) {
                    HabitRow(
                        data = item.habit,
                        status = item.status,
                        isSelected = selected,
                        canToggle = canToggleOnThisDate,
                        onToggle = {
                            if (selectionMode) {
                                onItemClickInSelection(item.habit.habit.ddlId)
                            } else {
                                onToggleHabit(item.habit.habit.id)
                            }
                        },
                        onLongPress = {
                            onItemLongPress(item.habit.habit.ddlId)
                        },
                        remainingText = item.remainingText
                    )
                } else {
                    HabitRowClassic(
                        data = item.habit,
                        isSelected = selected,
                        canToggle = canToggleOnThisDate,
                        onToggle = {
                            if (selectionMode) {
                                onItemClickInSelection(item.habit.habit.ddlId)
                            } else {
                                onToggleHabit(item.habit.habit.id)
                            }
                        },
                        onLongPress = {
                            onItemLongPress(item.habit.habit.ddlId)
                        },
                        remainingText = item.remainingText
                    )
                }
            }
        }
    }
}
