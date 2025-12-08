package com.aritxonly.deadliner.ui.main.simplified

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.GlobalUtils.refreshCount
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.HabitMetaData
import com.aritxonly.deadliner.model.updateNoteWithDate
import com.aritxonly.deadliner.ui.main.DDLItemCardSwipeable
import com.aritxonly.deadliner.ui.main.HabitItemCardSimplified
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max

@Composable
fun AnimatedItem(
    item: DDLItem,
    index: Int,
    content:  @Composable () -> Unit
) {
    var visible by rememberSaveable(item.id) { mutableStateOf(false) }

    LaunchedEffect(item.id) {
        delay(index * 70L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(380)) +
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(380)
                ),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        content()
    }
}

@Composable
fun TaskItem(
    item: DDLItem,
    activity: MainActivity,
    updateDDL: (DDLItem) -> Unit,
    celebrate: () -> Unit,
    onDelete: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongPressSelect: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val startTime = GlobalUtils.parseDateTime(item.startTime)
    val endTime = GlobalUtils.parseDateTime(item.endTime)
    val now = LocalDateTime.now()

    val remainingTimeText =
        if (!item.isCompleted)
            GlobalUtils.buildRemainingTime(
                context,
                startTime,
                endTime,
                true,
                now
            )
        else stringResource(R.string.completed)

    val progress = computeProgress(startTime, endTime, now)
    val status =
        DDLStatus.calculateStatus(startTime, endTime, now, item.isCompleted)

    DDLItemCardSwipeable(
        title = item.name,
        remainingTimeAlt = remainingTimeText,
        note = item.note,
        progress = progress,
        isStarred = item.isStared,
        status = status,
        onClick = {
            val intent = DeadlineDetailActivity.newIntent(context, item)
            activity.startActivity(intent)
        },
        onComplete = {
            GlobalUtils.triggerVibration(activity, 100)

            val realItem = DDLRepository().getDDLById(item.id)
                ?: return@DDLItemCardSwipeable
            val newItem = realItem.copy(
                isCompleted = !realItem.isCompleted,
                completeTime = if (!realItem.isCompleted) LocalDateTime.now()
                    .toString() else ""
            )

            updateDDL(newItem)

            if (newItem.isCompleted) {
                celebrate()
                Toast.makeText(
                    activity,
                    R.string.toast_finished,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    activity,
                    R.string.toast_definished,
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onDelete = {
            GlobalUtils.triggerVibration(activity, 200)
            onDelete()
        },
        selectionMode = selectionMode,
        selected = selected,
        onLongPressSelect = onLongPressSelect,
        onToggleSelect = onToggleSelect,
    )
}

@Composable
fun HabitItem(
    item: DDLItem,
    onRefresh: () -> Unit,
    updateDDL: (DDLItem) -> Unit,
    onCheckInFailed: () -> Unit = {},
    onCheckInSuccess: (DDLItem, HabitMetaData) -> Unit = { i, m -> },
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongPressSelect: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null
) {
    val now = LocalDateTime.now()
    val habitMeta = remember(item.note) { GlobalUtils.parseHabitMetaData(item.note) }

    LaunchedEffect(item.id, habitMeta.refreshDate) {
        refreshCount(item, habitMeta) {
            onRefresh()
        }
    }

    val startTime = GlobalUtils.safeParseDateTime(item.startTime)
    val endTime = GlobalUtils.safeParseDateTime(item.endTime)

    // —— 频率/总计描述 —— //
    val freqAndTotalText = when (habitMeta.frequencyType) {
        DeadlineFrequency.DAILY ->
            if (habitMeta.total == 0)
                stringResource(R.string.daily_frequency, habitMeta.frequency)
            else
                stringResource(
                    R.string.daily_frequency_with_total,
                    habitMeta.frequency,
                    habitMeta.total
                )

        DeadlineFrequency.WEEKLY ->
            if (habitMeta.total == 0)
                stringResource(R.string.weekly_frequency, habitMeta.frequency)
            else
                stringResource(
                    R.string.weekly_frequency_with_total,
                    habitMeta.frequency,
                    habitMeta.total
                )

        DeadlineFrequency.MONTHLY ->
            if (habitMeta.total == 0)
                stringResource(R.string.monthly_frequency, habitMeta.frequency)
            else
                stringResource(
                    R.string.monthly_frequency_with_total,
                    habitMeta.frequency,
                    habitMeta.total
                )

        DeadlineFrequency.TOTAL ->
            if (habitMeta.total == 0)
                stringResource(R.string.total_frequency_persistent)
            else
                stringResource(R.string.total_frequency_count, habitMeta.total)
    }

    val remainingText = if (endTime != GlobalUtils.timeNull) {
        val duration = Duration.between(now, endTime)
        val days = duration.toDays()
        if (days < 0)
            stringResource(R.string.ddl_overdue_short)
        else
            stringResource(R.string.remaining_days_arg, days)
    } else ""

    val (count, total) =
        if (habitMeta.total > 0) {
            item.habitTotalCount to habitMeta.total
        } else {
            val denom = max(1, habitMeta.frequency)
            item.habitCount to denom
        }

    val status = remember(startTime, endTime, now, item.isCompleted) {
        DDLStatus.calculateStatus(
            startTime = startTime,
            endTime = if (endTime == GlobalUtils.timeNull) null else endTime,
            now = now,
            isCompleted = item.isCompleted || // 已完成或累计达到总次数都算完成
                    (habitMeta.total != 0 && item.habitTotalCount >= habitMeta.total)
        )
    }

    val progress = computeProgress(startTime, endTime, now)

    HabitItemCardSimplified(
        title = item.name,
        habitCount = count,
        habitTotalCount = total,
        freqAndTotalText = freqAndTotalText,
        remainingText = remainingText,
        isStarred = item.isStared,
        status = status,
        progressTime = progress,
        onCheckIn = {
            val completedDates: Set<LocalDate> =
                habitMeta.completedDates.map { LocalDate.parse(it) }.toSet()

            val canCheckIn = (habitMeta.total != 0 && (
                    if (habitMeta.frequencyType != DeadlineFrequency.TOTAL) {
                        (item.habitCount < habitMeta.frequency) && (completedDates.size < habitMeta.total)
                    } else true
                    ) && (item.habitTotalCount < habitMeta.total)) || (habitMeta.total == 0)

            val alreadyChecked = when (habitMeta.frequencyType) {
                DeadlineFrequency.TOTAL -> false
                else -> habitMeta.frequency <= item.habitCount
            }
            val canPerformClick = canCheckIn && !alreadyChecked

            if (!canPerformClick) {
                onCheckInFailed()
                return@HabitItemCardSimplified
            }

            val today = LocalDate.now()
            val updatedNote = updateNoteWithDate(item, today)

            val updatedHabit = item.copy(
                note = updatedNote,
                habitCount = item.habitCount + 1,
                habitTotalCount = item.habitTotalCount + 1
            )

            onCheckInSuccess(item, habitMeta)

            updateDDL(updatedHabit)
        },
        selectionMode = selectionMode,
        selected = selected,
        onLongPressSelect = onLongPressSelect,
        onToggleSelect = onToggleSelect,
    )
}