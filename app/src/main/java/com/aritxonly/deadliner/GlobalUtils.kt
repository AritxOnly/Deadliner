package com.aritxonly.deadliner

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import androidx.core.content.edit
import com.aritxonly.deadliner.model.CalendarEvent
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.HabitMetaData
import java.time.Instant

object GlobalUtils {

    private const val PREF_NAME = "app_settings"

    private lateinit var sharedPreferences: SharedPreferences

    class NotificationBefore {
        companion object {
            const val ONE_DAY = 0b100
            const val HALF_DAY = 0b10
            const val TWO_HOURS = 0b1
        }
    }

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        loadSettings()  // 初始化时加载设置
    }

    var vibration: Boolean
        get() = sharedPreferences.getBoolean("vibration", true)
        set(value) {
            sharedPreferences.edit { putBoolean("vibration", value) }
        }

    var progressDir: Boolean
        get() = sharedPreferences.getBoolean("main_progress_dir", false)
        set(value) {
            sharedPreferences.edit { putBoolean("main_progress_dir", value) }
        }

    var progressWidget: Boolean
        get() = sharedPreferences.getBoolean("widget_progress_dir", false)
        set(value) {
            sharedPreferences.edit { putBoolean("widget_progress_dir", value) }
        }

    var deadlineNotification: Boolean
        get() = sharedPreferences.getBoolean("deadline_notification", false)
        set(value) {
            sharedPreferences.edit { putBoolean("deadline_notification", value) }
        }

    var dailyStatsNotification: Boolean
        get() = sharedPreferences.getBoolean("daily_stats_notification", false)
        set(value) {
            sharedPreferences.edit { putBoolean("daily_stats_notification", value) }
        }

    var dailyNotificationHour: Int
        get() = sharedPreferences.getInt("daily_notification_hour", 9)
        set(value) {
            sharedPreferences.edit { putInt("daily_notification_hour", value) }
        }
    var dailyNotificationMinute: Int
        get() = sharedPreferences.getInt("daily_notification_minute", 0)
        set(value) {
            sharedPreferences.edit { putInt("daily_notification_minute", value) }
        }

    var motivationalQuotes: Boolean
        get() = sharedPreferences.getBoolean("motivational_quotes", true)
        set(value) {
            sharedPreferences.edit { putBoolean("motivational_quotes", value) }
        }

    var fireworksOnFinish: Boolean
        get() = sharedPreferences.getBoolean("fireworks_anim", true)
        set(value) {
            sharedPreferences.edit { putBoolean("fireworks_anim", value) }
        }

    var autoArchiveTime: Int
        get() = sharedPreferences.getInt("archive_time", 7)
        set(value) {
            sharedPreferences.edit { putInt("archive_time", value) }
        }

    var firstRun: Boolean
        get() = sharedPreferences.getBoolean("first_run_v2", true)
        set(value) {
            sharedPreferences.edit { putBoolean("first_run_v2", value) }
        }

    var showIntroPage: Boolean
        get() = sharedPreferences.getBoolean("show_intro_page", true)
        set(value) {
            sharedPreferences.edit { putBoolean("show_intro_page", value) }
        }

    var detailDisplayMode: Boolean
        get() = sharedPreferences.getBoolean("detail_display_mode", true)
        set(value) {
            sharedPreferences.edit { putBoolean("detail_display_mode", value) }
        }

    var nearbyTasksBadge: Boolean
        get() = sharedPreferences.getBoolean("nearby_tasks_badge", true)
        set(value) {
            sharedPreferences.edit { putBoolean("nearby_tasks_badge", value) }
        }

    var nearbyDetailedBadge: Boolean
        get() = sharedPreferences.getBoolean("nearby_detailed_badge", false)
        set(value) {
            sharedPreferences.edit { putBoolean("nearby_detailed_badge", value) }
        }

    var notificationBefore: Int
        get() = sharedPreferences.getInt("notification_before", 0b111)
        set(value) {
            sharedPreferences.edit { putInt("notification_before", value) }
        }

    private var notifiedSet: MutableSet<String>
        get() = sharedPreferences.getStringSet("notified_set", emptySet())?.toMutableSet()?: mutableSetOf()
        set(value) {
            sharedPreferences.edit { putStringSet("notified_set", value.toSet()) }
        }

    var developerMode: Boolean
        get() = sharedPreferences.getBoolean("developer_mode", false)
        set(value) {
            sharedPreferences.edit { putBoolean("developer_mode", value) }
        }

    var dynamicColors: Boolean
        get() = sharedPreferences.getBoolean("dynamic_colors", true)
        set(value) {
            sharedPreferences.edit { putBoolean("dynamic_colors", value) }
        }

