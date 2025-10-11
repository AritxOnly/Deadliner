@file:OptIn(ExperimentalMaterial3Api::class)
package com.aritxonly.deadliner.ui.settings

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils.getOrCreateDeviceId
import com.aritxonly.deadliner.model.DeadlinerCheckResp
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.web.fetchQuotaCheck
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun QuotaPanelSimple(
    endpoint: String,
    appSecret: String,
    modifier: Modifier = Modifier,
    onClickPlan: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val deviceId = remember { getOrCreateDeviceId(ctx) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 初次加载
    val state by produceState<Result<DeadlinerCheckResp>?>(initialValue = null, endpoint, appSecret, deviceId) {
        value = fetchQuotaCheck(endpoint, appSecret, deviceId)
    }

    val _latestResult = remember { mutableStateOf(state) }
    val result = _latestResult.value ?: state

    fun refresh() {
        if (refreshing) return
        refreshing = true
        scope.launch {
            val r = fetchQuotaCheck(endpoint, appSecret, deviceId)
            _latestResult.value = r
            refreshing = false
        }
    }

    val corner = dimensionResource(R.dimen.item_corner_radius)
    val shape = RoundedCornerShape(corner)

    // 外层容器透明；保留形状以统一圆角（比如后续要加阴影、边框也方便）
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            // 顶部：标题 + 身份 + 刷新
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.quota_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(8.dp))

                val isSuccess = result?.isSuccess == true
                AssistChip(
                    onClick = onClickPlan,
                    label = {
                        Text(
                            if (isSuccess) {
                                val vip = result.getOrNull()?.vip == true
                                if (vip) stringResource(R.string.quota_identity_vip)
                                else stringResource(R.string.quota_identity_free)
                            } else stringResource(R.string.quota_identity_unknown)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isSuccess && result.getOrNull()?.vip == true)
                                iconResource(R.drawable.ic_plan_vip) else iconResource(R.drawable.ic_plan_free),
                            contentDescription = null
                        )
                    },
                    shape = shape,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color.Transparent
                    ),
                )

                Spacer(Modifier.weight(1f))

                FilledTonalButton(
                    onClick = { refresh() },
                    enabled = !refreshing,
                    shape = shape
                ) {
                    Row {
                        Icon(
                            iconResource(R.drawable.ic_autorenew),
                            contentDescription = stringResource(R.string.quota_action_refresh_cd),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (refreshing) stringResource(R.string.quota_action_refreshing)
                            else stringResource(R.string.quota_action_refresh),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Crossfade(targetState = result, label = "quota-state") { res ->
                when {
                    res == null -> LoadingBlock(shape)
                    res.isFailure -> ErrorBlock(
                        throwable = res.exceptionOrNull(),
                        onRetry = { refresh() },
                        shape = shape
                    )
                    res.isSuccess -> SuccessBlock(
                        info = res.getOrNull()!!,
                        shape = shape
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingBlock(shape: RoundedCornerShape) {
    Column {
        // 简化：纯进度条 + 两行占位文字，全部透明背景
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(iconResource(R.drawable.ic_devices), contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.width(8.dp))
            Text("— — —", color = MaterialTheme.colorScheme.outlineVariant)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(iconResource(R.drawable.ic_calendar), contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.width(8.dp))
            Text("— — —", color = MaterialTheme.colorScheme.outlineVariant)
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(shape)
        )
    }
}

@Composable
private fun ErrorBlock(
    throwable: Throwable?,
    onRetry: () -> Unit,
    shape: RoundedCornerShape
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(iconResource(R.drawable.ic_error), contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.quota_error_text, throwable?.message ?: stringResource(R.string.quota_error_unknown)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry, shape = shape) {
            Text(stringResource(R.string.quota_action_retry))
        }
    }
}

@Composable
private fun SuccessBlock(
    info: DeadlinerCheckResp,
    shape: RoundedCornerShape
) {
    val number = remember { NumberFormat.getIntegerInstance() }
    val limit = info.limit.coerceAtLeast(0)
    val used = info.used.coerceAtLeast(0)
    val progressRaw = if (limit > 0) used.toFloat() / limit.toFloat() else 0f
    val progress by animateFloatAsState(targetValue = progressRaw.coerceIn(0f, 1f), label = "quota-progress")

    // 信息行：无背景，仅图标 + 文本
    Column {
        InfoRow(
            icon = iconResource(R.drawable.ic_devices),
            label = stringResource(R.string.quota_device_label),
            value = info.deviceId,
            mono = true
        )

        Spacer(Modifier.height(14.dp))

        // 进度条 + 百分比（透明背景）
        Box(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(shape),
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }

        Spacer(Modifier.height(12.dp))

        val vipText =
            if (info.vip) stringResource(R.string.quota_identity_vip) else stringResource(R.string.quota_identity_free)

        val stats = listOf(
            StatItem(
                stringResource(R.string.quota_used_label),
                "${number.format(used)} / ${number.format(limit)}"
            ),
            StatItem(
                stringResource(R.string.quota_remaining_label),
                number.format(info.remaining.coerceAtLeast(0))
            ),
            StatItem(stringResource(R.string.quota_month_label), "${info.month} · $vipText"),
            StatItem(stringResource(R.string.quota_reset_label), formatIsoToLocal(info.reset_at)),
        )

        StatsGrid2Column(stats = stats, shape = shape)
    }
}

private data class StatItem(val title: String, val value: String)

@Composable
private fun StatsGrid2Column(
    stats: List<StatItem>,
    shape: RoundedCornerShape
) {
    val rows = remember(stats) { stats.chunked(2) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.getOrNull(0)?.let {
                    StatChip(title = it.title, value = it.value, modifier = Modifier.weight(1f), shape = shape)
                } ?: Spacer(Modifier.weight(1f))
                row.getOrNull(1)?.let {
                    StatChip(title = it.title, value = it.value, modifier = Modifier.weight(1f), shape = shape)
                } ?: Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    mono: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label：",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = if (mono) FontFamily.Monospace else MaterialTheme.typography.bodyMedium.fontFamily
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape
) {
    // 透明容器 + 细边框，保持信息分组但不铺底色
    OutlinedCard(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun formatIsoToLocal(iso: String): String {
    return runCatching {
        val zdt = Instant.parse(iso).atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(zdt)
    }.getOrElse { iso }
}