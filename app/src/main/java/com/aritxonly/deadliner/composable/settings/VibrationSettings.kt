package com.aritxonly.deadliner.composable.settings

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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.composable.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils

@Composable
fun VibrationSettingsScreen(
    navigateUp: () -> Unit
) {
    var vibrationEnabled by remember { mutableStateOf(GlobalUtils.vibration) }
    var vibrationRealAmplitude by remember { mutableIntStateOf(GlobalUtils.vibrationAmplitude) }
    var vibrationAmplitude by remember { mutableFloatStateOf(196f) }

    val onVibrationChange: (Boolean) -> Unit = {
        GlobalUtils.vibration = it
        vibrationEnabled = it
    }
    val onVibrationAmplitudeChange: (Float) -> Unit = {
        vibrationAmplitude = it
        vibrationRealAmplitude = it.toInt()
        GlobalUtils.vibrationAmplitude = it.toInt()
    }

    val vibrationOpts = listOf(
        RadioOption("system", R.string.settings_vibration_system),
        RadioOption("custom", R.string.settings_vibration_custom)
    )
    val modeRightNow = if (vibrationRealAmplitude == -1) "system" else "custom"
    var vibrationModeSelected by remember { mutableStateOf(modeRightNow) }

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_vibration_title),
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
        Column(
            Modifier
                .padding(padding)
                .padding(vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(
                mainContent = true,
                enabled = vibrationEnabled
            ) {
                SettingsSwitchItem(
                    label = R.string.settings_vibration,
                    checked = vibrationEnabled,
                    onCheckedChange = onVibrationChange,
                    mainSwitch = true
                )
            }

            SvgCard(
                if (vibrationEnabled) R.drawable.svg_vibrate
                else R.drawable.svg_vibrate_off,
                modifier = Modifier.padding(16.dp)
            )

            if (vibrationEnabled) {
                SettingsSection {
                    SettingsRadioGroupItem(
                        options = vibrationOpts,
                        selectedKey = vibrationModeSelected,
                        onOptionSelected = {
                            vibrationModeSelected = it
                            GlobalUtils.vibrationAmplitude = if (vibrationModeSelected == "system") {
                                vibrationRealAmplitude = -1
                                -1
                            } else {
                                vibrationAmplitude.toInt()
                            }
                        },
                        divider = { SettingsSectionDivider() }
                    )

                    SettingsSectionDivider()

                    if (vibrationModeSelected == "custom") {
                        SettingsSliderItemWithLabel(
                            label = R.string.settings_vibration_amplitude,
                            value = vibrationAmplitude.toFloat(),
                            valueRange = 1f..255f,
                            onValueChange = onVibrationAmplitudeChange
                        )
                    }
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}