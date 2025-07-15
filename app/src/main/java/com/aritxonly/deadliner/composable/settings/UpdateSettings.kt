package com.aritxonly.deadliner.composable.settings

import ApkDownloaderInstaller
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.composable.SvgCard
import io.noties.markwon.Markwon
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import androidx.core.net.toUri
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
        val url = "https://api.github.com/repos/AritxOnly/Deadliner/releases/latest"

        withContext(Dispatchers.IO) {
            try {
                val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
                val current = pkg.versionName ?: "0.0.0"

                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("Code ${resp.code}")
                    val body = resp.body!!.string()
                    val json = JSONObject(body)
                    val latest = json.getString("tag_name")
                    if (compareSemVer(current, latest) < 0) {
                        val notesMd = json.getString("body")
                        // 用 Markwon 转 HTML
                        val assets = json.getJSONArray("assets")
                        val downloadUrl = if (assets.length() > 0)
                            assets.getJSONObject(0).getString("browser_download_url")
                        else ""
                        value = UpdateState.Available(current, latest, notesMd, downloadUrl)
                    } else {
                        value = UpdateState.UpToDate(current)
                    }
                }
            } catch (e: Exception) {
                value = UpdateState.Error(e.message ?: "Unknown error")
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
                    contentDescription = "返回",
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
                                Text("更新并安装")
                            }
                            FilledTonalButton(onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, s.downloadUrl.toUri())
                                )
                            }) {
                                Text("浏览器下载")
                            }
                        }
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
                else -> {}
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
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
                            text = "检查更新失败：${state.message}",
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
                                "当前已是最新版本：${state.current}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    is UpdateState.Available -> {
                        // 展示更新信息
                        Column(
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text(
                                "检测到新版本：${state.latest}",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "当前版本：${state.current}",
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

// SemVer 比较函数（同 MainActivity）
private fun compareSemVer(v1: String, v2: String): Int {
    val p1 = v1.removePrefix("v").split(".")
    val p2 = v2.removePrefix("v").split(".")
    val max = maxOf(p1.size, p2.size)
    for (i in 0 until max) {
        val s1 = p1.getOrNull(i).orEmpty()
        val s2 = p2.getOrNull(i).orEmpty()
        val n1 = s1.substringBefore('-').toIntOrNull() ?: 0
        val n2 = s2.substringBefore('-').toIntOrNull() ?: 0
        if (n1 != n2) return n1 - n2
        val suf1 = s1.substringAfter('-', "")
        val suf2 = s2.substringAfter('-', "")
        if (suf1.isEmpty() != suf2.isEmpty()) return if (suf1.isEmpty()) 1 else -1
        if (suf1 != suf2) return suf1.compareTo(suf2)
    }
    return 0
}