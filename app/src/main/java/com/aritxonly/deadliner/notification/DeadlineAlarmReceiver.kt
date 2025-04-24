package com.aritxonly.deadliner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aritxonly.deadliner.notification.KeepAliveService
import com.aritxonly.deadliner.notification.NotificationUtil.sendImmediateNotification
import java.time.Duration
import java.time.LocalDateTime

// DeadlineAlarmReceiver.kt
class DeadlineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ddlId = intent.getIntExtra("DDL_ID", -1)
        val ddl = DatabaseHelper.getInstance(context).getDDLById(ddlId.toLong()) ?: return

        // 启动保活前台服务（提前10分钟）
        if (shouldStartForegroundService(ddl)) {
            val serviceIntent = Intent(context, KeepAliveService::class.java).apply {
                putExtra("DDL_ID", ddlId)
                putExtra("TRIGGER_TIME", ddl.endTime)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            sendImmediateNotification(context, ddl)
        }
    }

    private fun shouldStartForegroundService(ddl: DDLItem): Boolean {
        val remaining = Duration.between(LocalDateTime.now(), GlobalUtils.safeParseDateTime(ddl.endTime))
        return remaining.toMinutes() > 10 // 提前10分钟启动保活
    }
}