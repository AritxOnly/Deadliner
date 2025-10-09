package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.composable.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InterfaceSettingsScreen(
    navigateUp: () -> Unit
) {
    var progressDirEnabled by remember { mutableStateOf(GlobalUtils.progressDir) }
    var motivationalQuotesEnabled by remember { mutableStateOf(GlobalUtils.motivationalQuotes) }
    var fireworksOnFinishEnabled by remember { mutableStateOf(GlobalUtils.fireworksOnFinish) }
    var detailDisplayEnabled by remember { mutableStateOf(GlobalUtils.detailDisplayMode) }
    var hideDividerEnabled by remember { mutableStateOf(GlobalUtils.hideDivider) }
    var simplifiedEnabled by remember { mutableStateOf(GlobalUtils.style == "simplified") }

    val onProgressDirChange: (Boolean) -> Unit = {
        GlobalUtils.progressDir = it
        progressDirEnabled = it
    }
    val onMotivationalQuotesChange: (Boolean) -> Unit = {
        GlobalUtils.motivationalQuotes = it
        motivationalQuotesEnabled = it
    }
    val onFireworksOnFinishChange: (Boolean) -> Unit = {
        GlobalUtils.fireworksOnFinish = it
        fireworksOnFinishEnabled = it
    }
    val onDetailDisplayChange: (Boolean) -> Unit = {
        GlobalUtils.detailDisplayMode = it
        detailDisplayEnabled = it
    }
    val onHideDividerChange: (Boolean) -> Unit = {
        GlobalUtils.hideDivider = it
        hideDividerEnabled = it
    }
    val onSimplifiedChange: (Boolean) -> Unit = {
        GlobalUtils.style = if (it) "simplified" else "classic"
        simplifiedEnabled = it
    }

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_interface_display),
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
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SvgCard(R.drawable.svg_interface, modifier = Modifier.padding(16.dp))

            SettingsSection(topLabel = stringResource(R.string.settings_interface_design)) {
                SettingsDetailSwitchItem(
                    headline = R.string.settings_hide_divider,
                    supportingText = R.string.settings_support_hide_divider,
                    checked = hideDividerEnabled,
                    onCheckedChange = onHideDividerChange
                )

                SettingsSectionDivider()

                SettingsDetailSwitchItem(
                    headline = R.string.settings_simplified,
                    supportingText = R.string.settings_support_simplified,
                    checked = simplifiedEnabled,
                    onCheckedChange = onSimplifiedChange
                )
            }

            SettingsSection(topLabel = stringResource(R.string.settings_interface_mainscreen)) {
                SettingsDetailSwitchItem(
                    headline = R.string.settings_progress_dir_main,
                    supportingText = R.string.settings_support_progress_dir,
                    checked = progressDirEnabled,
                    onCheckedChange = onProgressDirChange
                )
                SettingsSectionDivider()

                SettingsDetailSwitchItem(
                    headline = R.string.settings_excitement,
                    supportingText = R.string.settings_support_excitement,
                    checked = motivationalQuotesEnabled,
                    onCheckedChange = onMotivationalQuotesChange
                )
                SettingsSectionDivider()

                SettingsDetailSwitchItem(
                    headline = R.string.settings_fireworks,
                    supportingText = R.string.settings_support_fireworks,
                    checked = fireworksOnFinishEnabled,
                    onCheckedChange = onFireworksOnFinishChange
                )
                SettingsSectionDivider()

                SettingsDetailSwitchItem(
                    headline = R.string.settings_detail_display,
                    supportingText = R.string.settings_support_detail_display,
                    checked = detailDisplayEnabled,
                    onCheckedChange = onDetailDisplayChange
                )

            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}