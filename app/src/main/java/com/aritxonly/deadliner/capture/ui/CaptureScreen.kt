package com.aritxonly.deadliner.capture.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.capture.CaptureEffect
import com.aritxonly.deadliner.capture.CaptureViewModel
import com.aritxonly.deadliner.capture.model.InspirationItem
import com.aritxonly.deadliner.ui.base.Scaffold
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.main.shared.mainListContainerClip
import com.aritxonly.deadliner.ui.settings.RoundedTextField
import com.aritxonly.deadliner.ui.settings.RoundedTextFieldMetrics
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureTopBar(
    vm: CaptureViewModel,
    onClose: () -> Unit,
    showNavigationIcon: Boolean = true,
    onRequestMerge: () -> Unit = {},
    forceMaterial3: Boolean = false,
) {
    val ui by vm.uiState.collectAsState()
    if (!ui.isMultiSelectMode) {
        TopAppBar(
            title = stringResource(R.string.capture_title),
            forceMaterial3 = forceMaterial3,
            navigationIcon = if (showNavigationIcon) {
                {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(R.string.close),
                            modifier = expressiveTypeModifier,
                        )
                    }
                }
            } else null,
            actions = {
                TextButton(onClick = vm::toggleMultiSelect) { Text(stringResource(R.string.capture_multi_select)) }
            },
            mode = if (showNavigationIcon) TopAppBarStyle.CENTER else TopAppBarStyle.SMALL
        )
    } else {
        TopAppBar(
            title = stringResource(R.string.capture_selected_count, ui.selectedIds.size),
            forceMaterial3 = forceMaterial3,
            navigationIcon = {
                IconButton(onClick = vm::exitMultiSelect) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close),
                        modifier = expressiveTypeModifier,
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = vm::deleteSelected,
                    enabled = ui.selectedIds.isNotEmpty()
                ) {
                    Text(stringResource(R.string.capture_delete))
                }
                TextButton(
                    onClick = onRequestMerge,
                    enabled = ui.selectedIds.size >= 2
                ) {
                    Text(stringResource(R.string.capture_merge_count, ui.selectedIds.size))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    vm: CaptureViewModel,
    onClose: () -> Unit,
    showTopBar: Boolean = true,
    showNavigationIcon: Boolean = true,
) {
    var showMergeSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (!showTopBar) return@Scaffold
            CaptureTopBar(
                vm = vm,
                onClose = onClose,
                showNavigationIcon = showNavigationIcon,
                onRequestMerge = { showMergeSheet = true },
            )
        }
    ) { innerPadding ->
        CaptureContent(
            vm = vm,
            contentPadding = innerPadding,
            showMergeSheet = showMergeSheet,
            onShowMergeSheetChange = { showMergeSheet = it },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CaptureContent(
    vm: CaptureViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    twoColumnLayout: Boolean = false,
    showMergeSheet: Boolean = false,
    onShowMergeSheetChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val ui by vm.uiState.collectAsState()
    var showDirectConvertMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.updateQuery("")
        vm.effects.collect { effect ->
            when (effect) {
                is CaptureEffect.ToastRes -> {
                    Toast.makeText(context, context.getString(effect.resId), Toast.LENGTH_SHORT).show()
                }
                is CaptureEffect.OpenTaskEditor -> {
                    copyToClipboard(context, effect.sourceText)
                    val intent = Intent(context, AddDDLActivity::class.java).apply {
                        putExtra("EXTRA_CURRENT_TYPE", 0)
                        putExtra(AddDDLActivity.EXTRA_PREFILL_TEXT, effect.sourceText)
                        putExtra(AddDDLActivity.EXTRA_AUTO_RUN_AI, effect.autoRunAi)
                        effect.generated?.let { putExtra("EXTRA_GENERATE_DDL", it) }
                    }
                    context.startActivity(intent)
                }
                is CaptureEffect.OpenHabitEditor -> {
                    copyToClipboard(context, effect.sourceText)
                    val intent = Intent(context, AddDDLActivity::class.java).apply {
                        putExtra("EXTRA_CURRENT_TYPE", 1)
                        putExtra(AddDDLActivity.EXTRA_PREFILL_TEXT, effect.sourceText)
                        putExtra(AddDDLActivity.EXTRA_AUTO_RUN_AI, effect.autoRunAi)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    if (ui.editingItemId != null) {
        CaptureDetailSheet(
            text = ui.editingText,
            loading = ui.loading,
            onTextChange = vm::updateEditingText,
            onClose = vm::closeDetail,
            onSave = vm::saveEditing,
            onDelete = {
                ui.editingItemId?.let(vm::deleteItem)
            },
            onAiTask = { vm.convertCurrentEditingToTask(useAi = true) },
            onAiHabit = { vm.convertCurrentEditingToHabit(useAi = true) },
            onDirectTask = { vm.convertCurrentEditingToTask(useAi = false) },
            onDirectHabit = { vm.convertCurrentEditingToHabit(useAi = false) }
        )
    }

    if (showMergeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onShowMergeSheetChange(false) },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.capture_merge_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.capture_merge_sheet_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(
                    onClick = {
                        onShowMergeSheetChange(false)
                        vm.mergeAndConvertToTask(useAi = true)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.capture_ai_task)) }
                FilledTonalButton(
                    onClick = {
                        onShowMergeSheetChange(false)
                        vm.mergeAndConvertToHabit(useAi = true)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.capture_ai_habit)) }

                Box {
                    OutlinedButton(
                        onClick = { showDirectConvertMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.capture_direct_secondary))
                    }
                    DropdownMenu(
                        expanded = showDirectConvertMenu,
                        onDismissRequest = { showDirectConvertMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_task)) },
                            onClick = {
                                showDirectConvertMenu = false
                                onShowMergeSheetChange(false)
                                vm.mergeAndConvertToTask(useAi = false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_habit)) },
                            onClick = {
                                showDirectConvertMenu = false
                                onShowMergeSheetChange(false)
                                vm.mergeAndConvertToHabit(useAi = false)
                            }
                        )
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .mainListContainerClip(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CaptureInputCard(
                        draftText = ui.draftText,
                        onDraftChange = vm::updateDraft,
                        onSave = vm::saveDraft
                    )
                }
            }

            if (ui.filteredItems.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.capture_list_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                }
            }

            if (twoColumnLayout && ui.filteredItems.isNotEmpty()) {
                item {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        maxItemsInEachRow = 2,
                    ) {
                        ui.filteredItems.forEach { item ->
                            InspirationLightCard(
                                item = item,
                                selected = ui.selectedIds.contains(item.id),
                                inMultiSelectMode = ui.isMultiSelectMode,
                                modifier = Modifier.weight(1f),
                                useHorizontalPadding = false,
                                onClick = {
                                    if (ui.isMultiSelectMode) vm.toggleSelect(item.id) else vm.openDetail(item.id)
                                },
                                onDelete = { vm.deleteItem(item.id) },
                                onAiTask = { vm.convertItemToTask(item.id, useAi = true) },
                                onAiHabit = { vm.convertItemToHabit(item.id, useAi = true) }
                            )
                        }
                    }
                }
            } else {
                items(ui.filteredItems, key = { it.id }) { item ->
                    InspirationLightCard(
                        item = item,
                        selected = ui.selectedIds.contains(item.id),
                        inMultiSelectMode = ui.isMultiSelectMode,
                        onClick = {
                            if (ui.isMultiSelectMode) vm.toggleSelect(item.id) else vm.openDetail(item.id)
                        },
                        onDelete = { vm.deleteItem(item.id) },
                        onAiTask = { vm.convertItemToTask(item.id, useAi = true) },
                        onAiHabit = { vm.convertItemToHabit(item.id, useAi = true) }
                    )
                }
            }

            if (ui.filteredItems.isEmpty()) {
                item {
                    EmptyCaptureHint(
                        text = stringResource(R.string.capture_empty_hint_default)
                    )
                }
            }

            item { Box(modifier = Modifier.size(16.dp)) }
    }
}

