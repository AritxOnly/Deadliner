package com.aritxonly.deadliner.composable.settings

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.AppSingletons
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.composable.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.web.WebUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    val hostFaultHint = stringResource(R.string.settings_web_host_fault)
    val hostSuccessHint = stringResource(R.string.settings_web_host_success)
    val hostIncompleteHint = stringResource(R.string.settings_web_host_incomplete)

    val onWebChange: (Boolean) -> Unit = {
        GlobalUtils.cloudSyncEnable = it
        webEnabled = it
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
                            value = serverPass ?: "",
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
            }

            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }
    }
}