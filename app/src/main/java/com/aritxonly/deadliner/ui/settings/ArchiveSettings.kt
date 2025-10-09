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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun ArchiveSettingsScreen(
    navigateUp: () -> Unit
) {
    var archiveEnabled by remember { mutableStateOf(GlobalUtils.autoArchiveEnable) }
    var archiveDays by remember { mutableIntStateOf(GlobalUtils.autoArchiveTime) }

    val onArchiveEnabledChange: (Boolean) -> Unit = {
        GlobalUtils.autoArchiveEnable = it
        archiveEnabled = it
    }
    val onArchiveDaysChange: (Int) -> Unit = {
        GlobalUtils.autoArchiveTime = it
        archiveDays = it
    }

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_auto_archive_title),
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
            SettingsSection(
                mainContent = true,
                enabled = archiveEnabled
            ) {
                SettingsSwitchItem(
                    label = R.string.settings_auto_archive,
                    checked = archiveEnabled,
                    onCheckedChange = onArchiveEnabledChange,
                    mainSwitch = true
                )
            }

            SvgCard(R.drawable.svg_archive, modifier = Modifier.padding(16.dp))

            if (archiveEnabled) {
                SettingsSection {
                    SettingsSliderItemWithLabel(
                        label = R.string.settings_auto_archive_time,
                        value = archiveDays.toFloat(),
                        valueRange = 1f..7f,
                        steps = 5,
                        onValueChange = { onArchiveDaysChange(it.toInt()) }
                    )
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}