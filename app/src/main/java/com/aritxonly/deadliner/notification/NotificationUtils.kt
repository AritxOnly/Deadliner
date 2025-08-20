package com.aritxonly.deadliner.notification

import android.Manifest
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.DatabaseHelper
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.LauncherActivity
import com.aritxonly.deadliner.OverviewActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils.PendingCode.RC_DDL_DETAIL
import com.aritxonly.deadliner.localutils.GlobalUtils.PendingCode.RC_DELETE
import com.aritxonly.deadliner.localutils.GlobalUtils.PendingCode.RC_LATER
import com.aritxonly.deadliner.localutils.GlobalUtils.PendingCode.RC_MARK_COMPLETE
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.Duration
import java.time.LocalDateTime

const val CHANNEL_ID = "deadliner_alerts"
const val CHANNEL_DAILY_ID = "deadliner_daily_alerts"
const val XIAOMI_PERMISSION_REQUEST_CODE = 0x1001

object NotificationUtil {

    // 初始化通知渠道（在Application类中调用）
    fun createNotificationChannels(context: Context) {
        // 通用渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Deadline临近通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "用于在Deadline到来前推送通知提醒，需要在设置中打开通知推送功能"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY_ID,
                "Deadliner定时通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "每天定时向用户推送任务完成情况信息，需要在设置中打开通知推送功能"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(dailyChannel)
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
                "Deadliner重要通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "ColorOS专用渠道"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
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

//        // MIUI权限检查
//        if (isXiaomiDevice() && !checkXiaomiPopupPermission(context)) {
//            showXiaomiPermissionDialog(context)
//            return
//        }

        val notification = buildNotification(context, ddl)
        NotificationManagerCompat.from(context).notify(ddl.id.hashCode(), notification)
    }

    fun sendDailyNotification(context: Context) {
        // 检查通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

//        // MIUI权限检查
//        if (isXiaomiDevice() && !checkXiaomiPopupPermission(context)) {
//            showXiaomiPermissionDialog(context)
//            return
//        }

        val notification = buildDailyNotification(context)
        NotificationManagerCompat.from(context).notify(114514, notification)
    }

    private fun buildDailyNotification(context: Context): Notification {
        val dbHelper = DatabaseHelper.getInstance(context)
        val allDdls: List<DDLItem> = dbHelper.getDDLsByType(DeadlineType.TASK)

        val now = LocalDateTime.now()
        var overdueCount = 0
        var inProgressCount = 0
        var dueTodayCount = 0

        for (ddl in allDdls) {
            if (ddl.isCompleted || ddl.isArchived) continue

            val endTime = GlobalUtils.safeParseDateTime(ddl.endTime)

            when {
                endTime.isBefore(now) -> {
                    // 截止时间已过
                    overdueCount++
                }
                endTime.toLocalDate() == now.toLocalDate() -> {
                    // 今天之内到期
                    dueTodayCount++
                }
                else -> {
                    // 还在进行中，且不在今天
                    inProgressCount++
                }
            }
        }

        val title = "今日任务概览" + if (dueTodayCount == 0) "" else "- ${dueTodayCount}个任务今日到期"
        val summary = "🔥逾期 $overdueCount ｜ ⌛️进行中 $inProgressCount"

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_ID).apply {
            setSmallIcon(R.mipmap.ic_launcher)
            setContentTitle(title)
            setContentText(summary)
            setStyle(NotificationCompat.BigTextStyle().bigText(summary))

            // OPPO（ColorOS）额外字段
            if (isOppoDevice()) {
                addExtras(Bundle().apply {
                    putString("oppo_notification_channel_id", "important_channel")
                })
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                114514,
                Intent(context, OverviewActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setContentIntent(pendingIntent)
        }

        return builder.build()
    }

    private fun buildNotification(context: Context, ddl: DDLItem): Notification {
        // 点击打开详情页
        val detailIntent = DeadlineDetailActivity.newIntent(context, ddl).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val detailPending = PendingIntent.getActivity(
            context,
            RC_DDL_DETAIL + ddl.id.hashCode(),
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // “标记完成”广播 PendingIntent
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_COMPLETE
            putExtra(EXTRA_DDL_ID, ddl.id)
        }
        val completePending = PendingIntent.getBroadcast(
            context,
            RC_MARK_COMPLETE + ddl.id.hashCode(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // “删除”广播 PendingIntent
        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DELETE
            putExtra(EXTRA_DDL_ID, ddl.id)
        }
        val deletePending = PendingIntent.getBroadcast(
            context,
            RC_DELETE + ddl.id.hashCode() + 1,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // “稍后提醒”广播 PendingIntent
        val laterIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_LATER
            putExtra(EXTRA_DDL_ID, ddl.id)
        }
        val laterPending = PendingIntent.getBroadcast(
            context,
            RC_LATER + ddl.id.hashCode() + 2,
            laterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setSmallIcon(R.mipmap.ic_launcher)
            setContentTitle("即将到期提醒：${ddl.name}")
            setContentText("${formatRemainingTime(GlobalUtils.safeParseDateTime(ddl.endTime))} - ${ddl.note}")
            setStyle(NotificationCompat.BigTextStyle().bigText(ddl.note))
            priority = NotificationCompat.PRIORITY_MAX
            setCategory(NotificationCompat.CATEGORY_ALARM)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            setContentIntent(detailPending)

            addAction(R.drawable.ic_check, "完成", completePending)
            addAction(R.drawable.ic_delete, "删除", deletePending)
            addAction(R.drawable.ic_close, "稍后提醒", laterPending)

            if (isOppoDevice()) {
                addExtras(Bundle().apply {
                    putString("oppo_notification_channel_id", "important_channel")
                })
            }
        }.build()
    }

    private fun isOppoDevice() =
        Build.MANUFACTURER.equals("oppo", ignoreCase = true)

    private fun formatRemainingTime(endTime: LocalDateTime): String {
        val duration = Duration.between(LocalDateTime.now(), endTime)
        return when {
            duration.toHours() > 0 -> "${duration.toHours()}小时${duration.toMinutes() % 60}分钟"
            duration.toMinutes() > 0 -> "${duration.toMinutes()}分钟"
            else -> "即将到期！"
        }
    }
}