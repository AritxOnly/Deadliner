package com.aritxonly.deadliner.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aritxonly.deadliner.model.DayOverview
import com.aritxonly.deadliner.model.Habit
import com.aritxonly.deadliner.model.HabitGoalType
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.HabitRecordStatus
import com.aritxonly.deadliner.model.HabitStatus
import com.aritxonly.deadliner.model.HabitWithDailyStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

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
        // 启动时加载今天的数据
        viewModelScope.launch(Dispatchers.IO) {
            refreshForDate(LocalDate.now())
        }
    }

    /**
     * 核心：从 repo 读取
     * - 所有活跃习惯
     * - 当前日期的完成记录
     * 然后推导：
     * - 当前日期的 HabitWithDailyStatus 列表
     * - 整周的 DayOverview 列表
     */
    private fun refreshForDate(date: LocalDate) {
        // 1) 所有活跃习惯
        val allHabits: List<Habit> = habitRepo.getAllHabits()

        // 2) 当前日期的所有记录
        val recordsToday = habitRepo.getRecordsForDate(date)

        // 把今天完成情况按 habitId 聚合一下
        val doneCountByHabit: Map<Long, Int> =
            recordsToday
                .filter { it.status == HabitRecordStatus.COMPLETED }
                .groupBy { it.habitId }
                .mapValues { (_, list) -> list.sumOf { it.count } }

        // 3) 生成当前日期的 HabitWithDailyStatus 列表
        val baseList: List<HabitWithDailyStatus> = allHabits.map { h ->
            val doneCount = doneCountByHabit[h.id] ?: 0
            val target = if (h.timesPerPeriod > 0) h.timesPerPeriod else 1
            HabitWithDailyStatus(
                habit = h,
                doneCount = doneCount,
                targetCount = target,
                isCompleted = doneCount >= target
            )
        }

        _baseHabitsForSelectedDate.value = baseList
        // 应用当前搜索过滤
        _habitsForSelectedDate.value = applySearchFilter(baseList, _searchQuery.value)

        // 4) 生成这一周的 DayOverview
        val startOfWeek = date.with(DayOfWeek.MONDAY)
        val week = (0..6).map { offset ->
            val d = startOfWeek.plusDays(offset.toLong())
            val records = habitRepo.getRecordsForDate(d)
                .filter { it.status == HabitRecordStatus.COMPLETED }
            val doneByDay = records
                .groupBy { it.habitId }
                .mapValues { (_, list) -> list.sumOf { it.count } }

            val completedCountForDay = allHabits.count { h ->
                val done = doneByDay[h.id] ?: 0
                val target = if (h.timesPerPeriod > 0) h.timesPerPeriod else 1
                done >= target
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
        viewModelScope.launch { refreshForDate(_selectedDate.value) }
    }

    fun onToggleHabit(habitId: Long) {
        val date = _selectedDate.value
        viewModelScope.launch(Dispatchers.IO) {
            habitRepo.toggleRecord(habitId, date)
            refreshForDate(date)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        val base = _baseHabitsForSelectedDate.value
        _habitsForSelectedDate.value = applySearchFilter(base, query)
    }
}