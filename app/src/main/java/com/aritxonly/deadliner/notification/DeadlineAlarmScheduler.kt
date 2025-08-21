package com.aritxonly.deadliner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.util.Log
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.GlobalUtils.PendingCode.RC_ALARM_SHOW
import com.aritxonly.deadliner.localutils.GlobalUtils.PendingCode.RC_ALARM_TRIGGER
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlin.math.abs

object DeadlineAlarmScheduler {
    fun scheduleExactAlarm(context: Context, ddl: DDLItem, hours: Long = 12L) {
        if (ddl.type == DeadlineType.HABIT || ddl.isCompleted || ddl.isArchived ||
            GlobalUtils.NotificationStatusManager.hasBeenNotified(ddl.id)) return

        val endTime = GlobalUtils.safeParseDateTime(ddl.endTime)
        val duration = Duration.between(LocalDateTime.now(), endTime).toMinutes()
        if (duration < 0) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!alarmManager.canScheduleExactAlarms()) {
            return
        }

        val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
            putExtra("DDL_ID", ddl.id)
            action = "com.aritxonly.deadliner.ACTION_DDL_ALARM"
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        val requestCode = abs(ddl.id.hashCode())

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RC_ALARM_TRIGGER + requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("OPEN_DDL_ID", ddl.id)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            RC_ALARM_SHOW + requestCode + 1,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 提前720分钟（12小时）触发
        val triggerTime = GlobalUtils.safeParseDateTime(ddl.endTime)
            .minusMinutes(hours * 60L)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        Log.d("AlarmDebug", "设置闹钟时间: ${Date(triggerTime)} | DDL结束时间: ${ddl.endTime}")

        val info = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
        alarmManager.setAlarmClock(info, pendingIntent)

//        alarmManager.setExactAndAllowWhileIdle(
//            AlarmManager.RTC_WAKEUP,
//            triggerTime,
//            pendingIntent
//        )
    }

    fun cancelAllAlarms(context: Context) {
        val allDdls = DatabaseHelper.getInstance(context).getDDLsByType(DeadlineType.TASK)
        val allDdlIds: List<Long> = allDdls.map { it.id }

        for (ddlId in allDdlIds) {
            Log.d("AlarmDebug", "Removing $ddlId")
            cancelAlarm(context, ddlId)
        }

        cancelDailyAlarm(context)
    }

    fun cancelAlarm(context: Context, ddlId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
            action = "com.aritxonly.deadliner.ACTION_DDL_ALARM"
            putExtra("DDL_ID", ddlId)
        }

        val requestCode = abs(ddlId.hashCode())

        PendingIntent.getBroadcast(
            context,
            RC_ALARM_TRIGGER + requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let { pi ->
            alarmManager.cancel(pi)
            pi.cancel()
            Log.d("AlarmDebug", "取消闹钟：DDL_ID: $ddlId")
        }

        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let { pi ->
            alarmManager.cancel(pi)
            pi.cancel()
            Log.d("AlarmDebug", "取消旧版本设定的闹钟：DDL_ID: $ddlId")
        }
    }

    fun scheduleDailyAlarm(context: Context) {
        if (!GlobalUtils.dailyStatsNotification) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val hour = GlobalUtils.dailyNotificationHour
        val minute = GlobalUtils.dailyNotificationMinute

        val now = LocalDateTime.now()
        var nextTrigger = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)
        if (nextTrigger.isBefore(now) || nextTrigger.isEqual(now)) {
            nextTrigger = nextTrigger.plusDays(1)
        }
        val triggerMillis = nextTrigger
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(context, DailyAlarmReceiver::class.java).apply {
            action = "com.aritxonly.deadliner.ACTION_DAILY_ALARM"
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        val magicNumber = 114514    // senpai
        val pi = PendingIntent.getBroadcast(
            context,
            RC_ALARM_TRIGGER + magicNumber,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPi = PendingIntent.getActivity(
            context,
            RC_ALARM_SHOW + magicNumber + 1,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val info = AlarmManager.AlarmClockInfo(triggerMillis, showPi)
        alarmManager.setAlarmClock(info, pi)

//        alarmManager.setExactAndAllowWhileIdle(
//            AlarmManager.RTC_WAKEUP,
//            triggerMillis,
//            pi
//        )

        Log.d("AlarmDebug", "已调度每日通知，每天 ${hour}:${minute}，首次触发：${Date(triggerMillis)}")
    }

    fun cancelDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 构造与 scheduleDailyAlarm 完全一致的 Intent 和 requestCode
        val intent = Intent(context, DailyAlarmReceiver::class.java).apply {
            action = "com.aritxonly.deadliner.ACTION_DAILY_ALARM"
        }
        val magicNumber = 114514

        // 只尝试获取已存在的 PendingIntent，不创建新实例
        PendingIntent.getBroadcast(
            context,
            RC_ALARM_TRIGGER + magicNumber,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let { pi->
            // 取消 AlarmManager 中的定时任务
            alarmManager.cancel(pi)
            // 取消掉 PendingIntent 本身
            pi.cancel()
            Log.d("AlarmDebug", "已取消每日通知闹钟")
        }

        PendingIntent.getBroadcast(
            context,
            magicNumber,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let { pi->
            alarmManager.cancel(pi)
            pi.cancel()
            Log.d("AlarmDebug", "已取消旧版本的每日通知闹钟")
        }
    }
}