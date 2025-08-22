package com.aritxonly.deadliner.composable.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.composable.SvgCard

@Composable
fun BackupSettingsScreen(
    handleImport: () -> Unit,
    handleExport: () -> Unit,
    navigateUp: () -> Unit,
) {
    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_backup),
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
            modifier = Modifier.padding(padding)
        ) {
            SvgCard(R.drawable.svg_backup, modifier = Modifier.padding(16.dp))

            SettingsSection {
                SettingsTextButtonItem(
                    text = R.string.settings_import,
                    iconRes = R.drawable.ic_import
                ) {
                    handleImport()
                }
                SettingsSectionDivider()
                SettingsTextButtonItem(
                    text = R.string.settings_export,
                    iconRes = R.drawable.ic_export
                ) {
                    handleExport()
                }
            }
        }
    }
}