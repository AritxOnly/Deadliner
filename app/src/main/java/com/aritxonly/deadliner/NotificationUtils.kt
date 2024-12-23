package com.aritxonly.deadliner

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

fun sendDeadlineNotification(context: Context, ddlName: String) {
    val channelId = "ddl_reminder_channel"

    // 创建通知渠道（适用于 Android 8.0+）
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "DDL Reminder Notifications"
        val descriptionText = "通知您即将到期的DDL"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // 构建通知
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_alert) // 替换为你的图标
        .setContentTitle("DDL 即将到期！")
        .setContentText("$ddlName 还有不到 1 小时到期！")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    // 检查通知权限
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，记录日志或进行其他处理（例如提示用户开启通知权限）
            return
        }
    }

    // 显示通知
    with(NotificationManagerCompat.from(context)) {
        notify(System.currentTimeMillis().toInt(), builder.build())
    }
}