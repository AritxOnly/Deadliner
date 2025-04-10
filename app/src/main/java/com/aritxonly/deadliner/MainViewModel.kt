package com.aritxonly.deadliner

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

class MainViewModel(
    private val dbHelper: DatabaseHelper
) : ViewModel() {

    // 用于存储经过筛选、排序后的数据
    private val _ddlList = MutableLiveData<List<DDLItem>>()
    val ddlList: LiveData<List<DDLItem>> = _ddlList

    // 当前筛选的 DeadlineType
    var currentType: DeadlineType = DeadlineType.TASK

    fun isEmpty(): Boolean? = _ddlList.value?.isEmpty()

    private fun filterDataByList(ddlList: List<DDLItem>): List<DDLItem> {
        val filteredList = ddlList.filter { item ->
            Log.d("updateData", "item ${item.id}, " +
                    "name ${item.name}, " +
                    "completeTime ${item.completeTime}," +
                    "isArchived ${item.isArchived}")
            if (item.completeTime.isNotEmpty()) {
                item.isArchived = (!GlobalUtils.filterArchived(item)) || item.isArchived
                dbHelper.updateDDL(item)
                !item.isArchived
            } else {
                true // 如果 completeTime 为空，保留该项目
            }
        }.sortedWith(
            compareBy<DDLItem> { it.isCompleted }
                .thenBy { !it.isStared }
                .thenBy {
                    when (GlobalUtils.filterSelection) {
                        1 -> {  // 按名称
                            it.name
                        }
                        2 -> {  // 按开始时间
                            GlobalUtils.safeParseDateTime(it.startTime)
                        }
                        3 -> {  // 按百分比
                            val startTime = GlobalUtils.safeParseDateTime(it.startTime)
                            val endTime = GlobalUtils.safeParseDateTime(it.endTime)
                            val remainingMinutes =
                                Duration.between(LocalDateTime.now(), endTime).toMinutes().toInt()
                            val fullTime =
                                Duration.between(startTime, endTime).toMinutes().toInt()
                            val progress = remainingMinutes.toFloat() / fullTime.toFloat()
                            progress
                        }
                        else -> {
                            val endTime = GlobalUtils.safeParseDateTime(it.endTime)
                            val remainingMinutes =
                                Duration.between(LocalDateTime.now(), endTime).toMinutes().toInt()
                            remainingMinutes
                        }
                    }
                }
        )
        return filteredList
    }

    /**
     * 加载数据：调用 DatabaseHelper 根据 type 获取数据，
     * 再过滤（比如归档）、排序后更新 LiveData。
     * 如果 showArchived 为 false，则过滤掉已归档数据。
     */
    fun loadData(type: DeadlineType) {
        currentType = type
        viewModelScope.launch(Dispatchers.IO) {
            _ddlList.postValue(filterDataByList(dbHelper.getDDLsByType(type)))
        }
    }

    // 获取所有DDLItem，并根据条件过滤：
    // 1. note或name中必须包含纯文本查询（不区分大小写）
    // 2. 如果提供了时间过滤条件，则要求对应的开始时间或完成时间符合条件
    fun filterData(filter: SearchFilter, type: DeadlineType) {
        viewModelScope.launch(Dispatchers.IO) {
            val filteredList = filterDataByList(dbHelper.getDDLsByType(type)).filter { ddlItem ->
                // 文本匹配：检查 name 和 note 是否包含查询关键字
                val matchesText = ddlItem.name.contains(filter.query, ignoreCase = true) ||
                        ddlItem.note.contains(filter.query, ignoreCase = true)
                if (!matchesText) return@filter false

                // 尝试解析时间，解析失败时认为该条件不满足
                val startTime = try { GlobalUtils.safeParseDateTime(ddlItem.startTime) } catch (e: Exception) { null }
                val completeTime = try { GlobalUtils.safeParseDateTime(ddlItem.completeTime) } catch (e: Exception) { null }

                var timeMatch = true

                filter.year?.let { year ->
                    timeMatch = timeMatch && ((startTime?.year == year) || (completeTime?.year == year))
                }
                filter.month?.let { month ->
                    timeMatch = timeMatch && ((startTime?.monthValue == month) || (completeTime?.monthValue == month))
                }
                filter.day?.let { day ->
                    timeMatch = timeMatch && ((startTime?.dayOfMonth == day) || (completeTime?.dayOfMonth == day))
                }
                filter.hour?.let { hour ->
                    timeMatch = timeMatch && ((startTime?.hour == hour) || (completeTime?.hour == hour))
                }

                matchesText && timeMatch
            }

            _ddlList.postValue(filteredList)
        }
    }

    fun toggleStar(itemId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.getDDLById(itemId)?.let { item ->
                dbHelper.updateDDL(item.copy(isStared = !item.isStared))
                // 刷新当前列表
                loadData(currentType)
            }
        }
    }
}