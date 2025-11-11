package com.aritxonly.deadliner.data

import com.aritxonly.deadliner.AppSingletons
import com.aritxonly.deadliner.model.Habit
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.HabitRecord
import com.aritxonly.deadliner.model.HabitRecordStatus
import java.time.LocalDate
import java.time.LocalDateTime

class HabitRepository(
    private val db: DatabaseHelper = AppSingletons.db
) {

    // —— Habit 本体 —— //

    fun createHabitForDdl(
        ddlId: Long,
        name: String,
        period: com.aritxonly.deadliner.model.HabitPeriod,
        timesPerPeriod: Int = 1,
        goalType: com.aritxonly.deadliner.model.HabitGoalType =
            com.aritxonly.deadliner.model.HabitGoalType.PER_PERIOD,
        totalTarget: Int? = null,
        description: String? = null,
        color: Int? = null,
        iconKey: String? = null,
        sortOrder: Int = 0
    ): Long {
        val now = LocalDateTime.now()
        val habit = Habit(
            ddlId = ddlId,
            name = name,
            description = description,
            color = color,
            iconKey = iconKey,
            period = period,
            timesPerPeriod = timesPerPeriod,
            goalType = goalType,
            totalTarget = totalTarget,
            createdAt = now,
            updatedAt = now,
            sortOrder = sortOrder
        )
        return db.insertHabit(habit)
    }

    fun getHabitByDdlId(ddlId: Long): Habit? = db.getHabitByDdlId(ddlId)

    fun getHabitById(id: Long): Habit? = db.getHabitById(id)

    fun getAllHabits(): List<Habit> = db.getAllHabits()

    fun updateHabit(habit: Habit) {
        val updated = habit.copy(updatedAt = LocalDateTime.now())
        db.updateHabit(updated)
    }

    fun deleteHabitByDdlId(ddlId: Long) {
        db.deleteHabitByDdlId(ddlId)
    }

    // —— Habit 打卡记录 —— //

    fun getRecordsForHabitOnDate(habitId: Long, date: LocalDate): List<HabitRecord> =
        db.getHabitRecordsForHabitOnDate(habitId, date)

    fun getRecordsForDate(date: LocalDate): List<HabitRecord> =
        db.getHabitRecordsForDate(date)

    fun getRecordsForHabitInRange(
        habitId: Long,
        startDate: LocalDate,
        endDateInclusive: LocalDate
    ): List<HabitRecord> =
        db.getHabitRecordsForHabitInRange(habitId, startDate, endDateInclusive)

    fun insertRecord(
        habitId: Long,
        date: LocalDate,
        count: Int = 1,
        status: com.aritxonly.deadliner.model.HabitRecordStatus =
            com.aritxonly.deadliner.model.HabitRecordStatus.COMPLETED
    ): Long {
        val record = HabitRecord(
            habitId = habitId,
            date = date,
            count = count,
            status = status,
            createdAt = LocalDateTime.now()
        )
        return db.insertHabitRecord(record)
    }

    fun deleteRecordsForHabitOnDate(habitId: Long, date: LocalDate) {
        db.deleteHabitRecordsForHabitOnDate(habitId, date)
    }

    // 返回某天“有完成记录”的 habitId 集合（只看 COMPLETED）
    fun getCompletedIdsForDate(date: LocalDate): Set<Long> {
        val records = getRecordsForDate(date)
        return records
            .filter { it.status == HabitRecordStatus.COMPLETED }
            .map { it.habitId }
            .toSet()
    }

    private fun periodBounds(period: HabitPeriod, date: LocalDate): Pair<LocalDate, LocalDate> {
        return when (period) {
            HabitPeriod.DAILY -> date to date
            HabitPeriod.WEEKLY -> {
                val start = date.with(java.time.DayOfWeek.MONDAY)
                val end = start.plusDays(6)
                start to end
            }
            HabitPeriod.MONTHLY -> {
                val ym = java.time.YearMonth.from(date)
                val start = ym.atDay(1)
                val end = ym.atEndOfMonth()
                start to end
            }
        }
    }

    /**
     * 切换某天某个习惯的完成状态：
     * - 如果当天已经有 COMPLETED 记录，则删掉这一天所有该习惯记录
     * - 如果没有，则插入一条新的 COMPLETED 记录 count=1
     */
    fun toggleRecord(habitId: Long, date: LocalDate) {
        val habit = getHabitById(habitId) ?: return

        // 该习惯在一个周期内允许的最大次数
        val target = habit.timesPerPeriod.coerceAtLeast(1)

        // 周期边界（天/周/月）
        val (start, endInclusive) = periodBounds(habit.period, date)

        // 周期内所有完成记录
        val recordsInPeriod = getRecordsForHabitInRange(habitId, start, endInclusive)
            .filter { it.status == HabitRecordStatus.COMPLETED }

        val totalInPeriod = recordsInPeriod.sumOf { it.count }

        // 今天的记录
        val todayRecords = recordsInPeriod.filter { it.date == date }
        val todayCount = todayRecords.sumOf { it.count }

        when {
            // 1. 今天已经有记录 → 这次点击 = 取消今天
            todayCount > 0 -> {
                deleteRecordsForHabitOnDate(habitId, date)
            }

            // 2. 今天没有记录，且本周期已经满额 → no-op
            totalInPeriod >= target -> {
                // 什么都不做
            }

            // 3. 今天没有记录，且本周期未满 → 给今天 +1
            else -> {
                insertRecord(
                    habitId = habitId,
                    date = date,
                    count = 1,
                    status = HabitRecordStatus.COMPLETED
                )
            }
        }
    }
}