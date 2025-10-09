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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.ui.main.DDLItemCardSwipeable
import kotlinx.coroutines.delay
import java.time.LocalDateTime

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
        }
    )
}