package com.aritxonly.deadliner.ui.settings

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.DisplayScaleManager
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DisplayScalePreset
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DisplayScaleSettingsScreen(
    navigateUp: () -> Unit,
) {
    val context = LocalContext.current
    var displayScalePreset by remember { mutableStateOf(GlobalUtils.displayScalePreset) }

    val displayScaleOptions = remember {
        listOf(
            RadioOption(DisplayScalePreset.FollowSystem, R.string.settings_display_size_follow_system),
            RadioOption(DisplayScalePreset.Compact, R.string.settings_display_size_compact),
            RadioOption(DisplayScalePreset.SlightlyCompact, R.string.settings_display_size_slightly_compact),
            RadioOption(DisplayScalePreset.Standard, R.string.settings_display_size_standard),
            RadioOption(DisplayScalePreset.SlightlyLarge, R.string.settings_display_size_slightly_large),
            RadioOption(DisplayScalePreset.Large, R.string.settings_display_size_large),
        )
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_display_size_title),
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
            SettingsSection(topLabel = stringResource(R.string.settings_display_size_title)) {
                SettingsRadioGroupItem(
                    options = displayScaleOptions,
                    selectedKey = displayScalePreset,
                    onOptionSelected = { preset ->
                        if (displayScalePreset == preset) return@SettingsRadioGroupItem
                        GlobalUtils.displayScalePreset = preset
                        displayScalePreset = preset
                        (context as? Activity)?.let(DisplayScaleManager::restartApp)
                    }
                )
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomDisplayScaleSettingsScreen(
    navigateUp: () -> Unit,
) {
    val context = LocalContext.current
    var customScaleMultiplier by remember { mutableFloatStateOf(GlobalUtils.customDisplayScaleMultiplier) }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_display_size_custom_title),
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
            SettingsSection(topLabel = stringResource(R.string.settings_experimental)) {
                SettingsSliderItemWithLabel(
                    label = R.string.settings_display_size_custom_ratio,
                    value = customScaleMultiplier * 100f,
                    valueRange = 85f..125f,
                    steps = 39,
                    onValueChange = {
                        customScaleMultiplier = it / 100f
                    }
                )
                SettingsSectionDivider()
                SettingsDetailTextButtonItem(
                    headlineText = stringResource(R.string.settings_apply_custom_display_scale),
                    supportingText = stringResource(
                        R.string.settings_support_apply_custom_display_scale,
                        (customScaleMultiplier * 100f).toInt()
                    )
                ) {
                    GlobalUtils.customDisplayScaleMultiplier = customScaleMultiplier
                    GlobalUtils.displayScalePreset = DisplayScalePreset.Custom
                    (context as? Activity)?.let(DisplayScaleManager::restartApp)
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
