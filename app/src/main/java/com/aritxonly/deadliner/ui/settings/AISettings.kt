package com.aritxonly.deadliner.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.aritxonly.deadliner.ui.base.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsRoute
import com.aritxonly.deadliner.ai.AIUtils
import com.aritxonly.deadliner.ai.defaultLlmPreset
import com.aritxonly.deadliner.localutils.ApiKeystore
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AISettingsScreen(
    nav: NavHostController,
    navigateUp: () -> Unit
) {
    val context = LocalContext.current

    var apiKey by remember { mutableStateOf(ApiKeystore.retrieveAndDecrypt(context)) }
    val onApiKeyChange: (String) -> Unit = {
        apiKey = it
    }

    var clipboard by remember { mutableStateOf(GlobalUtils.clipboardEnable) }
    var onClipboardChange: (Boolean) -> Unit = {
        GlobalUtils.clipboardEnable = it
        clipboard = it
    }

    var advancedSettings by remember { mutableStateOf(GlobalUtils.advancedAISettings) }
    val onAdvancedChange: (Boolean) -> Unit = {
        GlobalUtils.advancedAISettings = it
        advancedSettings = it
    }

    var test by remember { mutableStateOf("") }
    var testResp by remember { mutableStateOf("") }
    val onTestChange: (String) -> Unit = {
        test = it
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val successText = stringResource(R.string.settings_ai_success)
    val incompleteText = stringResource(R.string.settings_ai_incomplete)
    val onSaveButtonClick: () -> Unit = {
        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(context, incompleteText, Toast.LENGTH_SHORT).show()
        } else {
            ApiKeystore.encryptAndStore(context, apiKey ?: "")
            Toast.makeText(context, successText, Toast.LENGTH_SHORT).show()
        }
    }

    var showTestDialog by remember { mutableStateOf(false) }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_deadliner_ai),
        navigationIcon = {
            IconButton(
                onClick = navigateUp,
                modifier = navIconPaddingModifier
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
        SettingsScrollColumn(
            contentPadding = padding,
            modifier = Modifier,
        ) {

            SvgCard(
                svgRes = R.drawable.svg_deadliner_ai,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                stringResource(R.string.settings_ai_description),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.padding(8.dp))

            val preset = GlobalUtils.getDeadlinerAIConfig().getCurrentPreset() ?: defaultLlmPreset
            if (advancedSettings) {
                SettingsSection(
                    customColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    InfoCardCentered(
                        headlineText = stringResource(R.string.settings_current_model, preset.name),
                        supportingText = stringResource(
                            R.string.settings_current_endpoint,
                            preset.endpoint
                        ),
                        textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        iconColor = MaterialTheme.colorScheme.tertiary
                    )
                }
            } else {
                QuotaPanelSimple(
                    endpoint = preset.endpoint,
                    appSecret = GlobalUtils.getDeadlinerAppSecret(context),
                    onClickPlan = {}
                )
            }

            SettingsSection(topLabel = stringResource(R.string.settings_ai_function)) {
                SettingsDetailSwitchItem(
                    headline = R.string.settings_clipboard,
                    supportingText = R.string.settings_support_clipboard,
                    checked = clipboard,
                    onCheckedChange = onClipboardChange
                )

                SettingsSectionDivider()

                SettingsDetailTextButtonItem(
                    headline = R.string.settings_ai_custom_prompt,
                    supporting = R.string.settings_support_ai_custom_prompt
                ) {
                    nav.navigate(SettingsRoute.Prompt.route)
                }
            }

            SettingsSection(
                topLabel = stringResource(R.string.settings_advance),
            ) {
                SettingsDetailSwitchItem(
                    headline = R.string.settings_model_settings,
                    supportingText = R.string.settings_model_settings_advance,
                    checked = advancedSettings,
                    onCheckedChange = onAdvancedChange
                )

                if (advancedSettings) {
                    SettingsSectionDivider()

                    SettingsDetailTextButtonItem(
                        headline = R.string.settings_ai_test,
                        supporting = R.string.settings_support_ai_test
                    ) {
                        showTestDialog = true
                    }

                    SettingsSectionDivider()

                    SettingsDetailTextButtonItem(
                        headline = R.string.settings_model_endpoint,
                        supporting = R.string.settings_support_model_endpoint
                    ) {
                        nav.navigate(SettingsRoute.Model.route)
                    }
                }
            }

            if (advancedSettings) {
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
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
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
                                val (_, json) = AIUtils.generateAuto(context, test)
                                json
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