//    var customColorScheme:

    var hideFromRecent: Boolean
        get() = sharedPreferences.getBoolean("hide_from_recent", false)
        set(value) {
            sharedPreferences.edit { putBoolean("hide_from_recent", value) }
        }

    var cloudSyncEnable: Boolean
        get() = sharedPreferences.getBoolean("cloud_sync_enable", false)
        set(value) {
            sharedPreferences.edit { putBoolean("cloud_sync_enable", value) }
        }

    var cloudSyncServer: String?
        get() = sharedPreferences.getString("cloud_sync_server", null)
        set(value) {
            sharedPreferences.edit { putString("cloud_sync_server", value) }
        }

    var cloudSyncPort: Int
        get() = sharedPreferences.getInt("cloud_sync_port", 5000)
        set(value) {
            sharedPreferences.edit { putInt("cloud_sync_port", value) }
        }

    var cloudSyncConstantToken: String?
        get() = sharedPreferences.getString("cloud_sync_constant_token", null)
        set(value) {
            sharedPreferences.edit { putString("cloud_sync_constant_token", value) }
        }

    var experimentalEdgeToEdge: Boolean
        get() = sharedPreferences.getBoolean("enable_edge_to_edge", true)
        set(value) {
            sharedPreferences.edit { putBoolean("enable_edge_to_edge", value) }
        }

    object NotificationStatusManager {
        fun markAsNotified(ddlId: Long) {
            val set = notifiedSet
            set.add(ddlId.toString())
            notifiedSet = set
        }

        fun hasBeenNotified(ddlId: Long): Boolean {
            return notifiedSet.contains(ddlId.toString())
        }

        fun clearAllNotified() {
            notifiedSet = mutableSetOf()
        }
    }

    // v2.0 - filter功能
    /**
     * 映射表
     * 0 - 默认（按剩余时间）
     * 1 - 按名称
     * 2 - 按开始时间
     * 3 - 按百分比(进度)
     */
    var filterSelection: Int
        get() = sharedPreferences.getInt("filter_selection", 0)
        set(value) {
            sharedPreferences.edit().putInt("filter_selection", value).apply()
        }

    // null pointer对应的safe解析时间：第一次启动时间
    var timeNull: LocalDateTime
        get() = parseDateTime(sharedPreferences.getString("time_null", LocalDateTime.now().toString())?:LocalDateTime.now().toString())!!
        set(value) {
            sharedPreferences.edit().putString("time_null", value.toString()).apply()
        }

    private fun loadSettings() {
        Log.d("GlobalUtils", "Settings loaded from SharedPreferences")
    }

    fun dpToPx(dp: Float, context: Context): Float {
        return dp * context.resources.displayMetrics.density
    }

    fun parseDateTime(dateTimeString: String): LocalDateTime? {
        if (dateTimeString == "null") return null

        val formatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        )

        for (formatter in formatters) {
            try {
                return LocalDateTime.parse(dateTimeString, formatter)
            } catch (e: Exception) {
                // 尝试下一个格式
            }
        }
        throw IllegalArgumentException("Invalid date format: $dateTimeString")
    }

    fun safeParseDateTime(dateTimeString: String): LocalDateTime {
        return parseDateTime(dateTimeString)?: timeNull
    }

    fun filterArchived(item: DDLItem): Boolean {
        try {
            val completeTime = safeParseDateTime(item.completeTime)
            val daysSinceCompletion = Duration.between(completeTime, LocalDateTime.now()).toDays()
            return daysSinceCompletion <= autoArchiveTime
        } catch (e: Exception) {
            return true // 如果解析失败，默认保留
        }
    }

    /**
     * 显示日期和时间选择器
     */
    fun showDateTimePicker(fragmentManager: FragmentManager, onDateTimeSelected: (LocalDateTime) -> Unit ) {
        val calendar = Calendar.getInstance()

        // 创建日期选择器
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())  // 设置默认日期
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            // 获取选择的日期（毫秒）
            val selectedDateTime = LocalDateTime.ofInstant(
                Date(selectedDate).toInstant(), ZoneId.systemDefault())

            // 显示时间选择器
            showTimePicker(fragmentManager, selectedDateTime, onDateTimeSelected)
        }

        // 显示日期选择器
        datePicker.show(fragmentManager, datePicker.toString())
    }

    /**
     * 显示时间选择器
     */
    private fun showTimePicker(fragmentManager: FragmentManager, selectedDateTime: LocalDateTime, onDateTimeSelected: (LocalDateTime) -> Unit ) {
        val currentTime = LocalDateTime.now()

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentTime.hour) // 设置当前时间的小时
            .setMinute(currentTime.minute) // 设置当前时间的分钟
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)  // 设置默认为dial模式
            .build()

        timePicker.addOnPositiveButtonClickListener {
            // 获取选择的时间
            val hourOfDay = timePicker.hour
            val minute = timePicker.minute

            // 创建最终的 LocalDateTime
            val finalDateTime = selectedDateTime.withHour(hourOfDay).withMinute(minute)

            // 回调选中的日期时间
            onDateTimeSelected(finalDateTime)
        }

        // 显示时间选择器
        timePicker.show(fragmentManager, timePicker.toString())
    }

    /**
     * v2.0新增
     * 过滤功能相关API
     */

    fun parseHabitMetaData(note: String): HabitMetaData {
        val gson = Gson()
        val type = object : TypeToken<HabitMetaData>() {}.type
        val habitMeta: HabitMetaData = try {
            gson.fromJson(note, type)
                ?: HabitMetaData(
                    emptySet(),
                    DeadlineFrequency.DAILY,
                    1,
                    0,
                    LocalDate.now().toString()
                )
        } catch (e: Exception) {
            HabitMetaData(emptySet(), DeadlineFrequency.DAILY, 1, 0, LocalDate.now().toString())
        }

        return habitMeta
    }

    fun setAlarms(databaseHelper: DatabaseHelper, context: Context) {
        if (!deadlineNotification) {
            return
        }

        val pendingTasks = databaseHelper.getDDLsByType(DeadlineType.TASK)
            .filter {
                if (it.isCompleted || it.isArchived) {
                    return@filter false
                }

                val endTime = parseDateTime(it.endTime)
                endTime != null && endTime.isAfter(LocalDateTime.now())
            }

        pendingTasks.forEach { ddlItem ->
            DeadlineAlarmScheduler.scheduleExactAlarm(context, ddlItem)
        }
    }

    fun decideHideFromRecent(context: Context, activity: Activity) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val myTaskId = activity.taskId
        activityManager.appTasks
            .firstOrNull { it.taskInfo?.id == myTaskId }
            ?.setExcludeFromRecents(hideFromRecent)
    }

    fun Long.toDateTimeString(): String {
        val zonedDateTime = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
        return zonedDateTime.toLocalDateTime().toString()
    }
}