package com.aritxonly.deadliner.composable.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
fun LabSettingsScreen(
    onClickCustomFilter: () -> Unit,
    onClickCancelAll: () -> Unit,
    onClickShowIntro: () -> Unit,
    navigateUp: () -> Unit
) {

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_lab),
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
        Column(modifier = Modifier.padding(padding)
            .verticalScroll(rememberScrollState())) {
            SvgCard(R.drawable.svg_developer_avatar, modifier = Modifier.padding(16.dp))

            SettingsSection(topLabel = stringResource(R.string.settings_advance)) {
                SettingsDetailTextButtonItem(
                    headline = R.string.settings_model_endpoint,
                    supporting = R.string.settings_support_model_endpoint_advance
                ) {  }
            }

            SettingsSection(topLabel = stringResource(R.string.settings_experimental)) {
                SettingsDetailTextButtonItem(
                    headline = R.string.settings_custom_filter_list,
                    supporting = R.string.settings_support_custom_filter_list
                ) { onClickCustomFilter() }
            }

            SettingsSection(topLabel = stringResource(R.string.settings_developer_options)) {
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