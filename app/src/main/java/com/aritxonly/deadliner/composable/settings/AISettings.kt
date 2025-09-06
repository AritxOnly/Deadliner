package com.aritxonly.deadliner.composable.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsRoute
import com.aritxonly.deadliner.composable.PreviewCard
import com.aritxonly.deadliner.composable.SvgCard
import com.aritxonly.deadliner.composable.expressiveTypeModifier
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.KeystorePreferenceManager
import com.aritxonly.deadliner.model.LlmPreset
import com.aritxonly.deadliner.model.defaultLlmPreset
import com.aritxonly.deadliner.web.AIUtils.generateDeadline
import kotlinx.coroutines.launch

@Composable
fun AISettingsScreen(
    nav: NavHostController,
    navigateUp: () -> Unit
) {
    val context = LocalContext.current

    var masterEnable by remember { mutableStateOf(GlobalUtils.deadlinerAIEnable) }
    val onMasterChange: (Boolean) -> Unit = {
        GlobalUtils.deadlinerAIEnable = it
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

    val successText = stringResource(R.string.settings_ai_success)
    val incompleteText = stringResource(R.string.settings_ai_incomplete)
    val onSaveButtonClick: () -> Unit = {
        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(context, incompleteText, Toast.LENGTH_SHORT).show()
        } else {
            KeystorePreferenceManager.encryptAndStore(context, apiKey?:"")
            Toast.makeText(context, successText, Toast.LENGTH_SHORT).show()
        }
    }

    var showTestDialog by remember { mutableStateOf(false) }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_deadliner_ai),
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
                    label = R.string.settings_enable_ai,
                    checked = masterEnable,
                    onCheckedChange = onMasterChange,
                    mainSwitch = true
                )
            }

            SvgCard(
                svgRes = R.drawable.svg_deadliner_ai,
                modifier = Modifier.padding(16.dp)
            )

            Text(
                stringResource(R.string.settings_ai_description),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodySmall
            )

            if (masterEnable) {

                val preset = GlobalUtils.getDeadlinerAIConfig().getCurrentPreset()?: defaultLlmPreset
                SettingsSection(
                    customColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    InfoCardCentered(
                        headlineText = stringResource(R.string.settings_current_model, preset.name),
                        supportingText = stringResource(
                            R.string.settings_current_endpoint,
                            preset.endpoint
                        ),
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                SettingsSection(topLabel = stringResource(R.string.settings_model)) {
                    SettingsDetailTextButtonItem(
                        headline = R.string.settings_model_endpoint,
                        supporting = R.string.settings_support_model_endpoint
                    ) {
                        nav.navigate(SettingsRoute.Model.route)
                    }
                }

                SettingsSection(customColor = MaterialTheme.colorScheme.surface) {
                    RoundedTextField(
                        value = apiKey ?: "",
                        onValueChange = onApiKeyChange,
                        hint = stringResource(R.string.settings_ai_api_key),
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
                        headline = R.string.settings_ai_custom_prompt,
                        supporting = R.string.settings_support_ai_custom_prompt
                    ) {
                        nav.navigate(SettingsRoute.Prompt.route)
                    }

                    SettingsSectionDivider()

                    SettingsDetailTextButtonItem(
                        headline = R.string.settings_ai_test,
                        supporting = R.string.settings_support_ai_test
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
                                generateDeadline(context, test)
                            } catch (e: Exception) {
                                "Error: ${e.message}"
                            }
                        }
                    }
                ) { Text(stringResource(R.string.test)) }
            },
            text = {
                Column {
                    RoundedTextField(
                        value = test,
                        onValueChange = onTestChange,
                        hint = stringResource(R.string.test)
                    )

                    Text(text = testResp)
                }
            }
        )
    }
}