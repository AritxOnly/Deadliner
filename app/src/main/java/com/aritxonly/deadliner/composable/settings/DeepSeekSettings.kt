package com.aritxonly.deadliner.composable.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsRoute
import com.aritxonly.deadliner.composable.PreviewCard
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.KeystorePreferenceManager
import com.aritxonly.deadliner.web.DeepSeekUtils
import com.aritxonly.deadliner.web.DeepSeekUtils.generateDeadline
import kotlinx.coroutines.launch

@Composable
fun DeepSeekSettingsScreen(
    nav: NavHostController,
    navigateUp: () -> Unit
) {
    val context = LocalContext.current

    var masterEnable by remember { mutableStateOf(GlobalUtils.deepSeekEnable) }
    val onMasterChange: (Boolean) -> Unit = {
        GlobalUtils.deepSeekEnable = it
        masterEnable = it
    }

    var apiKey by remember { mutableStateOf(KeystorePreferenceManager.retrieveAndDecrypt(context)) }
    val onApiKeyChange: (String) -> Unit = {
        apiKey = it
    }

    var clipboard by remember { mutableStateOf(GlobalUtils.clipboardEnable) }
    var onClipboardChange: (Boolean) -> Unit = {
        GlobalUtils.clipboardEnable = it
        clipboard = it
    }

    var test by remember { mutableStateOf("") }
    var testResp by remember { mutableStateOf("") }
    val onTestChange: (String) -> Unit = {
        test = it
    }
    val scope = rememberCoroutineScope()

    val successText = stringResource(R.string.settings_deepseek_success)
    val incompleteText = stringResource(R.string.settings_deepseek_incomplete)
    val onSaveButtonClick: () -> Unit = {
        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(context, incompleteText, Toast.LENGTH_SHORT).show()
        } else {
            KeystorePreferenceManager.encryptAndStore(context, apiKey?:"")
            Toast.makeText(context, successText, Toast.LENGTH_SHORT).show()
        }
    }

    var showTestDialog by remember { mutableStateOf(false) }

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_deepseek),
        navigationIcon = {
            IconButton(
                onClick = navigateUp,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState())) {
            SettingsSection(
                mainContent = true,
                enabled = masterEnable
            ) {
                SettingsSwitchItem(
                    label = R.string.settings_enable_deepseek,
                    checked = masterEnable,
                    onCheckedChange = onMasterChange,
                    mainSwitch = true
                )
            }

            PreviewCard(modifier = Modifier.padding(16.dp)) {
                DeepSeekLogo(
                    modifier = Modifier.fillMaxSize(),
                    iconSize = 64.dp,
                    wordmarkHeight = 48.dp,
                    enabled = masterEnable
                )
            }

            Text(
                stringResource(R.string.settings_deepseek_description),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodySmall
            )

            if (masterEnable) {
                SettingsSection(customColor = MaterialTheme.colorScheme.surface) {
                    RoundedTextField(
                        value = apiKey ?: "",
                        onValueChange = onApiKeyChange,
                        hint = stringResource(R.string.settings_deepseek_api_key),
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

                SettingsSection(topLabel = stringResource(R.string.settings_more)) {
                    SettingsSwitchItem(
                        label = R.string.settings_clipboard,
                        checked = clipboard,
                        onCheckedChange = onClipboardChange
                    )
                }

                SettingsSection(
                    topLabel = stringResource(R.string.settings_advance),
                ) {
                    SettingsDetailTextButtonItem(
                        headline = R.string.settings_deepseek_custom_prompt,
                        supporting = R.string.settings_support_deepseek_custom_prompt
                    ) {
                        nav.navigate(SettingsRoute.Prompt.route)
                    }

                    SettingsSectionDivider()

                    SettingsDetailTextButtonItem(
                        headline = R.string.settings_deepseek_test,
                        supporting = R.string.settings_support_deepseek_test
                    ) {
                        showTestDialog = true
                    }
                }

                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }

    if (showTestDialog) {
        AlertDialog(
            onDismissRequest = { showTestDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            testResp = try {
                                generateDeadline(test)
                            } catch (e: Exception) {
                                "Error: ${e.message}"
                            }
                        }
                    }
                ) { Text("测试") }
            },
            text = {
                Column {
                    RoundedTextField(
                        value = test,
                        onValueChange = onTestChange,
                        hint = "测试"
                    )

                    Text(text = testResp)
                }
            }
        )
    }
}

@Composable
fun DeepSeekLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp,
    wordmarkHeight: Dp = 24.dp,
    spacing: Dp = 4.dp,
    enabled: Boolean = true
) {
    val tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_deepseek),
            contentDescription = "DeepSeek Icon",
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.height(spacing))
        Icon(
            painter = painterResource(R.drawable.ic_deepseek_text),
            contentDescription = "DeepSeek Wordmark",
            tint = tint,
            modifier = Modifier
                .height(wordmarkHeight)
                .wrapContentWidth()
        )
    }
}