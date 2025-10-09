package com.aritxonly.deadliner.ui.settings

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.AppSingletons
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WebSettingsScreen(
    navigateUp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var webEnabled by remember { mutableStateOf(GlobalUtils.cloudSyncEnable) }
    var serverBase by remember { mutableStateOf(GlobalUtils.webDavBaseUrl) }
    var serverUser by remember { mutableStateOf(GlobalUtils.webDavUser) }
    var serverPass by remember { mutableStateOf(GlobalUtils.webDavPass) }
    var showSheet by remember { mutableStateOf(false) }
    var intervalMin by remember { mutableStateOf(GlobalUtils.syncIntervalMinutes.coerceAtLeast(0)) }
    var wifiOnly by remember { mutableStateOf(GlobalUtils.syncWifiOnly) }
    var chargingOnly by remember { mutableStateOf(GlobalUtils.syncChargingOnly) }

    val hostFaultHint = stringResource(R.string.settings_web_host_fault)
    val hostSuccessHint = stringResource(R.string.settings_web_host_success)
    val hostIncompleteHint = stringResource(R.string.settings_web_host_incomplete)

    val onWebChange: (Boolean) -> Unit = {
        GlobalUtils.cloudSyncEnable = it
        webEnabled = it

        if (GlobalUtils.cloudSyncEnable) {
            SyncScheduler.enqueuePeriodic(context)
        } else {
            SyncScheduler.cancelAll(context)
        }
    }
    val onBaseChange: (String) -> Unit = {
        serverBase = it
    }
    val onUserChange: (String) -> Unit = {
        serverUser = it
    }
    val onPassChange: (String) -> Unit = {
        serverPass = it
    }

    val onSaveButtonClick: () -> Unit = onSaveButtonClick@{
        if (serverBase.isEmpty() || serverUser.isEmpty() || serverPass.isEmpty()) {
            Toast.makeText(context, hostIncompleteHint, Toast.LENGTH_SHORT).show()
            return@onSaveButtonClick
        }
        if (!(serverBase.startsWith("https://") || serverBase.startsWith("http://"))) {
            Toast.makeText(context, hostFaultHint, Toast.LENGTH_SHORT).show()
            return@onSaveButtonClick
        }

        // 1) 本地保存配置
        GlobalUtils.webDavBaseUrl = serverBase
        GlobalUtils.webDavUser = serverUser
        GlobalUtils.webDavPass = serverPass
        AppSingletons.updateWeb()
        Toast.makeText(context, hostSuccessHint, Toast.LENGTH_SHORT).show()

        if (!webEnabled) return@onSaveButtonClick

        // 2) 异步检测可用性 & 触发一次同步
        scope.launch {
            try {
                Toast.makeText(
                    context,
                    context.getString(R.string.webdav_checking),
                    Toast.LENGTH_SHORT
                ).show()

                // MKCOL Deadliner
                runCatching { AppSingletons.web.mkcol("Deadliner") }

                // 检测：HEAD Deadliner/（404 也视为可用）
                val (code, _, _) = AppSingletons.web.head("Deadliner/")
                val usable = when (code) {
                    200, 204, 207, 404 -> true
                    else -> false
                }
                if (!usable) {
                    Log.e("WebDAV", code.toString())
                    Toast.makeText(
                        context,
                        context.getString(R.string.webdav_unusable, code),
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                Toast.makeText(
                    context,
                    context.getString(R.string.webdav_available),
                    Toast.LENGTH_SHORT
                ).show()

                val ok = AppSingletons.sync.syncOnce()
                if (ok) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.sync_done),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.sync_conflict),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                val msg = e.message ?: context.getString(R.string.unknown_error)
                Toast.makeText(
                    context,
                    context.getString(R.string.sync_failed_with_msg, msg),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if (GlobalUtils.cloudSyncEnable) {
            SyncScheduler.enqueuePeriodic(context)
        } else {
            SyncScheduler.cancelAll(context)
        }
    }

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_webdav),
        navigationIcon = {
            IconButton(onClick = navigateUp, modifier = Modifier.padding(start = 8.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                SettingsSection(
                    mainContent = true,
                    enabled = webEnabled
                ) {
                    SettingsSwitchItem(
                        label = R.string.settings_enable_webDAV,
                        checked = webEnabled,
                        onCheckedChange = onWebChange,
                        mainSwitch = true
                    )
                }
            }

            item { SvgCard(R.drawable.svg_cloud_sync, modifier = Modifier.padding(16.dp)) }

            item {
                Text(
                    stringResource(R.string.settings_webDAV_description),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (webEnabled) {
                item {
                    SettingsSection(
                        customColor = MaterialTheme.colorScheme.surface
                    ) {
                        RoundedTextField(
                            value = serverBase,
                            onValueChange = onBaseChange,
                            hint = stringResource(R.string.settings_web_host)
                        )

                        RoundedTextField(
                            value = serverUser.toString(),
                            onValueChange = onUserChange,
                            hint = stringResource(R.string.settings_web_user)
                        )

                        RoundedTextField(
                            value = serverPass,
                            onValueChange = onPassChange,
                            hint = stringResource(R.string.settings_web_pass),
                            keyboardType = KeyboardType.Password,
                            isPassword = true
                        )

                        Button(
                            onClick = onSaveButtonClick,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }

                item {
                    SettingsSection(topLabel = stringResource(R.string.settings_more)) {
                        SettingsDetailTextButtonItem(
                            headline = R.string.settings_web_auto_sync,
                            supporting = R.string.settings_support_web_auto_sync
                        ) {
                            showSheet = true
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }

        SyncIntervalBottomSheet(
            show = showSheet,
            onDismiss = { showSheet = false },
            intervalMin = intervalMin,
            onIntervalChange = { intervalMin = it },
            wifiOnly = wifiOnly,
            onWifiOnlyChange = { wifiOnly = it },
            chargingOnly = chargingOnly,
            onChargingOnlyChange = { chargingOnly = it },
            onSave = {
                GlobalUtils.syncIntervalMinutes = intervalMin
                GlobalUtils.syncWifiOnly = wifiOnly
                GlobalUtils.syncChargingOnly = chargingOnly

                if (GlobalUtils.cloudSyncEnable) {
                    if (intervalMin <= 0) {
                        SyncScheduler.cancelPeriodic(context)
                    } else {
                        SyncScheduler.enqueuePeriodic(context)
                    }
                } else {
                    SyncScheduler.cancelPeriodic(context)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncIntervalBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    intervalMin: Int,
    onIntervalChange: (Int) -> Unit,
    wifiOnly: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit,
    chargingOnly: Boolean,
    onChargingOnlyChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    if (!show) return

    // 允许“部分展开”，并在首次显示时默认半屏
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        runCatching { sheetState.partialExpand() }
    }

    val intervalOptions = remember {
        listOf(
            0    to R.string.sync_interval_manual,
            15   to R.string.sync_interval_15min,
            30   to R.string.sync_interval_30min,
            60   to R.string.sync_interval_1h,
            180  to R.string.sync_interval_3h,
            360  to R.string.sync_interval_6h,
            720  to R.string.sync_interval_12h,
            1440 to R.string.sync_interval_24h,
        )
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        // 用 Column 将主体内容与底部操作区分开：上面滚动、下面固定
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // ====== 可滚动的区域 ======
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                // 标题
                item {
                    Text(
                        text = stringResource(R.string.settings_web_auto_sync),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // 两个开关在最上面
                item {
                    SettingsDetailSwitchItem(
                        headline = R.string.settings_web_wifi_only,
                        supportingText = R.string.settings_support_web_wifi_only,
                        checked = wifiOnly,
                        onCheckedChange = onWifiOnlyChange,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    SettingsDetailSwitchItem(
                        headline = R.string.settings_web_charging_only,
                        supportingText = R.string.settings_support_web_charging_only,
                        checked = chargingOnly,
                        onCheckedChange = onChargingOnlyChange,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // 分割线（样式与 SettingsSectionDivider 一致）
                item {
                    SheetDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                // 分组标题
                item {
                    Text(
                        text = stringResource(R.string.settings_web_auto_sync_time),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }

                // 间隔单选
                items(
                    items = intervalOptions,
                    key = { it.first }
                ) { opt ->
                    val value = opt.first
                    val label = opt.second
                    RadioRow(
                        text = stringResource(label),
                        selected = (intervalMin == value),
                        onClick = {
                            onIntervalChange(value)
                            GlobalUtils.triggerVibration(context, 10L)
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // ====== 固定在底部的操作按钮 ======
            SheetDivider(modifier = Modifier.fillMaxWidth())

            var closing by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    enabled = !closing,
                    onClick = {
                        scope.launch {
                            closing = true
                            // 直接收起，带过渡动画
                            sheetState.hide()        // suspend 直到动画完成
                            onDismiss()              // 动画结束后真正移除
                            closing = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    enabled = !closing,
                    onClick = {
                        scope.launch {
                            closing = true
                            onSave()                 // 先执行业务保存
                            sheetState.hide()        // 平滑收起
                            onDismiss()              // 动画结束再关闭
                            closing = false
                        }
                    }
                ) {
                    if (closing) {
                        // 简单的反馈：保存/收起动画时显示一个小 Loading
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun RadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 圆角背景 + 轻微高亮
    val bg = if (selected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainer

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        color = Color.Transparent
    ) {
        ListItem(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp), // 让圆角更明显
            headlineContent = { Text(text) },
            trailingContent = {
                RadioButton(selected = selected, onClick = onClick)
            },
            leadingContent = null,
            tonalElevation = ListItemDefaults.Elevation,
            shadowElevation = ListItemDefaults.Elevation,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

/** 与 SettingsSectionDivider 一致的样式（厚度 2dp、同色系） */
@Composable
private fun SheetDivider(modifier: Modifier = Modifier, onContainer: Boolean = true) {
    if (!GlobalUtils.hideDividerUi) {
        HorizontalDivider(
            modifier = modifier,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    } else {
        Spacer(modifier = Modifier.height(0.dp))
    }
}

suspend fun checkWebDavConnection(): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        val (code, _, _) = AppSingletons.web.head("Deadliner/")
        code in listOf(200, 204, 207, 404)  // 常见“能连通”的状态
    } catch (_: Exception) {
        false
    }
}