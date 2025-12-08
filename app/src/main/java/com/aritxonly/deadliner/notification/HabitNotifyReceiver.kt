package com.aritxonly.deadliner.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.data.HabitRepository
import com.aritxonly.deadliner.localutils.GlobalUtils.PendingCode.RC_ALARM_SHOW
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.Habit
import com.aritxonly.deadliner.model.HabitGoalType
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.HabitRecordStatus
import com.aritxonly.deadliner.notification.NotificationUtil.sendHabitNotification
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private const val ACTION_HABIT_NOTIFY = "com.aritxonly.deadliner.ACTION_HABIT_NOTIFY"

class HabitNotifyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != ACTION_HABIT_NOTIFY) return

        val ddlId = intent.getLongExtra("DDL_ID", -1L)
        if (ddlId == -1L) {
            Log.e("HabitAlarm", "onReceive: invalid DDL_ID")
            return
        }

        val db = DatabaseHelper.getInstance(context)
        val ddl = db.getDDLById(ddlId) ?: run {
            Log.d("HabitAlarm", "onReceive: no DDL for id=$ddlId")
            return
        }
        if (ddl.type != DeadlineType.HABIT) {
            Log.d("HabitAlarm", "onReceive: ddlId=$ddlId is not HABIT, ignore")
            return
        }

        val habitRepo = HabitRepository(db)
        val habit = habitRepo.getHabitByDdlId(ddlId) ?: run {
            Log.d("HabitAlarm", "onReceive: no Habit for ddlId=$ddlId")
            return
        }

        val today = LocalDate.now()

        if (habit.goalType == HabitGoalType.TOTAL) {
            val lifetimeDone = getLifetimeDoneCount(habitRepo, habit, today)
            val target = habit.totalTarget
            if (target != null && lifetimeDone >= target) {
                // 已经达成总目标，取消闹钟，不再重排
                DeadlineAlarmScheduler.cancelHabitNotifyAlarm(context, ddlId)
                Log.d(
                    "HabitAlarm",
                    "onReceive: habitId=${habit.id} lifetime done($lifetimeDone/$target), cancel alarm"
                )
                return
            }
        }

        // 2. 判断“这个周期是否已经完成了今天/本周/本月的目标”
        val shouldNotify = shouldNotifyToday(habitRepo, habit, today)

        // 3. 不论今天是否完成，本次触发之后都先重排下一次（今天的时间点已经过了 → 会排到明天）
        DeadlineAlarmScheduler.scheduleHabitNotifyAlarm(context, ddlId)

        // 4. 只有“本周期还没做完”的情况下才发通知
        if (shouldNotify) {
            Log.d("HabitAlarm", "onReceive: send habit notification for habitId=${habit.id}")
            sendHabitNotification(context, habit, ddl)
        } else {
            Log.d("HabitAlarm", "onReceive: habitId=${habit.id} already completed for period, no notify")
        }
    }

    /**
     * 当前周期是否需要提醒：
     * - PER_PERIOD:
     *   DAILY  : 今天完成次数 < timesPerPeriod
     *   WEEKLY : 本周内完成次数 < timesPerPeriod
     *   MONTHLY: 本月内完成次数 < timesPerPeriod
     *
     * - TOTAL:
     *   如果 lifetime 尚未到 totalTarget：
     *     今天还没有 COMPLETED 记录 → 需要提醒
     *     今天已经打过 → 不提醒
     */
    private fun shouldNotifyToday(
        repo: HabitRepository,
        habit: Habit,
        today: LocalDate
    ): Boolean {
        return when (habit.goalType) {
            HabitGoalType.PER_PERIOD -> {
                val (start, endInclusive) = periodBounds(habit.period, today)
                val recordsInPeriod = repo
                    .getRecordsForHabitInRange(habit.id, start, endInclusive)
                    .filter { it.status == HabitRecordStatus.COMPLETED }

                val done = recordsInPeriod.sumOf { it.count }
                val target = habit.timesPerPeriod.coerceAtLeast(1)
                done < target
            }

            HabitGoalType.TOTAL -> {
                val allRecordsUpToToday = repo
                    .getRecordsForHabitInRange(
                        habit.id,
                        LocalDate.of(1970, 1, 1),
                        today
                    )
                    .filter { it.status == HabitRecordStatus.COMPLETED }

                val totalDone = allRecordsUpToToday.sumOf { it.count }
                val target = habit.totalTarget
                if (target != null && totalDone >= target) {
                    false
                } else {
                    val todayDone = allRecordsUpToToday
                        .filter { it.date == today }
                        .sumOf { it.count }
                    todayDone <= 0
                }
            }
        }
    }

    /**
     * 计算 lifetime 已完成次数（仅 TOTAL 模式使用）
     */
    private fun getLifetimeDoneCount(
        repo: HabitRepository,
        habit: Habit,
        today: LocalDate
    ): Int {
        val allRecords = repo
            .getRecordsForHabitInRange(
                habit.id,
                LocalDate.of(1970, 1, 1),
                today
            )
            .filter { it.status == HabitRecordStatus.COMPLETED }

        return allRecords.sumOf { it.count }
    }

    /**
     * 周期边界：用于 PER_PERIOD 模式下统计当前周期内的完成次数
     */
    private fun periodBounds(
        period: HabitPeriod,
        date: LocalDate
    ): Pair<LocalDate, LocalDate> {
        return when (period) {
            HabitPeriod.DAILY -> date to date
            HabitPeriod.WEEKLY -> {
                val start = date.with(DayOfWeek.MONDAY)
                val end = start.plusDays(6)
                start to end
            }
            HabitPeriod.MONTHLY -> {
                val ym = YearMonth.from(date)
                val start = ym.atDay(1)
                val end = ym.atEndOfMonth()
                start to end
            }
        }
    }
}