package com.aritxonly.deadliner.ui.settings

import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.appcompat.widget.AppCompatImageView
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.AppIconManager
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.AppIconMode
import com.aritxonly.deadliner.ui.base.RadioButton
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIconSettingsScreen(
    navigateUp: () -> Unit,
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(GlobalUtils.appIconMode) }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_app_icon_title),
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
        },
    ) { padding ->
        SettingsScrollColumn(
            contentPadding = padding,
            modifier = Modifier,
        ) {
            SettingsSection(topLabel = stringResource(R.string.settings_app_icon_title)) {
                Text(
                    text = stringResource(R.string.settings_app_icon_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            SettingsSection {
                AppIconManager.selectableModes.forEachIndexed { index, mode ->
                    AppIconOptionRow(
                        mode = mode,
                        selected = selectedMode == mode,
                        onClick = {
                            selectedMode = mode
                            AppIconManager.applyMode(context, mode)
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_app_icon_applied_hint),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                    if (index != AppIconManager.selectableModes.lastIndex) {
                        SettingsSectionDivider()
                    }
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun AppIconOptionRow(
    mode: AppIconMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp,
        ) {
            AndroidView(
                factory = { viewContext ->
                    AppCompatImageView(viewContext).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageResource(AppIconManager.previewResFor(mode))
                    }
                },
                update = { imageView ->
                    imageView.setImageResource(AppIconManager.previewResFor(mode))
                },
                modifier = Modifier.size(52.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(AppIconManager.titleResFor(mode)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(AppIconManager.summaryResFor(mode)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}
