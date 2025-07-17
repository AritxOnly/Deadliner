package com.aritxonly.deadliner.composable.settings

import ApkDownloaderInstaller
import android.provider.Settings
import android.widget.Space
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.composable.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.web.WebUtils

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WebSettingsScreen(
    navigateUp: () -> Unit
) {
    val context = LocalContext.current

    var webEnabled by remember { mutableStateOf(GlobalUtils.cloudSyncEnable) }
    var serverHost by remember { mutableStateOf(GlobalUtils.cloudSyncServer) }
    var serverPort by remember { mutableIntStateOf(GlobalUtils.cloudSyncPort) }
    var serverToken by remember { mutableStateOf(GlobalUtils.cloudSyncConstantToken) }

    val hostFaultHint = stringResource(R.string.settings_web_host_fault)
    val hostSuccessHint = stringResource(R.string.settings_web_host_success)
    val hostIncompleteHint = stringResource(R.string.settings_web_host_incomplete)

    val onWebChange: (Boolean) -> Unit = {
        GlobalUtils.cloudSyncEnable = it
        webEnabled = it
    }
    val onHostChange: (String) -> Unit = {
        serverHost = it
    }
    val onPortChange: (String) -> Unit = {
        serverPort = if (it.isEmpty() || it.isBlank()) {
            5000
        } else {
            it.toIntOrNull()?:5000
        }
    }
    val onTokenChange: (String) -> Unit = {
        serverToken = it
    }
    val onSaveButtonClick: () -> Unit = {
        if (serverHost.isNullOrEmpty() || serverToken.isNullOrEmpty()) {
            Toast.makeText(context, hostIncompleteHint, Toast.LENGTH_SHORT).show()
        } else if (serverHost?.startsWith("https://") == true || serverHost?.startsWith("http://") == true) {
            GlobalUtils.cloudSyncServer = serverHost
            GlobalUtils.cloudSyncPort = serverPort
            GlobalUtils.cloudSyncConstantToken = serverToken
            WebUtils.init()
            Toast.makeText(context, hostSuccessHint, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, hostFaultHint, Toast.LENGTH_SHORT).show()
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
                    contentDescription = "返回",
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
                            value = serverHost ?: "",
                            onValueChange = onHostChange,
                            hint = stringResource(R.string.settings_web_host)
                        )

                        RoundedTextField(
                            value = serverPort.toString(),
                            onValueChange = onPortChange,
                            hint = stringResource(R.string.settings_web_port),
                            keyboardType = KeyboardType.Number
                        )

                        RoundedTextField(
                            value = serverToken ?: "",
                            onValueChange = onTokenChange,
                            hint = stringResource(R.string.settings_web_token),
                            keyboardType = KeyboardType.Password,
                            isPassword = true
                        )

                        Button(
                            onClick = onSaveButtonClick,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Text("保存")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }
    }
}