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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils

@Composable
fun WidgetSettingsScreen(
    navigateUp: () -> Unit
) {
    var progressWidget by remember { mutableStateOf(GlobalUtils.progressWidget) }
    val onProgressWidgetChange: (Boolean) -> Unit = {
        progressWidget = it
        GlobalUtils.progressWidget = it
    }

    var mdWidgetAddBtn by remember { mutableStateOf(GlobalUtils.mdWidgetAddBtn) }
    val onMdWidgetAddBtnChange: (Boolean) -> Unit = {
        mdWidgetAddBtn = it
        GlobalUtils.mdWidgetAddBtn = it
    }

    var ldWidgetAddBtn by remember { mutableStateOf(GlobalUtils.ldWidgetAddBtn) }
    val onLdWidgetAddBtnChange: (Boolean) -> Unit = {
        ldWidgetAddBtn = it
        GlobalUtils.ldWidgetAddBtn = it
    }

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_widget),
        navigationIcon = {
            IconButton(onClick = navigateUp, modifier = Modifier.padding(start = 8.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SvgCard(R.drawable.svg_space, modifier = Modifier.padding(16.dp))

            SettingsSection(topLabel = stringResource(R.string.settings_widget_tasks)) {
                SettingsDetailSwitchItem(
                    headline = R.string.settings_progress_widget,
                    supportingText = R.string.settings_support_progress_widget,
                    checked = progressWidget,
                    onCheckedChange = onProgressWidgetChange
                )
                SettingsSectionDivider()

                SettingsDetailSwitchItem(
                    headline = R.string.settings_mdwidget_add_btn,
                    supportingText = R.string.settings_support_mdwidget_add_btn,
                    checked = mdWidgetAddBtn,
                    onCheckedChange = onMdWidgetAddBtnChange
                )

                SettingsSectionDivider()

                SettingsDetailSwitchItem(
                    headline = R.string.settings_ldwidget_add_btn,
                    supportingText = R.string.settings_support_ldwidget_add_btn,
                    checked = ldWidgetAddBtn,
                    onCheckedChange = onLdWidgetAddBtnChange
                )
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}