@Composable
private fun CaptureInputCard(
    draftText: String,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val sectionShape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = sectionShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.capture_input_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.capture_input_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.capture_input_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RoundedTextField(
                value = draftText,
                onValueChange = onDraftChange,
                hint = stringResource(R.string.capture_input_placeholder),
                metrics = RoundedTextFieldMetrics(
                    singleLine = false,
                    minHeight = 120.dp,
                    cornerSize = 12.dp
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.capture_save)) }
            }
        }
    }
}

@Composable
private fun InspirationLightCard(
    item: InspirationItem,
    selected: Boolean,
    inMultiSelectMode: Boolean,
    modifier: Modifier = Modifier,
    useHorizontalPadding: Boolean = true,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onAiTask: () -> Unit,
    onAiHabit: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val sectionShape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    val cardColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (useHorizontalPadding) Modifier.padding(horizontal = 16.dp) else Modifier)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = sectionShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_lightbulb),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "  ${formatRelativeTime(context, item.updatedAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                if (inMultiSelectMode) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .let {
                                if (selected) it else it.background(
                                    color = Color.Transparent,
                                    shape = CircleShape
                                )
                            }
                    )
                } else {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = stringResource(R.string.settings_more)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_delete)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_ai_task)) },
                            onClick = {
                                menuExpanded = false
                                onAiTask()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_ai_habit)) },
                            onClick = {
                                menuExpanded = false
                                onAiHabit()
                            }
                        )
                    }
                }
            }

            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.capture_card_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyCaptureHint(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CaptureDetailSheet(
    text: String,
    loading: Boolean,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onAiTask: () -> Unit,
    onAiHabit: () -> Unit,
    onDirectTask: () -> Unit,
    onDirectHabit: () -> Unit
) {
    var secondaryMenuExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close),
                        modifier = expressiveTypeModifier,
                    )
                }
                Text(
                    stringResource(R.string.capture_detail_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSave) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = stringResource(R.string.save)
                    )
                }
            }

            RoundedTextField(
                value = text,
                onValueChange = onTextChange,
                hint = stringResource(R.string.capture_detail_placeholder),
                metrics = RoundedTextFieldMetrics(
                    singleLine = false,
                    minHeight = 180.dp,
                    cornerSize = 12.dp
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (loading) {
                Text(
                    stringResource(R.string.capture_ai_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                stringResource(R.string.capture_ai_default_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = onAiTask) { Text(stringResource(R.string.capture_ai_task)) }
                FilledTonalButton(onClick = onAiHabit) { Text(stringResource(R.string.capture_ai_habit)) }
                Box {
                    OutlinedButton(onClick = { secondaryMenuExpanded = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = null
                        )
                        Text(" ${stringResource(R.string.capture_direct_secondary_short)}")
                    }
                    DropdownMenu(
                        expanded = secondaryMenuExpanded,
                        onDismissRequest = { secondaryMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_task)) },
                            onClick = {
                                secondaryMenuExpanded = false
                                onDirectTask()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.capture_direct_habit)) },
                            onClick = {
                                secondaryMenuExpanded = false
                                onDirectHabit()
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            TextButton(onClick = onDelete) { Text(stringResource(R.string.capture_delete_item)) }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.capture_clipboard_label), text))
    Toast.makeText(context, context.getString(R.string.capture_clipboard_copied), Toast.LENGTH_SHORT).show()
}

private fun formatRelativeTime(context: Context, timestampMillis: Long): String {
    val nowMillis = System.currentTimeMillis()
    val diff = Duration.ofMillis((nowMillis - timestampMillis).absoluteValue)
    val minutes = diff.toMinutes()
    val hours = diff.toHours()
    val days = diff.toDays()
    return when {
        minutes < 1 -> context.getString(R.string.capture_relative_just_now)
        minutes < 60 -> context.getString(R.string.capture_relative_minutes_ago, minutes)
        hours < 24 -> context.getString(R.string.capture_relative_hours_ago, hours)
        days < 7 -> context.getString(R.string.capture_relative_days_ago, days)
        else -> {
            val date = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestampMillis),
                ZoneId.systemDefault()
            )
            date.format(DateTimeFormatter.ofPattern(context.getString(R.string.capture_date_short_pattern)))
        }
    }
}
