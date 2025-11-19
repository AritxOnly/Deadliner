package com.aritxonly.deadliner.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aritxonly.deadliner.model.DayOverview
import com.aritxonly.deadliner.model.Habit
import com.aritxonly.deadliner.model.HabitGoalType
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.HabitRecordStatus
import com.aritxonly.deadliner.model.HabitWithDailyStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class HabitViewModel(
    private val habitRepo: HabitRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _weekOverview = MutableStateFlow<List<DayOverview>>(emptyList())
    val weekOverview: StateFlow<List<DayOverview>> = _weekOverview

    // 原始列表（当前日期下，未过滤）
    private val _baseHabitsForSelectedDate =
        MutableStateFlow<List<HabitWithDailyStatus>>(emptyList())

    // UI 实际展示列表（应用搜索过滤后）
    private val _habitsForSelectedDate =
        MutableStateFlow<List<HabitWithDailyStatus>>(emptyList())
    val habitsForSelectedDate: StateFlow<List<HabitWithDailyStatus>> = _habitsForSelectedDate

    init {
        viewModelScope.launch(Dispatchers.IO) {
            refreshForDate(LocalDate.now())
        }
    }

    /**
     * 根据周期返回这一周期的起止日期：
     * - DAILY: [date, date]
     * - WEEKLY: [本周周一, 本周周日]
     * - MONTHLY: [当月第一天, 当月最后一天]
     */
    private fun periodBounds(period: HabitPeriod, date: LocalDate): Pair<LocalDate, LocalDate> {
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

    /**
     * 针对某个日期构造单个 Habit 的状态：
     * WEEKLY / MONTHLY 会在对应周 / 月的窗口内聚合所有记录
     */
    private fun buildStatusForDate(
        habit: Habit,
        date: LocalDate
    ): HabitWithDailyStatus {
        return when (habit.goalType) {
            HabitGoalType.PER_PERIOD -> {
                // 原来的逻辑：按周期窗口统计
                val (start, endInclusive) = periodBounds(habit.period, date)

                val recordsInPeriod = habitRepo
                    .getRecordsForHabitInRange(habit.id, start, endInclusive)
                    .filter { it.status == HabitRecordStatus.COMPLETED }

                val done = recordsInPeriod.sumOf { it.count }
                val target = habit.timesPerPeriod.coerceAtLeast(1)
                val completed = done >= target

                HabitWithDailyStatus(
                    habit = habit,
                    doneCount = done,
                    targetCount = target,
                    isCompleted = completed
                )
            }

            HabitGoalType.TOTAL -> {
                // TOTAL：从“起点”到当前 date 的累计次数
                val recordsUntilToday = habitRepo
                    .getRecordsForHabitInRange(
                        habit.id,
                        LocalDate.of(1970, 1, 1),
                        date
                    )
                    .filter { it.status == HabitRecordStatus.COMPLETED }

                val done = recordsUntilToday.sumOf { it.count }
                val target = habit.totalTarget?.coerceAtLeast(1) ?: done.coerceAtLeast(1)
                val completed = habit.totalTarget?.let { done >= it } ?: false

                HabitWithDailyStatus(
                    habit = habit,
                    doneCount = done,
                    targetCount = target,
                    isCompleted = completed
                )
            }
        }
    }

    /**
     * 核心刷新逻辑：
     * - 所有活跃习惯
     * - 选中日期下的 HabitWithDailyStatus（按周期聚合）
     * - 一整周的 DayOverview（每一天都按周期聚合）
     */
    private fun refreshForDate(date: LocalDate) {
        // 1) 所有活跃习惯
        val allHabits: List<Habit> = habitRepo.getAllHabits()

        // 2) 当前日期的 HabitWithDailyStatus（按周期窗口）
        val baseList: List<HabitWithDailyStatus> = allHabits.map { h ->
            buildStatusForDate(h, date)
        }

        _baseHabitsForSelectedDate.value = baseList
        _habitsForSelectedDate.value = applySearchFilter(baseList, _searchQuery.value)

        // 3) 这一周的 DayOverview
        val startOfWeek = date.with(DayOfWeek.MONDAY)
        val week = (0..6).map { offset ->
            val d = startOfWeek.plusDays(offset.toLong())

            val completedCountForDay = allHabits.count { h ->
                val status = buildStatusForDate(h, d)
                status.isCompleted
            }

            DayOverview(
                date = d,
                completedCount = completedCountForDay,
                totalCount = allHabits.size
            )
        }
        _weekOverview.value = week
    }

    private fun applySearchFilter(
        source: List<HabitWithDailyStatus>,
        query: String
    ): List<HabitWithDailyStatus> {
        if (query.isBlank()) return source
        return source.filter { it.habit.name.contains(query, ignoreCase = true) }
    }

    fun onSelectDate(date: LocalDate) {
        _selectedDate.value = date
        viewModelScope.launch(Dispatchers.IO) {
            refreshForDate(date)
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshForDate(_selectedDate.value)
        }
    }

    fun onToggleHabit(habitId: Long, onCelebrate: (() -> Unit)? = null) {
        val date = _selectedDate.value
        viewModelScope.launch(Dispatchers.IO) {
            // 切换前的完成率
            val before = _baseHabitsForSelectedDate.value
                .find { it.habit.id == habitId }?.let {
                    it.doneCount.toFloat() / it.targetCount.coerceAtLeast(1)
                } ?: 0f

            habitRepo.toggleRecord(habitId, date)
            refreshForDate(date)

            // 切换后的完成率
            val after = _baseHabitsForSelectedDate.value
                .find { it.habit.id == habitId }?.let {
                    it.doneCount.toFloat() / it.targetCount.coerceAtLeast(1)
                } ?: 0f

            if (before < 1f && after >= 1f && onCelebrate != null) {
                withContext(Dispatchers.Main) {
                    onCelebrate()
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        val base = _baseHabitsForSelectedDate.value
        _habitsForSelectedDate.value = applySearchFilter(base, query)
    }
}