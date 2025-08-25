package com.aritxonly.deadliner.composable.settings

import android.util.Log
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.AppSingletons
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.composable.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BackupSettingsScreen(
    handleImport: () -> Unit,
    handleExport: () -> Unit,
    handleWebSettings: () -> Unit,
    navigateUp: () -> Unit,
) {
    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

            SettingsSection {
                SettingsTextButtonItem(
                    text = R.string.settings_from_webdav,
                    iconRes = R.drawable.ic_cloud_download
                ) {
                    if (GlobalUtils.cloudSyncEnable && GlobalUtils.webDavBaseUrl.isNotBlank()
                        && GlobalUtils.webDavUser.isNotBlank() && GlobalUtils.webDavPass.isNotBlank()
                    ) {
                        AppSingletons.updateWeb()

                        scope.launch {
                            try {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.webdav_checking),
                                    Toast.LENGTH_SHORT
                                ).show()

                                // MKCOL Deadliner
                                runCatching { AppSingletons.web.mkcol("Deadliner") }

                                // 检测：HEAD Deadliner/（404 也视为可用）
                                val (code, _, _) = AppSingletons.web.head("Deadliner/")
                                val usable = when (code) {
                                    200, 204, 207, 404 -> true
                                    else -> false
                                }
                                if (!usable) {
                                    Log.e("WebDAV", code.toString())
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.webdav_unusable, code),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@launch
                                }

                                Toast.makeText(
                                    context,
                                    context.getString(R.string.webdav_available),
                                    Toast.LENGTH_SHORT
                                ).show()

                                val ok = AppSingletons.sync.syncOnce()
                                if (ok) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.sync_done),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.sync_conflict),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                val msg = e.message ?: context.getString(R.string.unknown_error)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.sync_failed_with_msg, msg),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        handleWebSettings()
                    }
                }
            }
        }
    }
}