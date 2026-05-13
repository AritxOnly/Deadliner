package com.aritxonly.deadliner.data

import android.util.Log
import com.aritxonly.deadliner.AppSingletons
import com.aritxonly.deadliner.model.Habit
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.HabitRecord
import com.aritxonly.deadliner.model.HabitRecordStatus
import com.aritxonly.deadliner.sync.SyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class HabitRepository(
    private val db: DatabaseHelper = AppSingletons.db,
    private val sync: SyncService = AppSingletons.sync,
    private val autoScheduleSync: Boolean = true
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pendingJob: Job? = null

    private fun scheduleSync() {
        if (!autoScheduleSync) return
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(800)
            try { sync.syncOnce() } catch (_: Exception) {}
        }
    }

    private fun utcNow(): LocalDateTime = LocalDateTime.ofInstant(java.time.Instant.now(), java.time.ZoneOffset.UTC)

    internal fun nextContentUpdatedAt(previous: LocalDateTime?): LocalDateTime {
        val now = utcNow()
        val lastSyncTs = parseVersionTs(syncBaseVersion().ts)
        val floor = listOfNotNull(previous?.plusNanos(1), lastSyncTs.plusNanos(1)).maxOrNull()
        return when {
            floor == null -> now
            now.isAfter(floor) -> now
            else -> floor
        }
    }

    private fun syncBaseVersion() = db.getLastLocalVersion()

    private fun parseVersionTs(ts: String): LocalDateTime {
        return runCatching { LocalDateTime.parse(ts) }
            .getOrElse { OffsetDateTime.parse(ts).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime() }
    }

    private fun reservePayloadVersionAndUpdatedAt(ddlId: Long, previous: LocalDateTime?): Pair<com.aritxonly.deadliner.model.Ver, LocalDateTime> {
        val requestedUpdatedAt = nextContentUpdatedAt(previous)
        val ver = db.reserveVersionAtUtc(requestedUpdatedAt)
        val resolvedUpdatedAt = parseVersionTs(ver.ts)
        db.setDdlVersionById(ddlId, ver)
        return ver to resolvedUpdatedAt
    }

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
        val (ver, resolvedUpdatedAt) = reservePayloadVersionAndUpdatedAt(ddlId, null)
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
            createdAt = resolvedUpdatedAt,
            updatedAt = resolvedUpdatedAt,
            sortOrder = sortOrder
        )
        val id = db.insertHabit(habit)
        scheduleSync()
        return id
    }

    fun getHabitByDdlId(ddlId: Long): Habit? = db.getHabitByDdlId(ddlId)

    fun getHabitById(id: Long): Habit? = db.getHabitById(id)

    fun getAllHabits(): List<Habit> {
        val cleaned = db.cleanupOrphanHabits()
        if (cleaned > 0) {
            Log.w("HabitRepository", "Removed $cleaned orphan habit(s) before loading active habits")
        }
        return db.getAllHabits()
    }

    fun updateHabit(habit: Habit) {
        val (_, resolvedUpdatedAt) = reservePayloadVersionAndUpdatedAt(habit.ddlId, habit.updatedAt)
        val updated = habit.copy(updatedAt = resolvedUpdatedAt)
        db.updateHabit(updated)
        scheduleSync()
    }

    fun deleteHabitByDdlId(ddlId: Long) {
        db.deleteHabitByDdlId(ddlId)
        db.bumpDdlVersionById(ddlId)
        scheduleSync()
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
        val habit = getHabitById(habitId)
        val resolvedUpdatedAt = habit?.let {
            reservePayloadVersionAndUpdatedAt(it.ddlId, it.updatedAt).second
        } ?: nextContentUpdatedAt(null)
        val record = HabitRecord(
            habitId = habitId,
            date = date,
            count = count,
            status = status,
            createdAt = resolvedUpdatedAt
        )
        val id = db.insertHabitRecord(record)
        if (habit != null) {
            db.updateHabitUpdatedAt(habitId, resolvedUpdatedAt)
        }
        scheduleSync()
        return id
    }

    fun deleteRecordsForHabitOnDate(habitId: Long, date: LocalDate) {
        val habit = getHabitById(habitId)
        db.deleteHabitRecordsForHabitOnDate(habitId, date)
        if (habit != null) {
            val resolvedUpdatedAt = reservePayloadVersionAndUpdatedAt(habit.ddlId, habit.updatedAt).second
            db.updateHabitUpdatedAt(habitId, resolvedUpdatedAt)
        }
        scheduleSync()
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
        val target = habit.timesPerPeriod.coerceAtLeast(1)

        when (habit.period) {
            HabitPeriod.DAILY -> {
                // —— 每日习惯：次数按“当天”计 —— //
                val recordsToday = getRecordsForHabitOnDate(habitId, date)
                    .filter { it.status == HabitRecordStatus.COMPLETED }

                val currentCount = recordsToday.sumOf { it.count }

                when {
                    // 0 次 → 第一次打卡
                    currentCount <= 0 -> {
                        insertRecord(
                            habitId = habitId,
                            date = date,
                            count = 1,
                            status = HabitRecordStatus.COMPLETED
                        )
                    }

                    // 1..(target-1) 次 → 继续累加
                    currentCount < target -> {
                        insertRecord(
                            habitId = habitId,
                            date = date,
                            count = 1,
                            status = HabitRecordStatus.COMPLETED
                        )
                    }

                    // 已达标 → 再点视为“清空今天”
                    else -> {
                        deleteRecordsForHabitOnDate(habitId, date)
                    }
                }
            }

            HabitPeriod.WEEKLY,
            HabitPeriod.MONTHLY -> {
                // —— 周 / 月习惯：次数按“周期总和”计 —— //
                val (start, endInclusive) = periodBounds(habit.period, date)

                val recordsInPeriod = getRecordsForHabitInRange(habitId, start, endInclusive)
                    .filter { it.status == HabitRecordStatus.COMPLETED }

                val totalInPeriod = recordsInPeriod.sumOf { it.count }

                val recordsToday = recordsInPeriod.filter { it.date == date }
                val todayCount = recordsToday.sumOf { it.count }

                when {
                    // 今天已经有记录 → 再点 = 取消今天（但保留周期内其它天）
                    todayCount > 0 -> {
                        deleteRecordsForHabitOnDate(habitId, date)
                    }

                    // 今天没记录，但周期已经满额 → no-op
                    totalInPeriod >= target -> {
                        // do nothing
                    }

                    // 今天没记录，周期未满 → 今天 +1
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
    }
}
