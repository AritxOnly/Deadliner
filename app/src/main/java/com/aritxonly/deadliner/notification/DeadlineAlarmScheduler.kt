package com.aritxonly.deadliner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import com.aritxonly.deadliner.notification.NotificationUtil
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import kotlin.math.abs

object DeadlineAlarmScheduler {
    fun scheduleExactAlarm(context: Context, ddl: DDLItem) {
//        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (ddl.type == DeadlineType.HABIT && ddl.isCompleted) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!alarmManager.canScheduleExactAlarms()) {
            context.startActivity(Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            return
        }

        val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
            putExtra("DDL_ID", ddl.id)
            action = "ACTION_DDL_ALARM_${ddl.id}"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            abs(ddl.id.hashCode()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 提前720分钟（12小时）触发
        val triggerTime = GlobalUtils.safeParseDateTime(ddl.endTime)
            .minusMinutes(720)
            .atZone(ZoneId.systemDefault())
            .toEpochSecond() * 1000

        android.util.Log.d("AlarmDebug", "设置闹钟时间: ${Date(triggerTime)} | DDL结束时间: ${ddl.endTime}")

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
}