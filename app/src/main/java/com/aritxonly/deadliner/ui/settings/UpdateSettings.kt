package com.aritxonly.deadliner.ui.settings

import ApkDownloaderInstaller
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.SvgCard
import androidx.core.net.toUri
import com.aritxonly.deadliner.web.UpdateManager
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// UI 状态枚举
sealed class UpdateState {
    object Loading : UpdateState()
    data class UpToDate(val current: String) : UpdateState()
    data class Available(
        val current: String,
        val latest: String,
        val notes: String,
        val downloadUrl: String,
    ) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    navigateUp: () -> Unit,
) {
    val context = LocalContext.current

    val updateState by produceState<UpdateState>(initialValue = UpdateState.Loading) {
        withContext(Dispatchers.IO) {
            value = try {
                val info = UpdateManager.fetchUpdateInfo(context)
                if (UpdateManager.isNewer(info.currentVersion, info.latestVersion)) {
                    UpdateState.Available(info.currentVersion, info.latestVersion, info.releaseNotes, info.downloadUrl)
                } else {
                    UpdateState.UpToDate(info.currentVersion)
                }
            } catch (e: Exception) {
                UpdateState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_check_for_updates),
        navigationIcon = {
            IconButton(onClick = navigateUp, modifier = Modifier.padding(start = 8.dp)) {
                Icon(
                    painter            = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint               = MaterialTheme.colorScheme.onSurface,
                    modifier           = expressiveTypeModifier
                )
            }
        },
        bottomBar = {
            when (val s = updateState) {
                is UpdateState.Available -> {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = { ApkDownloaderInstaller(context).downloadAndInstall(s.downloadUrl) }) {
                                Text(stringResource(R.string.update_and_install))
                            }
                            FilledTonalButton(onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, s.downloadUrl.toUri())
                                )
                            }) {
                                Text(stringResource(R.string.download_from_browser))
                            }
                        }
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
                else -> {}
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)
            .verticalScroll(rememberScrollState())) {
            SvgCard(R.drawable.svg_update, modifier = Modifier.padding(16.dp))

            Box(
                Modifier.fillMaxSize()
            ) {
                when (val state = updateState) {
                    is UpdateState.Loading -> {
                        LoadingIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    is UpdateState.Error -> {
                        Text(
                            text = stringResource(R.string.check_for_updates_failed, state.message),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }

                    is UpdateState.UpToDate -> {
                        Column(
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(R.string.check_for_updates_new, state.current),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    is UpdateState.Available -> {
                        // 展示更新信息
                        Column(
                            Modifier
                                .padding(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.check_for_updates_found, state.latest),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.check_for_updates_current, state.current),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(16.dp))

                            // Markdown 文本
                            MarkdownText(
                                markdown = state.notes
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}