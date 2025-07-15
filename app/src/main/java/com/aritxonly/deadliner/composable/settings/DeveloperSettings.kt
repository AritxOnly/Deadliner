package com.aritxonly.deadliner.composable.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.composable.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils

@Composable
fun DeveloperSettingsScreen(
    onClickCustomFilter: () -> Unit,
    onClickCancelAll: () -> Unit,
    onClickShowIntro: () -> Unit,
    navigateUp: () -> Unit
) {
    var experimentalE2E by remember { mutableStateOf(GlobalUtils.experimentalEdgeToEdge) }
    val onExperimentalE2EChange: (Boolean) -> Unit = {
        experimentalE2E = it
        GlobalUtils.experimentalEdgeToEdge = it
    }

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_developer),
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
        Column(modifier = Modifier.padding(padding)) {
            SvgCard(R.drawable.svg_developer_avatar, modifier = Modifier.padding(16.dp))

            SettingsSection(topLabel = "实验性功能") {
                SettingsDetailTextButtonItem(
                    headline = R.string.settings_custom_filter_list,
                    supporting = R.string.settings_support_custom_filter_list
                ) { onClickCustomFilter() }

                SettingsSectionDivider()

                SettingsSwitchItem(
                    label = R.string.settings_experimental_e2e,
                    checked = experimentalE2E,
                    onCheckedChange = onExperimentalE2EChange
                )
            }

            SettingsSection(topLabel = "开发者选项") {
                SettingsTextButtonItem(
                    text = R.string.clear_all_notification
                ) { onClickCancelAll() }

                SettingsSectionDivider()

                SettingsTextButtonItem(
                    text = R.string.settings_show_intro
                ) { onClickShowIntro() }
            }
        }
    }
}