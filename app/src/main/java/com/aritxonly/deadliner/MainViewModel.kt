package com.aritxonly.deadliner

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainViewModel(
    private val dbHelper: DatabaseHelper
) : ViewModel() {

    // 用于存储经过筛选、排序后的数据
    private val _ddlList = MutableLiveData<List<DDLItem>>()
    val ddlList: LiveData<List<DDLItem>> = _ddlList

    // 当前筛选的 DeadlineType
    private var currentType: DeadlineType = DeadlineType.TASK

    /**
     * 加载数据：调用 DatabaseHelper 根据 type 获取数据，
     * 再过滤（比如归档）、排序后更新 LiveData。
     * 如果 showArchived 为 false，则过滤掉已归档数据。
     */
    fun loadData(type: DeadlineType, showArchived: Boolean = false) {
        currentType = type
        viewModelScope.launch(Dispatchers.IO) {
            // 根据类型获取数据（此 API 已经支持 type 筛选）
            val rawData = dbHelper.getDDLsByType(type)
            // 过滤归档数据（保留原有异常处理逻辑）
            val filteredData = if (showArchived) rawData else filterArchived(rawData)
            // 根据结束时间排序
            val sortedData = sortItems(filteredData)
            _ddlList.postValue(sortedData)
        }
    }

    /**
     * 根据结束时间排序
     */
    fun sortItems(items: List<DDLItem>) = items.sortedBy { it.endTime }

    /**
     * 过滤归档数据：如果 item 的完成时间距离当前时间超过 autoArchiveDays，则认为需要归档
     */
    private fun filterArchived(items: List<DDLItem>): List<DDLItem> {
        return items.filter { item ->
            GlobalUtils.filterArchived(item)
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