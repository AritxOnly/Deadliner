package com.aritxonly.deadliner.data

import com.aritxonly.deadliner.AppSingletons
import com.aritxonly.deadliner.model.Habit
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

    /**
     * 切换某天某个习惯的完成状态：
     * - 如果当天已经有 COMPLETED 记录，则删掉这一天所有该习惯记录
     * - 如果没有，则插入一条新的 COMPLETED 记录 count=1
     */
    fun toggleRecord(habitId: Long, date: LocalDate) {
        // 1. 找到 Habit，拿到每天的 target 次数
        val habit = getHabitById(habitId) ?: return
        val targetPerDay = habit.timesPerPeriod.coerceAtLeast(1)

        // 2. 查当天所有记录，累计 COMPLETED 的 count
        val records = getRecordsForHabitOnDate(habitId, date)
        val currentCount = records
            .filter { it.status == HabitRecordStatus.COMPLETED }
            .sumOf { it.count }

        when {
            // 0 次 → 第一次打卡：插入 count=1
            currentCount <= 0 -> {
                insertRecord(
                    habitId = habitId,
                    date = date,
                    count = 1,
                    status = HabitRecordStatus.COMPLETED
                )
            }

            // 1..(N-1) 次 → 继续累加：再插一条 count=1
            currentCount < targetPerDay -> {
                insertRecord(
                    habitId = habitId,
                    date = date,
                    count = 1,
                    status = HabitRecordStatus.COMPLETED
                )
            }

            // 已经达到或超过 N 次 → 下一次点击视为“清空今天”
            else -> {
                deleteRecordsForHabitOnDate(habitId, date)
            }
        }
    }
}