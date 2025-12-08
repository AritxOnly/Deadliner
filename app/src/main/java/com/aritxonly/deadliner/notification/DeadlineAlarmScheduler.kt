package com.aritxonly.deadliner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.util.Log
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.data.HabitRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.GlobalUtils.PendingCode.RC_ALARM_SHOW
import com.aritxonly.deadliner.localutils.GlobalUtils.PendingCode.RC_ALARM_TRIGGER
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.notification.HabitNotifyReceiver
import com.aritxonly.deadliner.notification.UpcomingLiveUpdatesReceiver
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlin.math.abs

object DeadlineAlarmScheduler {
    fun scheduleExactAlarm(context: Context, ddl: DDLItem, hours: Long = GlobalUtils.deadlineNotificationBefore) {
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
    }

    fun cancelAllAlarms(context: Context) {
        val allDdls = DatabaseHelper.getInstance(context).getDDLsByType(DeadlineType.TASK)
        val allDdlIds: List<Long> = allDdls.map { it.id }

        for (ddlId in allDdlIds) {
            Log.d("AlarmDebug", "Removing $ddlId")
            cancelAlarm(context, ddlId)
            cancelUpcomingDDLAlarm(context, ddlId)
            cancelHabitNotifyAlarm(context, ddlId)
        }

        cancelDailyAlarm(context)
    }

    fun cancelAlarm(context: Context, ddlId: Long) {
        cancelExactAlarm(context, ddlId)
        cancelUpcomingDDLAlarm(context, ddlId)
        cancelHabitNotifyAlarm(context, ddlId)
    }

    fun cancelExactAlarm(context: Context, ddlId: Long) {
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

        if (!alarmManager.canScheduleExactAlarms()) {
            return
        }

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

        Log.d("AlarmDebug", "已调度每日通知，每天 ${hour}:${minute}，首次触发：${Date(triggerMillis)}")
    }

