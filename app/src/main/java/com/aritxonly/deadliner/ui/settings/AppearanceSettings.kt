package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsRoute
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceSettingsScreen(
    nav: NavHostController,
    navigateUp: () -> Unit,
) {
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    var progressDirEnabled by remember { mutableStateOf(GlobalUtils.progressDir) }
    var motivationalQuotesEnabled by remember { mutableStateOf(GlobalUtils.motivationalQuotes) }
    var fireworksOnFinishEnabled by remember { mutableStateOf(GlobalUtils.fireworksOnFinish) }
    var detailDisplayEnabled by remember { mutableStateOf(GlobalUtils.detailDisplayMode) }
    var hideDividerEnabled by remember { mutableStateOf(GlobalUtils.hideDivider) }
    var presetIndicatorEnabled by remember { mutableStateOf(GlobalUtils.presetIndicatorColor) }
    var selectedColorState by remember { mutableStateOf(GlobalUtils.seedColor) }
    val advancedMaterialEnabled = appearance.useAdvancedMaterial

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
    val onPresetIndicatorChange: (Boolean) -> Unit = {
        GlobalUtils.presetIndicatorColor = it
        presetIndicatorEnabled = it
    }
    val onThemeChange: (String?) -> Unit = {
        GlobalUtils.seedColor = it
        selectedColorState = it
    }
    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_interface_display),
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
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SvgCard(R.drawable.svg_interface, modifier = Modifier.padding(16.dp))

            SettingsSection(topLabel = stringResource(R.string.settings_interface_mainscreen)) {
                SettingsRoute.appearanceThirdRoutes.forEachIndexed { index, route ->
                    SettingItem(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth()
                            .clickable { nav.navigate(route.route) },
                        headlineText = stringResource(route.titleRes),
                        supportingText = stringResource(route.supportRes!!),
                        trailingContent = null
                    )

                    if (index != SettingsRoute.appearanceThirdRoutes.lastIndex) {
                        SettingsSectionDivider()
                    }
                }
            }

            SettingsSection(topLabel = stringResource(R.string.theme)) {
                ThemeColorPicker(
                    currentSeed = selectedColorState,
                    onColorSelected = onThemeChange
                )
            }

            SettingsSection(topLabel = stringResource(R.string.settings_interface_design)) {
                SettingsDetailSwitchItem(
                    headline = R.string.settings_hide_divider,
                    supportingText = R.string.settings_support_hide_divider,
                    checked = hideDividerEnabled,
                    onCheckedChange = onHideDividerChange
                )
                SettingsSectionDivider()

                SettingsDetailSwitchItem(
                    headline = R.string.settings_preset_indicator,
                    supportingText = R.string.settings_support_preset_indicator,
                    checked = presetIndicatorEnabled,
                    onCheckedChange = onPresetIndicatorChange
                )
                SettingsSectionDivider()

                SettingsDetailSwitchItem(
                    headline = R.string.settings_advanced_material,
                    supportingText = R.string.settings_support_advanced_material,
                    checked = advancedMaterialEnabled,
                    onCheckedChange = { enabled ->
                        GlobalUtils.updateAppearance { current ->
                            current.copy(useAdvancedMaterial = enabled)
                        }
                    }
                )
            }

            SettingsSection(topLabel = stringResource(R.string.settings_interface_display)) {
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
