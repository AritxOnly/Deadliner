package com.aritxonly.deadliner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import java.time.ZoneOffset

object DeadlineAlarmScheduler {
    fun scheduleExactAlarm(context: Context, ddl: DDLItem) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
            putExtra("DDL_ID", ddl.id)
            action = "ACTION_DDL_ALARM_${ddl.id}" // 唯一action
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ddl.id.hashCode(), // 唯一requestCode
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 使用精确闹钟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                context.startActivity(Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            GlobalUtils.safeParseDateTime(ddl.endTime).toEpochSecond(ZoneOffset.UTC) * 1000,
            pendingIntent
        )
    }
}