    fun cancelDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!alarmManager.canScheduleExactAlarms()) {
            return
        }

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

    fun scheduleUpcomingDDLAlarm(context: Context, ddl: DDLItem) {
        if (ddl.type == DeadlineType.HABIT || ddl.isCompleted || ddl.isArchived) return

        val remainingTime = calculateRemainingTime(ddl) // 剩余秒数
        if (remainingTime <= 0) return // 过期不设闹钟

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!alarmManager.canScheduleExactAlarms()) {
            return
        }

        val triggerTime = System.currentTimeMillis() +
                ((remainingTime - GlobalUtils.liveUpdatesInAdvance * 60).coerceAtLeast(0)) * 1000L

        // 广播 PendingIntent
        val pendingIntent = createUpcomingDDLPendingIntent(context, ddl)

        // 打开 App 的 PendingIntent（用于锁屏时点击闹钟图标进入）
        val showIntent = Intent(context, MainActivity::class.java)
        val showPi = PendingIntent.getActivity(
            context,
            RC_ALARM_SHOW + ddl.id.hashCode(), // 可换成独立常量或 ddl id
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构造 AlarmClockInfo
        val info = AlarmManager.AlarmClockInfo(triggerTime, showPi)

        // 使用 setAlarmClock（高优先级闹钟，显示在状态栏和锁屏）
        alarmManager.setAlarmClock(info, pendingIntent)

        Log.d("DDLAlarm", "已调度DDL提醒：${ddl.name}，触发时间：${Date(triggerTime)}")
    }

    fun cancelUpcomingDDLAlarm(context: Context, ddlId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!alarmManager.canScheduleExactAlarms()) {
            return
        }

        val intent = Intent(context, UpcomingLiveUpdatesReceiver::class.java).apply {
            action = "com.aritxonly.deadliner.ACTION_UPCOMING_DDL"
            putExtra("DDL_ID", ddlId)
        }

        val requestCode = ddlId.hashCode()

        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let { pi ->
            alarmManager.cancel(pi)
            pi.cancel()
            Log.d("AlarmDebug", "取消Upcoming DDL闹钟：DDL_ID=$ddlId, reqCode=$requestCode")
        } ?: run {
            Log.d("AlarmDebug", "未找到可取消的Upcoming DDL闹钟：DDL_ID=$ddlId, reqCode=$requestCode")
        }
    }

    private const val ACTION_HABIT_NOTIFY = "com.aritxonly.deadliner.ACTION_HABIT_NOTIFY"

    private fun buildHabitAlarmRequestCode(ddlId: Long, habitId: Long): Int {
        return 31 * ddlId.hashCode() + habitId.hashCode()
    }

    fun scheduleHabitNotifyAlarm(context: Context, ddlId: Long) {
        val repo = HabitRepository()
        val habit = repo.getHabitByDdlId(ddlId) ?: run {
            Log.d("HabitAlarm", "schedule: no habit for ddlId=$ddlId")
            return
        }

        val alarmStr = habit.alarmTime?.takeIf { it.isNotBlank() } ?: run {
            Log.d("HabitAlarm", "schedule: alarmTime is null/blank for habit=${habit.id}")
            return
        }

        val localTime = try {
            // 推荐存 LocalTime("HH:mm")；兼容历史 LocalDateTime
            try {
                java.time.LocalTime.parse(alarmStr)
            } catch (e: Exception) {
                java.time.LocalDateTime.parse(alarmStr).toLocalTime()
            }
        } catch (e: Exception) {
            Log.e("HabitAlarm", "schedule: parse alarmTime failed: $alarmStr", e)
            return
        }

        val now = java.time.LocalDateTime.now()
        val today = now.toLocalDate()
        val todayAtAlarm = java.time.LocalDateTime.of(today, localTime)

        // 如果今天这个时间还没到 → 今天；否则 → 明天同一时间
        val nextTriggerDateTime =
            if (todayAtAlarm.isAfter(now)) todayAtAlarm else todayAtAlarm.plusDays(1)

        val triggerMillis = nextTriggerDateTime
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.d("HabitAlarm", "schedule: cannot schedule exact alarms, skip")
            return
        }

        val intent = Intent(context, HabitNotifyReceiver::class.java).apply {
            action = ACTION_HABIT_NOTIFY
            putExtra("DDL_ID", ddlId)
        }
        val requestCode = buildHabitAlarmRequestCode(ddlId, habit.id)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 锁屏图标跳转
        val showIntent = Intent(context, MainActivity::class.java)
        val showPi = PendingIntent.getActivity(
            context,
            RC_ALARM_SHOW + ddlId.hashCode(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val info = AlarmManager.AlarmClockInfo(triggerMillis, showPi)
        alarmManager.setAlarmClock(info, pendingIntent)

        Log.d(
            "HabitAlarm",
            "schedule: habitId=${habit.id}, ddlId=$ddlId, trigger=$nextTriggerDateTime"
        )
    }

    fun cancelHabitNotifyAlarm(context: Context, ddlId: Long) {
        val repo = HabitRepository()
        val habit = repo.getHabitByDdlId(ddlId) ?: run {
            Log.d("AlarmDebug", "cancel: no habit for ddlId=$ddlId")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.d("AlarmDebug", "cancel: cannot schedule exact alarms, skip cancel")
            return
        }

        val intent = Intent(context, HabitNotifyReceiver::class.java).apply {
            action = ACTION_HABIT_NOTIFY
            putExtra("DDL_ID", ddlId)
        }
        val requestCode = buildHabitAlarmRequestCode(ddlId, habit.id)

        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let { pi ->
            alarmManager.cancel(pi)
            pi.cancel()
            Log.d("AlarmDebug", "取消Habit闹钟：DDL_ID=$ddlId, reqCode=$requestCode")
        } ?: run {
            Log.d("AlarmDebug", "未找到可取消的Habit闹钟：DDL_ID=$ddlId, reqCode=$requestCode")
        }
    }

    internal fun calculateRemainingTime(ddl: DDLItem): Long {
        val now = LocalDateTime.now()
        val endTime = GlobalUtils.safeParseDateTime(ddl.endTime)
        val duration = Duration.between(now, endTime)
        return duration.toSeconds() // 返回剩余时间，单位为秒
    }

    private fun createUpcomingDDLPendingIntent(context: Context, ddl: DDLItem): PendingIntent {
        val intent = Intent(context, UpcomingLiveUpdatesReceiver::class.java).apply {
            action = "com.aritxonly.deadliner.ACTION_UPCOMING_DDL"
            putExtra("DDL_ID", ddl.id)
        }
        return PendingIntent.getBroadcast(
            context,
            ddl.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}