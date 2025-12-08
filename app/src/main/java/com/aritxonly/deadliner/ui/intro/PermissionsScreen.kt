package com.aritxonly.deadliner.ui.intro

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.net.toUri
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.iconResource

@SuppressLint("BatteryLife")
@Composable
fun PermissionsScreen() {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    // ==== 初始状态检测 ====
    var notificationGranted by remember {
        mutableStateOf(isNotificationGranted(context))
    }
    var calendarGranted by remember {
        mutableStateOf(isCalendarGranted(context))
    }
    var batteryIgnored by remember {
        mutableStateOf(isIgnoringBatteryOptimizations(context))
    }
    var wikiVisited by remember { mutableStateOf(false) }

    // ==== Launcher ====
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationGranted = granted || notificationGranted
        }

    val calendarLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { map ->
            val readOk = map[Manifest.permission.READ_CALENDAR] == true
            val writeOk = map[Manifest.permission.WRITE_CALENDAR] == true
            if (readOk && writeOk) {
                calendarGranted = true
            }
        }

    val batteryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // 返回后重新检查一次
            batteryIgnored = isIgnoringBatteryOptimizations(context)
        }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.permissions_intro_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // 1. 通知权限
        PermissionItemCard(
            title = stringResource(R.string.permissions_notification_title),
            description = stringResource(R.string.permissions_notification_desc),
            granted = notificationGranted,
            onPrimaryAction = {
                if (notificationGranted) return@PermissionItemCard

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // 33 以下无需显式申请，当作已完成
                    notificationGranted = true
                }
            }
        )

        // 2. 日历权限
        PermissionItemCard(
            title = stringResource(R.string.permissions_calendar_title),
            description = stringResource(R.string.permissions_calendar_desc),
            granted = calendarGranted,
            onPrimaryAction = {
                if (calendarGranted) return@PermissionItemCard

                calendarLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                    )
                )
            }
        )

        // 3. 禁止电池优化
        PermissionItemCard(
            title = stringResource(R.string.permissions_battery_title),
            description = stringResource(R.string.permissions_battery_desc),
            granted = batteryIgnored,
            onPrimaryAction = {
                if (batteryIgnored) return@PermissionItemCard

                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${activity.packageName}".toUri()
                }
                batteryLauncher.launch(intent)
            }
        )

        // 4. 权限设置向导（Wiki）
        PermissionItemCard(
            title = stringResource(R.string.permissions_wiki_title),
            description = stringResource(R.string.permissions_wiki_desc),
            granted = wikiVisited,
            onPrimaryAction = {
                val url = GlobalUtils.generateWikiForSpecificDevice()
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    activity.startActivity(intent)
                    wikiVisited = true
                }.getOrElse {
                    // 打不开就当作没完成，不崩溃
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    granted: Boolean,
    onPrimaryAction: () -> Unit,
) {
    val bgColor =
        if (granted)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    val iconTint =
        if (granted)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant

    val iconRes =
        if (granted) R.drawable.ic_ok
        else R.drawable.ic_close

    val shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onPrimaryAction,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                // 左侧 Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // 右上角状态图标（勾/叉）
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = CircleShape,
                    shadowElevation = 0.dp,
                ) {
                    Icon(
                        iconResource(iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==== 工具函数 ====

private fun isNotificationGranted(context: Context): Boolean {
    // Android 13+ 需要检查 POST_NOTIFICATIONS
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PermissionChecker.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun isCalendarGranted(context: Context): Boolean {
    val read = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CALENDAR
    ) == PermissionChecker.PERMISSION_GRANTED

    val write = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_CALENDAR
    ) == PermissionChecker.PERMISSION_GRANTED

    return read && write
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return pm?.isIgnoringBatteryOptimizations(context.packageName) == true
}