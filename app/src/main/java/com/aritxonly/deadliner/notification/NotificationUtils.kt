package com.aritxonly.deadliner.notification

import android.Manifest
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aritxonly.deadliner.DDLItem
import com.aritxonly.deadliner.GlobalUtils
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.Duration
import java.time.LocalDateTime

const val CHANNEL_ID = "deadliner_alerts"
const val XIAOMI_PERMISSION_REQUEST_CODE = 0x1001

object NotificationUtil {

    // 初始化通知渠道（在Application类中调用）
    fun createNotificationChannels(context: Context) {
        // 通用渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Deadline Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical deadline notifications"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        // ColorOS专用渠道
        if (isOppoDevice()) {
            createColorOSChannel(context)
        }
    }

    private fun createColorOSChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "important_channel",
                "Critical Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "ColorOS important notifications"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun sendImmediateNotification(context: Context, ddl: DDLItem) {
        // 检查通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // MIUI权限检查
        if (isXiaomiDevice() && !checkXiaomiPopupPermission(context)) {
            showXiaomiPermissionDialog(context)
            return
        }

        val notification = buildNotification(context, ddl)
        NotificationManagerCompat.from(context).notify(ddl.id.hashCode(), notification)
    }

    private fun buildNotification(context: Context, ddl: DDLItem): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setSmallIcon(R.mipmap.ic_launcher) // 确保资源存在
            setContentTitle("DDL警报：${ddl.name}")
            setContentText("剩余时间：${formatRemainingTime(GlobalUtils.safeParseDateTime(ddl.endTime))}")
            priority = NotificationCompat.PRIORITY_MAX
            setCategory(NotificationCompat.CATEGORY_ALARM)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setAutoCancel(true)

            // 点击打开应用
            val pendingIntent = PendingIntent.getActivity(
                context,
                ddl.id.hashCode(),
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setContentIntent(pendingIntent)

            // ColorOS适配
            if (isOppoDevice()) {
                addExtras(Bundle().apply {
                    putString("oppo_notification_channel_id", "important_channel")
                })
            }
        }.build()
    }

    // region 厂商适配工具
    private fun isXiaomiDevice() =
        Build.MANUFACTURER.equals("xiaomi", ignoreCase = true)

    private fun isOppoDevice() =
        Build.MANUFACTURER.equals("oppo", ignoreCase = true)

    private fun checkXiaomiPopupPermission(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                "miui_permission_controller_clazz"
            ) == 1
        } catch (e: Exception) {
            false
        }
    }

    private fun showXiaomiPermissionDialog(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle("需要后台弹窗权限")
            .setMessage("请在设置中允许Deadliner显示后台弹窗")
            .setPositiveButton("去设置") { _, _ ->
                try {
                    val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                        putExtra("extra_pkgname", context.packageName)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    })
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    // endregion

    // 剩余时间格式化（示例实现）
    private fun formatRemainingTime(endTime: LocalDateTime): String {
        val duration = Duration.between(LocalDateTime.now(), endTime)
        return when {
            duration.toHours() > 0 -> "${duration.toHours()}小时${duration.toMinutes() % 60}分钟"
            duration.toMinutes() > 0 -> "${duration.toMinutes()}分钟"
            else -> "即将到期！"
        }
    }
}