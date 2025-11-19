package com.aritxonly.deadliner.ui.main.simplified

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DayOverview
import com.aritxonly.deadliner.model.HabitGoalType
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.HabitWithDailyStatus
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HabitCircleProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 800)
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress.value },
            modifier = Modifier.size(84.dp)
        )
        AnimatedProgressText(progress = progress)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HabitLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 800)
        )
    }

    Box(
        modifier = modifier
    ) {
        LinearProgressIndicator(
            progress = { animatedProgress.value },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun WeekRow(
    weekOverview: List<DayOverview>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        weekOverview.forEach { day ->
            val isSelected = day.date == selectedDate
            val label = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val dayNum = day.date.dayOfMonth

            Column(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clickable { onSelectDate(day.date) }
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayNum.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 小圆点表示完成度
                val ratio = day.completionRatio
                if (ratio > 0f) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun HabitRow(
    data: HabitWithDailyStatus,
    status: DDLStatus,
    isSelected: Boolean,
    canToggle: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    remainingText: String? = null
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    val progress = (data.doneCount.toFloat() / data.targetCount.coerceAtLeast(1)).coerceIn(0f, 1f)

    // 颜色按 DDLStatus 来
    val indicatorColor: Color
    val bgColor: Color
    when (status) {
        DDLStatus.UNDERGO -> {
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            bgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        }
        DDLStatus.NEAR -> {
            indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
            bgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        }
        DDLStatus.PASSED -> {
            indicatorColor = MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
            bgColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        }
        DDLStatus.COMPLETED -> {
            indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
            bgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        }
    }

    val bottomLine = when (data.habit.goalType) {
        HabitGoalType.PER_PERIOD -> {
            val progressText = "${data.doneCount}/${data.targetCount}"
            if (!remainingText.isNullOrBlank()) {
                "$remainingText · $progressText"
            } else {
                progressText
            }
        }

        HabitGoalType.TOTAL -> {
            val totalTargetText = data.habit.totalTarget?.toString() ?: "∞"
            val progressText = "${data.doneCount}/$totalTargetText"
            if (!remainingText.isNullOrBlank()) {
                // e.g. “还有 12 天 · 7/30”
                "$remainingText · $progressText"
            } else {
                progressText
            }
        }
    }

    val rightLabel = when (data.habit.goalType) {
        HabitGoalType.PER_PERIOD -> when (data.habit.period) {
            HabitPeriod.DAILY -> stringResource(R.string.frequency_daily)
            HabitPeriod.WEEKLY -> stringResource(R.string.frequency_weekly)
            HabitPeriod.MONTHLY -> stringResource(R.string.frequency_monthly)
        }

        HabitGoalType.TOTAL -> data.habit.totalTarget?.let {
            stringResource(R.string.habit_goal_total_with_target, it)
        } ?: stringResource(R.string.habit_goal_total_open)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClick = { if (canToggle) onToggle() },
                onLongClick = onLongPress
            ),
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .background(bgColor)
                .fillMaxWidth()
                .height(72.dp)
        ) {
            // 进度前景条
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    indicatorColor.copy(alpha = 0.4f),
                                    indicatorColor
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp, bottom = 12.dp, end = 12.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    enabled = canToggle,
                    checked = data.isCompleted,
                    onCheckedChange = { if (canToggle) onToggle() }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = data.habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = bottomLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Text(
                    text = rightLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 👇 多选叠加层：有点类似 DDLItemCard 那种选中效果
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            shape = shape
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitRowClassic(
    data: HabitWithDailyStatus,
    isSelected: Boolean,
    canToggle: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    remainingText: String? = null
) {
    val context = LocalContext.current

    // 这里引用现有 drawable 背景
    val backgroundRes = if (isSelected) {
        R.drawable.item_background_selected
    } else {
        R.drawable.item_background
    }

    val bottomLine = when (data.habit.goalType) {
        HabitGoalType.PER_PERIOD -> {
            val progressText = "${data.doneCount}/${data.targetCount}"
            if (!remainingText.isNullOrBlank()) {
                "$remainingText · $progressText"
            } else {
                progressText
            }
        }

        HabitGoalType.TOTAL -> {
            val totalTargetText = data.habit.totalTarget?.toString() ?: "∞"
            val progressText = "${data.doneCount}/$totalTargetText"
            if (!remainingText.isNullOrBlank()) {
                "$remainingText · $progressText"
            } else {
                progressText
            }
        }
    }

    val rightLabel = when (data.habit.goalType) {
        HabitGoalType.PER_PERIOD -> when (data.habit.period) {
            HabitPeriod.DAILY -> stringResource(R.string.frequency_daily)
            HabitPeriod.WEEKLY -> stringResource(R.string.frequency_weekly)
            HabitPeriod.MONTHLY -> stringResource(R.string.frequency_monthly)
        }

        HabitGoalType.TOTAL -> data.habit.totalTarget?.let {
            stringResource(R.string.habit_goal_total_with_target, it)
        } ?: stringResource(R.string.habit_goal_total_open)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Transparent)
                ),
                shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
            )
            .clip(RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)))
            .combinedClickable(
                onClick = { if (canToggle) onToggle() },
                onLongClick = onLongPress
            )
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
            )
            .drawBehind {
                val drawable = ContextCompat.getDrawable(context, backgroundRes)
                drawable?.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                drawable?.draw(drawContext.canvas.nativeCanvas)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Checkbox(
                enabled = canToggle,
                checked = data.isCompleted,
                onCheckedChange = { if (canToggle) onToggle() }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = data.habit.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = bottomLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Text(
                text = rightLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}