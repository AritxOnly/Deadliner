package com.aritxonly.deadliner.ui.detail

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.calendar.CalendarHelper
import com.aritxonly.deadliner.localutils.DeadlinerURLScheme
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLState
import com.aritxonly.deadliner.model.SubTask
import com.aritxonly.deadliner.model.TaskStateAction
import com.aritxonly.deadliner.ui.base.RadioButton
import com.aritxonly.deadliner.ui.base.Scaffold
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import com.aritxonly.deadliner.ui.iconResource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class DetailAction(
    val iconRes: Int,
    val labelRes: Int
) {
    MARK_COMPLETE(R.drawable.ic_done, R.string.mark_complete),
    GIVE_UP(R.drawable.ic_flag, R.string.give_up_task),
    RESTORE_ACTIVE(R.drawable.ic_back, R.string.restore_active),
    ARCHIVE(R.drawable.ic_archiving, R.string.archive),
    UNARCHIVE(R.drawable.ic_back, R.string.unarchive),
    DELETE(R.drawable.ic_delete, R.string.delete),
    SAVE_TO_CALENDAR(R.drawable.ic_event, R.string.save_and_add_to_calendar)
}

private fun availableActions(state: DDLState): List<DetailAction> {
    val head = when (state) {
        DDLState.ACTIVE -> listOf(DetailAction.MARK_COMPLETE, DetailAction.GIVE_UP)
        DDLState.COMPLETED,
        DDLState.ABANDONED -> listOf(DetailAction.RESTORE_ACTIVE, DetailAction.ARCHIVE)
        DDLState.ARCHIVED,
        DDLState.ABANDONED_ARCHIVED -> listOf(DetailAction.UNARCHIVE)
    }

    return head + listOf(DetailAction.DELETE, DetailAction.SAVE_TO_CALENDAR)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeadlineDetailScreen(
    applicationContext: Context,
    activityContext: Context,
    deadline: DDLItem,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onPersistDeadline: (DDLItem) -> Unit,
    onToggleStar: (Boolean) -> Unit,
    onApplyTaskAction: (TaskStateAction, Boolean) -> DDLItem,
    onDelete: (Long) -> Unit,
    finishActivity: () -> Unit
) {
    var isStarred by remember(deadline.id) { mutableStateOf(deadline.isStared) }
    var showPlanComposer by rememberSaveable(deadline.id) { mutableStateOf(false) }
    var openComposerSignal by rememberSaveable(deadline.id) { mutableIntStateOf(0) }
    var draftSubTask by rememberSaveable(deadline.id) { mutableStateOf("") }
    var editingSubTaskId by rememberSaveable(deadline.id) { mutableStateOf<String?>(null) }
    var editingSubTaskDraft by rememberSaveable(deadline.id) { mutableStateOf("") }
    var isMutatingPlan by remember(deadline.id) { mutableStateOf(false) }
    val composerFocusRequester = remember { FocusRequester() }

    LaunchedEffect(deadline.isStared) {
        isStarred = deadline.isStared
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(showPlanComposer, openComposerSignal) {
        if (!showPlanComposer) return@LaunchedEffect
        scrollState.animateScrollTo(scrollState.maxValue)
        delay(80)
        composerFocusRequester.requestFocus()
    }

    fun mutateSubTasks(transform: (List<SubTask>) -> List<SubTask>) {
        if (isMutatingPlan) return
        isMutatingPlan = true
        try {
            val updated = deadline.copy(
                subTasks = transform(deadline.subTasks),
                timeStamp = LocalDateTime.now().toString()
            )
            onPersistDeadline(updated)
        } finally {
            isMutatingPlan = false
        }
    }

    fun submitPlanComposer() {
        val content = draftSubTask.trim()
        if (content.isEmpty()) {
            draftSubTask = ""
            return
        }

        mutateSubTasks { current ->
            val nextOrder = (current.maxOfOrNull { it.sortOrder } ?: -1) + 1
            current + SubTask(
                id = UUID.randomUUID().toString(),
                content = content,
                isCompleted = 0,
                sortOrder = nextOrder,
                createdAt = LocalDateTime.now().toString(),
                updatedAt = LocalDateTime.now().toString()
            )
        }

        draftSubTask = ""
        showPlanComposer = false
    }

    fun executeAction(action: DetailAction) {
        when (action) {
            DetailAction.MARK_COMPLETE -> {
                onApplyTaskAction(TaskStateAction.MARK_COMPLETE, true)
                Toast.makeText(activityContext, R.string.toast_finished, Toast.LENGTH_SHORT).show()
                finishActivity()
            }

            DetailAction.GIVE_UP -> {
                MaterialAlertDialogBuilder(activityContext)
                    .setTitle(R.string.confirm_give_up_title)
                    .setMessage(R.string.confirm_give_up_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.accept) { _, _ ->
                        onApplyTaskAction(TaskStateAction.MARK_GIVE_UP, true)
                        Toast.makeText(activityContext, R.string.toast_give_up, Toast.LENGTH_SHORT)
                            .show()
                        finishActivity()
                    }
                    .show()
            }

            DetailAction.RESTORE_ACTIVE -> {
                onApplyTaskAction(TaskStateAction.RESTORE_ACTIVE, true)
                Toast.makeText(activityContext, R.string.toast_restored_active, Toast.LENGTH_SHORT).show()
                finishActivity()
            }

            DetailAction.ARCHIVE -> {
                onApplyTaskAction(TaskStateAction.MARK_ARCHIVE, true)
                Toast.makeText(
                    activityContext,
                    activityContext.getString(R.string.toast_archived, 1),
                    Toast.LENGTH_SHORT
                ).show()
                finishActivity()
            }

            DetailAction.UNARCHIVE -> {
                onApplyTaskAction(TaskStateAction.UNARCHIVE, true)
                Toast.makeText(activityContext, R.string.toast_unarchived, Toast.LENGTH_SHORT).show()
                finishActivity()
            }

            DetailAction.DELETE -> {
                MaterialAlertDialogBuilder(activityContext)
                    .setTitle(R.string.alert_delete_title)
                    .setMessage(R.string.alert_delete_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.accept) { _, _ ->
                        onDelete(deadline.id)
                        DeadlineAlarmScheduler.cancelAlarm(applicationContext, deadline.id)
                        Toast.makeText(activityContext, R.string.toast_deletion, Toast.LENGTH_SHORT)
                            .show()
                        finishActivity()
                    }
                    .show()
            }

            DetailAction.SAVE_TO_CALENDAR -> {
                val calendarHelper = CalendarHelper(applicationContext)
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val eventId = calendarHelper.insertEvent(deadline)
                        onPersistDeadline(deadline.copy(calendarEventId = eventId))
                        Toast.makeText(
                            activityContext,
                            R.string.add_calendar_success,
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            activityContext,
                            activityContext.getString(R.string.add_calendar_failed, e.toString()),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    val sortedSubTasks = remember(deadline.subTasks) {
        deadline.subTasks.sortedWith(
            compareBy<SubTask> { it.isCompleted }
                .thenBy { it.sortOrder }
                .thenBy { it.id }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = deadline.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = iconResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.close),
                            modifier = expressiveTypeModifier
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = iconResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.edit),
                            modifier = expressiveTypeModifier
                        )
                    }
                    IconButton(onClick = {
                        val url = DeadlinerURLScheme.encodeWithPassphrase(
                            deadline,
                            "deadliner-2025".toCharArray()
                        )
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        activityContext.startActivity(
                            Intent.createChooser(sendIntent, activityContext.getString(R.string.share))
                        )
                    }) {
                        Icon(
                            imageVector = iconResource(R.drawable.ic_share_alt),
                            contentDescription = stringResource(R.string.share),
                            modifier = expressiveTypeModifier
                        )
                    }
                    IconButton(onClick = {
                        isStarred = !isStarred
                        onToggleStar(isStarred)
                    }) {
                        Icon(
                            painter = painterResource(
                                if (isStarred) R.drawable.ic_star_filled else R.drawable.ic_star
                            ),
                            contentDescription = stringResource(R.string.star),
                            tint = if (isStarred) Color(0xFFFFE819) else MaterialTheme.colorScheme.onSurface,
                            modifier = expressiveTypeModifier
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp)
            )
        },
        bottomBar = {
            val actions = availableActions(deadline.state)
            val split = actions.size / 2
            FlexibleBottomAppBar(
                horizontalArrangement = Arrangement.SpaceEvenly,
                contentPadding = PaddingValues(horizontal = 0.dp),
                content = {
                    actions.take(split).forEach { action ->
                        IconButton(onClick = { executeAction(action) }) {
                            Icon(
                                imageVector = iconResource(action.iconRes),
                                contentDescription = stringResource(action.labelRes)
                            )
                        }
                    }
                    actions.drop(split).forEach { action ->
                        IconButton(onClick = { executeAction(action) }) {
                            Icon(
                                imageVector = iconResource(action.iconRes),
                                contentDescription = stringResource(action.labelRes)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier
                    .semantics {
                        contentDescription = activityContext.getString(R.string.detail_plan_add)
                    },
                onClick = {
                    showPlanComposer = true
                    openComposerSignal++
                },
                icon = {
                    Icon(
                        imageVector = iconResource(R.drawable.ic_add),
                        contentDescription = null
                    )
                },
                text = { Text(text = stringResource(R.string.detail_plan_add)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressCard(deadline = deadline)
            TaskMetaCard(deadline = deadline)
            NoteCard(note = deadline.note)
            PlanCard(
                subTasks = sortedSubTasks,
                showComposer = showPlanComposer,
                composerText = draftSubTask,
                isMutating = isMutatingPlan,
                composerFocusRequester = composerFocusRequester,
                onComposerTextChange = { draftSubTask = it },
                onComposerConfirm = {
                    submitPlanComposer()
                },
                onComposerCancel = {
                    draftSubTask = ""
                    showPlanComposer = false
                },
                onToggleSubTask = { subTask ->
                    mutateSubTasks { current ->
                        current.map {
                            if (it.id == subTask.id) {
                                it.copy(
                                    isCompleted = if (it.isCompleted == 0) 1 else 0,
                                    updatedAt = LocalDateTime.now().toString()
                                )
                            } else it
                        }
                    }
                },
                onDeleteSubTask = { subTask ->
                    mutateSubTasks { current -> current.filterNot { it.id == subTask.id } }
                },
                editingSubTaskId = editingSubTaskId,
                editingSubTaskDraft = editingSubTaskDraft,
                onStartEditSubTask = { subTask ->
                    editingSubTaskId = subTask.id
                    editingSubTaskDraft = subTask.content
                },
                onEditingDraftChange = { editingSubTaskDraft = it },
                onSubmitEditSubTask = { subTask ->
                    val updatedContent = editingSubTaskDraft.trim()
                    if (updatedContent.isNotEmpty() && updatedContent != subTask.content) {
                        mutateSubTasks { current ->
                            current.map {
                                if (it.id == subTask.id) {
                                    it.copy(
                                        content = updatedContent,
                                        updatedAt = LocalDateTime.now().toString()
                                    )
                                } else it
                            }
                        }
                    }
                    editingSubTaskId = null
                    editingSubTaskDraft = ""
                },
                onCancelEditSubTask = {
                    editingSubTaskId = null
                    editingSubTaskDraft = ""
                }
            )
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

@Composable
private fun ProgressCard(deadline: DDLItem) {
    val startTime = GlobalUtils.safeParseDateTime(deadline.startTime)
    val endTime = GlobalUtils.safeParseDateTime(deadline.endTime)
    val now = LocalDateTime.now()

    val total = Duration.between(startTime, endTime).toMillis().coerceAtLeast(1L)
    val elapsed = Duration.between(startTime, now).toMillis().coerceIn(0L, total)
    val targetProgress = (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 650),
        label = "detail_progress"
    )

    val remainingDuration = Duration.between(now, endTime)
    val remainingText = if (remainingDuration.isNegative) {
        stringResource(R.string.overdue)
    } else {
        formatDuration(LocalContext.current, remainingDuration)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_progress_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f)
                )
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun TaskMetaCard(deadline: DDLItem) {
    val startTime = GlobalUtils.safeParseDateTime(deadline.startTime)
    val endTime = GlobalUtils.safeParseDateTime(deadline.endTime)
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_task_info_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.label_start_time, startTime.format(formatter)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.label_end_time, endTime.format(formatter)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StateBadge(deadline.state)
            }
        }
    }
}

@Composable
private fun StateBadge(state: DDLState) {
    val (label, bg, fg) = when (state) {
        DDLState.ACTIVE -> Triple(
            stringResource(R.string.state_active),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )

        DDLState.COMPLETED -> Triple(
            stringResource(R.string.state_completed),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )

        DDLState.ARCHIVED -> Triple(
            stringResource(R.string.state_archived),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )

        DDLState.ABANDONED -> Triple(
            stringResource(R.string.state_abandoned),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )

        DDLState.ABANDONED_ARCHIVED -> Triple(
            stringResource(R.string.state_abandoned_archived),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun NoteCard(note: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_note_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            SelectionContainer {
                Text(
                    text = note.ifBlank { stringResource(R.string.detail_note_empty) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    subTasks: List<SubTask>,
    showComposer: Boolean,
    composerText: String,
    isMutating: Boolean,
    composerFocusRequester: FocusRequester,
    onComposerTextChange: (String) -> Unit,
    onComposerConfirm: () -> Unit,
    onComposerCancel: () -> Unit,
    onToggleSubTask: (SubTask) -> Unit,
    onDeleteSubTask: (SubTask) -> Unit,
    editingSubTaskId: String?,
    editingSubTaskDraft: String,
    onStartEditSubTask: (SubTask) -> Unit,
    onEditingDraftChange: (String) -> Unit,
    onSubmitEditSubTask: (SubTask) -> Unit,
    onCancelEditSubTask: () -> Unit
) {
    val completedCount = subTasks.count { it.isCompleted == 1 }
    val rowSpacing = 3.dp

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.detail_plan_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.detail_plan_progress,
                        completedCount,
                        subTasks.size
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showComposer) {
                PlanComposerRow(
                    value = composerText,
                    focusRequester = composerFocusRequester,
                    enabled = !isMutating,
                    onValueChange = onComposerTextChange,
                    onConfirm = onComposerConfirm,
                    onCancel = onComposerCancel
                )
                if (subTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(rowSpacing))
                }
            }

            if (subTasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.detail_plan_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                subTasks.forEachIndexed { index, subTask ->
                    SubTaskRow(
                        item = subTask,
                        enabled = !isMutating,
                        isEditing = editingSubTaskId == subTask.id,
                        editingText = editingSubTaskDraft,
                        onEditingTextChange = onEditingDraftChange,
                        onToggle = { onToggleSubTask(subTask) },
                        onDelete = { onDeleteSubTask(subTask) },
                        onLongPressEdit = { onStartEditSubTask(subTask) },
                        onSubmitEdit = { onSubmitEditSubTask(subTask) },
                        onCancelEdit = onCancelEditSubTask
                    )
                    if (index != subTasks.lastIndex) {
                        Spacer(modifier = Modifier.height(rowSpacing))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanComposerRow(
    value: String,
    focusRequester: FocusRequester,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = false,
                onClick = null,
                enabled = false,
                modifier = Modifier.alpha(0.55f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.merge(
                    TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = stringResource(R.string.detail_plan_add_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
            FilledIconButton(
                onClick = onConfirm,
                enabled = enabled,
                modifier = Modifier
                    .width(36.dp)
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = iconResource(R.drawable.ic_ok),
                    contentDescription = stringResource(R.string.save)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            FilledIconButton(
                onClick = onCancel,
                enabled = enabled,
                modifier = Modifier
                    .width(36.dp)
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = iconResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.cancel)
                )
            }
        }
    }
}

@Composable
private fun SubTaskRow(
    item: SubTask,
    enabled: Boolean,
    isEditing: Boolean,
    editingText: String,
    onEditingTextChange: (String) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onLongPressEdit: () -> Unit,
    onSubmitEdit: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    enabled = enabled && !isEditing,
                    onClick = onToggle,
                    onLongClick = onLongPressEdit
                )
                .padding(end = 12.dp, start = if (GlobalUtils.miuixMode || isEditing) 12.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = item.isCompleted == 1,
                onClick = if (enabled && !isEditing) onToggle else null,
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(10.dp))
            if (isEditing) {
                BasicTextField(
                    value = editingText,
                    onValueChange = onEditingTextChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.merge(
                        TextStyle(color = MaterialTheme.colorScheme.onSurface)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmitEdit() }),
                    decorationBox = { inner ->
                        if (editingText.isBlank()) {
                            Text(
                                text = stringResource(R.string.detail_plan_add_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                )
                FilledIconButton(
                    onClick = onSubmitEdit,
                    enabled = enabled,
                    modifier = Modifier
                        .width(36.dp)
                        .height(36.dp)
                ) {
                    Icon(
                        imageVector = iconResource(R.drawable.ic_ok),
                        contentDescription = stringResource(R.string.save)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                FilledIconButton(
                    onClick = onCancelEdit,
                    enabled = enabled,
                    modifier = Modifier
                        .width(36.dp)
                        .height(36.dp)
                ) {
                    Icon(
                        imageVector = iconResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
            } else {
                Text(
                    text = item.content,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.isCompleted == 1) TextDecoration.LineThrough else TextDecoration.None
                )
                FilledIconButton(
                    onClick = onDelete,
                    enabled = enabled,
                    modifier = Modifier
                        .width(36.dp)
                        .height(36.dp)
                ) {
                    Icon(
                        imageVector = iconResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.delete)
                    )
                }
            }
        }
    }
}

private fun formatDuration(context: Context, duration: Duration): String {
    val days = duration.toDays()
    val hours = duration.minusDays(days).toHours()
    val minutes = duration.minusDays(days).minusHours(hours).toMinutes()

    return if (days > 0) {
        context.getString(R.string.duration_full, days, hours, minutes)
    } else {
        context.getString(R.string.duration_hours_minutes, hours, minutes)
    }
}
