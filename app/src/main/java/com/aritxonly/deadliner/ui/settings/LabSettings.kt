package com.aritxonly.deadliner.ui.settings

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.ModernColorPalette
import com.aritxonly.deadliner.model.UiStyle
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@Composable
fun LabSettingsScreen(
    onClickCustomDisplayScale: () -> Unit,
    onClickCustomFilter: () -> Unit,
    onClickCancelAll: () -> Unit,
    onClickShowIntro: () -> Unit,
    navigateUp: () -> Unit
) {
    val currentStyle by GlobalUtils.styleFlow.collectAsState()
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    var miuixModeEnabled by remember { mutableStateOf(GlobalUtils.miuixMode) }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_lab),
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
            SvgCard(R.drawable.svg_developer_avatar, modifier = Modifier.padding(vertical = 8.dp))

            SettingsSection(topLabel = stringResource(R.string.settings_experimental)) {
                if (currentStyle != UiStyle.Classic) {
                    SettingsDetailSwitchItem(
                        headline = R.string.settings_miuix_mode,
                        supportingText = R.string.settings_support_miuix_mode,
                        checked = miuixModeEnabled,
                        onCheckedChange = {
                            GlobalUtils.miuixMode = it
                            miuixModeEnabled = it
                        }
                    )
                    SettingsSectionDivider()

                    SettingsDetailSwitchItem(
                        headline = R.string.settings_miuix_material_top_bar,
                        supportingText = R.string.settings_support_miuix_material_top_bar,
                        checked = appearance.useMaterialTopAppBarInMiuix,
                        onCheckedChange = { enabled ->
                            GlobalUtils.updateAppearance { current ->
                                current.copy(useMaterialTopAppBarInMiuix = enabled)
                            }
                        }
                    )
                    if (miuixModeEnabled) {
                        SettingsSectionDivider()
                        ModernColorPalettePicker(
                            selectedPalette = appearance.modernColorPalette,
                            onSelect = { palette ->
                                GlobalUtils.modernColorPalette = palette
                            },
                        )
                    } else {
                        SettingsSectionDivider()
                    }
                }

                SettingsDetailTextButtonItem(
                    headline = R.string.settings_display_size_custom_title,
                    supporting = R.string.settings_support_display_size_lab
                ) { onClickCustomDisplayScale() }
            }

            SettingsSection(topLabel = stringResource(R.string.settings_advance)) {
                SettingsDetailTextButtonItem(
                    headline = R.string.settings_custom_filter_list,
                    supporting = R.string.settings_support_custom_filter_list
                ) { onClickCustomFilter() }
            }

//            SettingsSection(topLabel = stringResource(R.string.settings_developer_options)) {
//                SettingsTextButtonItem(
//                    text = R.string.clear_all_notification
//                ) { onClickCancelAll() }
//
//                SettingsSectionDivider()
//
//                SettingsTextButtonItem(
//                    text = R.string.settings_show_intro
//                ) { onClickShowIntro() }
//            }
        }
    }
}

@Composable
private fun ModernColorPalettePicker(
    selectedPalette: ModernColorPalette,
    onSelect: (ModernColorPalette) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_modern_palette_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = stringResource(R.string.settings_modern_palette_support),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(modifier = Modifier.size(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(ModernColorPalette.entries, key = { it.key }) { palette ->
                ModernColorPaletteCard(
                    palette = palette,
                    selected = palette == selectedPalette,
                    onClick = { onSelect(palette) },
                )
            }
        }
    }
}

@Composable
private fun ModernColorPaletteCard(
    palette: ModernColorPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val preview = palette.previewLight
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = if (selected) 2.dp else 0.dp,
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .clickable(onClick = onClick),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(min = 140.dp)
                .padding(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaletteDot(preview.surfaceHex, Modifier.weight(1f))
                PaletteDot(preview.surfaceContainerHex, Modifier.weight(1f))
                PaletteDot(preview.searchBarHex, Modifier.weight(1f))
                PaletteDot(preview.textSecondaryHex, Modifier.weight(1f))
            }
            Text(
                text = palette.label(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = palette.summary(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PaletteDot(
    hex: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Color(if (hex.startsWith("#")) hex.toColorInt() else "#$hex".toColorInt())),
    )
}

@Composable
private fun ModernColorPalette.label(): String = when (this) {
    ModernColorPalette.HyperOs -> stringResource(R.string.settings_modern_palette_hyperos)
    ModernColorPalette.Honor -> stringResource(R.string.settings_modern_palette_honor)
    ModernColorPalette.Oppo -> stringResource(R.string.settings_modern_palette_oppo)
    ModernColorPalette.Vivo -> stringResource(R.string.settings_modern_palette_vivo)
}

@Composable
private fun ModernColorPalette.summary(): String = when (this) {
    ModernColorPalette.HyperOs -> stringResource(R.string.settings_modern_palette_hyperos_support)
    ModernColorPalette.Honor -> stringResource(R.string.settings_modern_palette_honor_support)
    ModernColorPalette.Oppo -> stringResource(R.string.settings_modern_palette_oppo_support)
    ModernColorPalette.Vivo -> stringResource(R.string.settings_modern_palette_vivo_support)
}
