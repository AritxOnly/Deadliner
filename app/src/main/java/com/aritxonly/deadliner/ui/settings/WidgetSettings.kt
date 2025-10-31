package com.aritxonly.deadliner.ui.settings

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aritxonly.deadliner.AddDDLTileService
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils

@Composable
fun WidgetSettingsScreen(
    navigateUp: () -> Unit
) {
    val context = LocalContext.current

    var progressWidget by remember { mutableStateOf(GlobalUtils.progressWidget) }
    val onProgressWidgetChange: (Boolean) -> Unit = {
        progressWidget = it
        GlobalUtils.progressWidget = it
    }

    var mdWidgetAddBtn by remember { mutableStateOf(GlobalUtils.mdWidgetAddBtn) }
    val onMdWidgetAddBtnChange: (Boolean) -> Unit = {
        mdWidgetAddBtn = it
        GlobalUtils.mdWidgetAddBtn = it
    }

    var ldWidgetAddBtn by remember { mutableStateOf(GlobalUtils.ldWidgetAddBtn) }
    val onLdWidgetAddBtnChange: (Boolean) -> Unit = {
        ldWidgetAddBtn = it
        GlobalUtils.ldWidgetAddBtn = it
    }

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_widget),
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
        Column(
            Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SvgCard(R.drawable.svg_space, modifier = Modifier.padding(16.dp))

            SettingsSection(topLabel = stringResource(R.string.settings_widget_tasks)) {
                SettingsDetailSwitchItem(
                    headline = R.string.settings_progress_widget,
                    supportingText = R.string.settings_support_progress_widget,
                    checked = progressWidget,
                    onCheckedChange = onProgressWidgetChange
                )
                SettingsSectionDivider()

                SettingsDetailSwitchItem(
                    headline = R.string.settings_mdwidget_add_btn,
                    supportingText = R.string.settings_support_mdwidget_add_btn,
                    checked = mdWidgetAddBtn,
                    onCheckedChange = onMdWidgetAddBtnChange
                )

                SettingsSectionDivider()

                SettingsDetailSwitchItem(
                    headline = R.string.settings_ldwidget_add_btn,
                    supportingText = R.string.settings_support_ldwidget_add_btn,
                    checked = ldWidgetAddBtn,
                    onCheckedChange = onLdWidgetAddBtnChange
                )
            }

            SettingsSection(topLabel = stringResource(R.string.control_center_tile)) {
                SettingsDetailTextButtonItem(
                    headline = R.string.add_ddl_tile,
                    supporting = R.string.add_ddl_tile_supporting,
                ) {
                    val component = ComponentName(context, AddDDLTileService::class.java)
                    requestTile(
                        context,
                        component,
                        R.drawable.ic_add_ddl_tile,
                        ContextCompat.getString(context, R.string.add_ddl_tile_label)
                    )
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

internal fun requestTile(context: Context, component: ComponentName, @DrawableRes res: Int, appLabel: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val sbm = context.getSystemService(StatusBarManager::class.java)
        val icon = android.graphics.drawable.Icon.createWithResource(context, res)

        sbm.requestAddTileService(
            /* tileServiceComponentName = */ component,
            /* tileLabel = */ appLabel,
            /* icon = */ icon,
            /* resultExecutor = */ (context as? Activity)?.mainExecutor ?: context.mainExecutor
        ) { result ->
            when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
                    // 已成功添加
                    Toast.makeText(context, "已添加到快捷设置", Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                    Toast.makeText(context, "已在快捷设置中", Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> {
                    Toast.makeText(context, "用户拒绝添加", Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_ERROR_APP_NOT_IN_FOREGROUND -> {
                    Toast.makeText(context, "需要在前台界面调用", Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_ERROR_BAD_COMPONENT -> {
                    Toast.makeText(context, "组件未导出或未启用", Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_ERROR_MISMATCHED_PACKAGE -> {
                    Toast.makeText(context, "包名与 TileService 不一致", Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_ERROR_REQUEST_IN_PROGRESS -> {
                    Toast.makeText(context, "已有请求正在处理", Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_ERROR_NO_STATUS_BAR_SERVICE -> {
                    Toast.makeText(context, "系统服务不可用", Toast.LENGTH_SHORT).show()
                }
            }
        }
    } else {
        Toast.makeText(context, "仅支持 Android 13+ 的磁贴快速添加", Toast.LENGTH_SHORT).show()
    }
}