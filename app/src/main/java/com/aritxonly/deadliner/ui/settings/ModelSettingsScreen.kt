package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.LlmPreset
import com.aritxonly.deadliner.web.AIUtils
import java.util.UUID

@Composable
fun ModelSettingsScreen(
    navigateUp: () -> Unit
) {
    val cfg = remember { GlobalUtils.getDeadlinerAIConfig() }
    // 初始数据
    var modelPresets by remember { mutableStateOf(cfg.getPresets()) }
    var currentModel by remember { mutableStateOf(cfg.getCurrentPreset()) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showManageSheet by remember { mutableStateOf(false) }

    // 将预设映射为单选项
    val options = remember(modelPresets) {
        modelPresets.map { preset ->
            RadioOptionText(
                key = preset.id,
                label = preset.name
            )
        }
    }
    val selectedKey = currentModel?.id ?: options.firstOrNull()?.key

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_model_endpoint),
        navigationIcon = {
            IconButton(
                onClick = navigateUp,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        },
        actions = {
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(
                        painterResource(R.drawable.ic_more),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = expressiveTypeModifier
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_model)) },
                        onClick = { showMenu = false; showAddDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.manage_presets)) },
                        onClick = { showMenu = false; showManageSheet = true }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)
            .verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.settings_endpoint_description),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodySmall
            )

            SettingsSection {
                SettingsRadioGroupItem(
                    options = options,
                    selectedKey = selectedKey ?: "",
                    onOptionSelected = { newId ->
                        // 持久化当前选中
                        cfg.setCurrentPresetId(newId)

                        // 读取最新选中项，刷新本地状态
                        val updated = cfg.getCurrentPreset()
                        currentModel = updated
                        modelPresets = cfg.getPresets()

                        // 立即让全局 AIUtils 生效（切端点 + 模型名）
                        updated?.let { p ->
                            AIUtils.setPreset(p)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }

    if (showAddDialog) {
        AddPresetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, model, endpoint ->
                val newPreset = LlmPreset(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    model = model,
                    endpoint = endpoint
                )
                cfg.upsertPreset(newPreset)
                modelPresets = cfg.getPresets()
                currentModel = newPreset
                cfg.setCurrentPresetId(newPreset.id)
                AIUtils.setPreset(newPreset)

                showAddDialog = false
            }
        )
    }

    if (showManageSheet) {
        ManagePresetsSheet(
            presets = modelPresets,
            currentId = currentModel?.id,
            onClose = { showManageSheet = false },
            onDelete = { target ->
                // 不允许删当前 & 至少保留一个
                if (target.id == currentModel?.id || modelPresets.size <= 1) return@ManagePresetsSheet
                cfg.deletePreset(target.id)
                modelPresets = cfg.getPresets()
                currentModel = cfg.getCurrentPreset()
            },
            onEdit = { edited ->
                cfg.upsertPreset(edited)
                modelPresets = cfg.getPresets()
                if (edited.id == currentModel?.id) currentModel = cfg.getCurrentPreset()
            }
        )
    }
}

@Composable
fun AddPresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, model: String, endpoint: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && model.isNotBlank() && endpoint.isNotBlank()) {
                        onConfirm(name.trim(), model.trim(), endpoint.trim())
                    }
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.add_model)) },
        text = {
            Column {
                RoundedTextField(
                    value = name,
                    onValueChange = { name = it },
                    hint = stringResource(R.string.model_display_name)
                )
                RoundedTextField(
                    value = model,
                    onValueChange = { model = it },
                    hint = stringResource(R.string.model_internal_name)
                )
                RoundedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    hint = stringResource(R.string.model_endpoint)
                )
            }
        }
    )
}

@Composable
fun EditPresetDialog(
    initial: LlmPreset,
    onDismiss: () -> Unit,
    onConfirm: (name: String, model: String, endpoint: String) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var model by remember { mutableStateOf(initial.model) }
    var endpoint by remember { mutableStateOf(initial.endpoint) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { if (name.isNotBlank() && model.isNotBlank() && endpoint.isNotBlank()) onConfirm(name.trim(), model.trim(), endpoint.trim()) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(stringResource(R.string.edit_model)) },
        text = {
            Column {
                RoundedTextField(
                    value = name,
                    onValueChange = { name = it },
                    hint = stringResource(R.string.model_display_name)
                )
                RoundedTextField(
                    value = model,
                    onValueChange = { model = it },
                    hint = stringResource(R.string.model_internal_name)
                )
                RoundedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    hint = stringResource(R.string.model_endpoint)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePresetsSheet(
    presets: List<LlmPreset>,
    currentId: String?,
    onClose: () -> Unit,
    onDelete: (LlmPreset) -> Unit,
    onEdit: (LlmPreset) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmDelete by remember { mutableStateOf<LlmPreset?>(null) }
    var editTarget by remember { mutableStateOf<LlmPreset?>(null) }

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState) {
        Text(stringResource(R.string.manage_presets), modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium)
        presets.forEachIndexed { i, p ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.bodyLarge)
                    Text("${p.model} · ${p.endpoint}", style = MaterialTheme.typography.bodySmall)
                    if (p.id == currentId) Text(stringResource(R.string.current_in_use), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { editTarget = p }) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.edit))
                }
                val canDelete = p.id != currentId && presets.size > 1
                IconButton(onClick = { if (canDelete) confirmDelete = p }, enabled = canDelete) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete))
                }
            }
            if (i < presets.lastIndex) SettingsSectionDivider()
        }
        Spacer(Modifier.height(16.dp))
    }

    // 删除确认
    confirmDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            confirmButton = {
                TextButton(onClick = { onDelete(target); confirmDelete = null }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) } },
            title = { Text(stringResource(R.string.preset_delete_title)) },
            text = { Text(stringResource(R.string.preset_delete_msg, target.name)) }
        )
    }

    // 编辑对话框（与“添加”复用）
    editTarget?.let { t ->
        EditPresetDialog(
            initial = t,
            onDismiss = { editTarget = null },
            onConfirm = { name, model, endpoint ->
                onEdit(t.copy(name = name, model = model, endpoint = endpoint))
                editTarget = null
            }
        )
    }
}