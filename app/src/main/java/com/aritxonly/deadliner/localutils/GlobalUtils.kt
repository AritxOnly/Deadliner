package com.aritxonly.deadliner.localutils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentManager
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.HabitMetaData
import com.aritxonly.deadliner.model.updateNoteWithDate
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    var vibrationAmplitude: Int
        get() = sharedPreferences.getInt("amplitude", -1)
        set(value) {
            sharedPreferences.edit { putInt("amplitude", value) }
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

    var autoArchiveEnable: Boolean
        get() = sharedPreferences.getBoolean("archive_enable", true)
        set(value) {
            sharedPreferences.edit { putBoolean("archive_enable", value) }
        }

    var firstRun: Boolean
        get() = sharedPreferences.getBoolean("first_run_v2", true)
        set(value) {
            sharedPreferences.edit { putBoolean("first_run_v2", value) }
        }

    var showIntroPage: Boolean
        get() = sharedPreferences.getBoolean("show_intro_page_v3", true)
        set(value) {
            sharedPreferences.edit { putBoolean("show_intro_page_v3", value) }
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

    var filteredCalendars: Set<String?>?
        get() = sharedPreferences.getStringSet("filtered_calendars", null)
        set(value) {
            sharedPreferences.edit { putStringSet("filtered_calendars", value) }
        }

    var customCalendarFilterList: Set<String?>?
        get() = sharedPreferences.getStringSet("custom_filter_list", null)
        set(value) {
            sharedPreferences.edit { putStringSet("custom_filter_list", value) }
        }

    var customCalendarFilterListSelected: Set<String?>?
        get() = sharedPreferences.getStringSet("custom_filter_list_selected", null)
        set(value) {
            sharedPreferences.edit { putStringSet("custom_filter_list_selected", value) }
        }

    var permissionSetupDone: Boolean
        get() = sharedPreferences.getBoolean("permission_setup_done", false)
        set(value) {
            sharedPreferences.edit { putBoolean("permission_setup_done", value) }
        }

    var mdWidgetAddBtn: Boolean
        get() = sharedPreferences.getBoolean("show_add_button_multi_ddl_widget", false)
        set(value) {
            sharedPreferences.edit { putBoolean("show_add_button_multi_ddl_widget", value) }
        }

    var ldWidgetAddBtn: Boolean
        get() = sharedPreferences.getBoolean("show_add_button_large_ddl_widget", true)
        set(value) {
            sharedPreferences.edit { putBoolean("show_add_button_large_ddl_widget", value) }
        }

    var hideDivider: Boolean
        get() = sharedPreferences.getBoolean("hide_divider", false)
        set(value) {
            sharedPreferences.edit { putBoolean("hide_divider", value) }
            hideDividerUi = value
        }

    var hideDividerUi by mutableStateOf(false)
        private set

    var deepSeekEnable: Boolean
        get() = sharedPreferences.getBoolean("deepseek_master", false)
        set(value) {
            sharedPreferences.edit { putBoolean("deepseek_master", value) }
        }

    var customPrompt: String?
        get() = sharedPreferences.getString("custom_prompt", null)
        set(value) {
            sharedPreferences.edit { putString("custom_prompt", value) }
        }

    var embeddedActivities: Boolean
        get() = sharedPreferences.getBoolean("embedded_activities", true)
        set(value) {
            sharedPreferences.edit { putBoolean("embedded_activities", value) }
        }

    var webDavBaseUrl: String
        get() = sharedPreferences.getString("webdav_base", "")?:""
        set(value) {
            sharedPreferences.edit { putString("webdav_base", value) }
        }

    var webDavUser: String
        get() = sharedPreferences.getString("webdav_user", "")?:""
        set(value) {
            sharedPreferences.edit { putString("webdav_user", value) }
        }

    var webDavPass: String
        get() = sharedPreferences.getString("webdav_pass", "")?:""
        set(value) {
            sharedPreferences.edit { putString("webdav_pass", value) }
        }

    var clipboardEnable: Boolean
        get() = sharedPreferences.getBoolean("clipboard", true)
        set(value) {
            sharedPreferences.edit { putBoolean("clipboard", value) }
        }

    object OverviewSettings {
        var monthlyCount: Int
            get() = sharedPreferences.getInt("monthly_count(overview)", 12)
            set(value) {
                sharedPreferences.edit { putInt("monthly_count(overview)", value) }
            }

        var showOverdueInDaily: Boolean
            get() = sharedPreferences.getBoolean("show_overdue_in_daily(overview)", true)
            set(value) {
                sharedPreferences.edit { putBoolean("show_overdue_in_daily(overview)", value) }
            }
    }

    object NotificationStatusManager {
        fun markAsNotified(ddlId: Long) {
            val set = notifiedSet
            set.add(ddlId.toString())
            notifiedSet = set
        }

        fun clearNotified(ddlId: Long) {
            val set = notifiedSet
            if (set.remove(ddlId.toString())) {
                notifiedSet = set
            }
        }

        fun hasBeenNotified(ddlId: Long): Boolean {
            return notifiedSet.contains(ddlId.toString())
        }

        fun clearAllNotified() {
            notifiedSet = mutableSetOf()
        }
    }

    object PendingCode {
        const val RC_DDL_DETAIL = 1000
        const val RC_MARK_COMPLETE = 2000
        const val RC_DELETE = 3000
        const val RC_ALARM_TRIGGER = 4000
        const val RC_ALARM_SHOW = 5000
        const val RC_LATER = 6000
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
        get() = parseDateTime(sharedPreferences.getString("time_null", LocalDateTime.now().toString())?: LocalDateTime.now().toString())!!
        set(value) {
            sharedPreferences.edit().putString("time_null", value.toString()).apply()
        }

    private fun loadSettings() {
        Log.d("GlobalUtils", "Settings loaded from SharedPreferences")
        hideDividerUi = hideDivider
    }

    fun dpToPx(dp: Float, context: Context): Float {
        return dp * context.resources.displayMetrics.density
    }

    fun parseDateTime(dateTimeString: String): LocalDateTime? {
        if (dateTimeString == "null" || dateTimeString == "") return null

        val formatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
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
            if (!autoArchiveEnable) return true
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

    fun generateWikiForSpecificDevice(): String {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
        val brand = Build.BRAND.lowercase(Locale.getDefault())

        val baseWikiUrl = "https://github.com/AritxOnly/Deadliner/wiki/%E9%80%9A%E7%9F%A5%E6%8E%A8%E9%80%81%E5%8A%9F%E8%83%BD%E7%94%A8%E6%88%B7%E6%96%87%E6%A1%A3"

        Log.d("WikiGenerate", manufacturer)

        return baseWikiUrl
    }

    fun generateHabitNote(context: Context, frequency: Int?, total: Int?, type: DeadlineFrequency): String {
        val typeString = when (type) {
            DeadlineFrequency.DAILY -> context.getString(R.string.frequency_daily)
            DeadlineFrequency.WEEKLY -> context.getString(R.string.frequency_weekly)
            DeadlineFrequency.MONTHLY -> context.getString(R.string.frequency_monthly)
            else -> context.getString(R.string.frequency_daily)
        }

        val frequencyValue = frequency ?: 1

        val totalString = if (total == null) {
            context.getString(R.string.habit_total_unlimited)
        } else {
            context.getString(R.string.habit_total_count, total)
        }

        return context.getString(R.string.habit_checkin, typeString, frequencyValue, totalString)
    }

    fun canHabitBeDone(item: DDLItem, metaData: HabitMetaData): Boolean {
        val endTime = parseDateTime(item.endTime)
        if (endTime == null) {
            return true
        }

        val remainingTime = Duration.between(LocalDateTime.now(), endTime).toDays()
        val remainingTasks = metaData.total - item.habitTotalCount
        return when (metaData.frequencyType) {
            DeadlineFrequency.TOTAL ->
                true

            DeadlineFrequency.DAILY ->
                (remainingTime * metaData.frequency >= remainingTasks)

            DeadlineFrequency.WEEKLY ->
                ((remainingTime / 7) * metaData.frequency >= remainingTasks)

            DeadlineFrequency.MONTHLY ->
                ((remainingTime / 30) * metaData.frequency >= remainingTasks)
        }
    }

    fun showRetroactiveDatePicker(fragmentManager: FragmentManager, onDatePicked: (Long) -> Unit) {
        // 限制不选未来日期
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.choose_retro_date_title)
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            onDatePicked(selection)
        }

        picker.show(fragmentManager, "retro_date_picker")
    }

    interface OnWidgetCheckInGlobalListener {
        fun onCheckInFailedGlobal(context: Context, habitItem: DDLItem)
        fun onCheckInSuccessGlobal(context: Context, habitItem: DDLItem, habitMeta: HabitMetaData)
    }

    fun checkInFromWidget(context: Context, habitItem: DDLItem, habitMeta: HabitMetaData, canPerformClick: Boolean, listener: OnWidgetCheckInGlobalListener?) {
        if (!canPerformClick) {
            listener?.onCheckInFailedGlobal(context, habitItem)
            return
        }

        val today = LocalDate.now()
        val updatedNote = updateNoteWithDate(habitItem, today)
        val updatedHabit = habitItem.copy(
            note = updatedNote,
            habitCount = habitItem.habitCount + 1,
            habitTotalCount = habitItem.habitTotalCount + 1
        )

        listener?.onCheckInSuccessGlobal(context, updatedHabit, habitMeta)

        DDLRepository().updateDDL(updatedHabit)
    }

    fun triggerVibration(context: Context, duration: Long = 100) {
        if (!vibration) {
            return
        }

        val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)

        vibrator?.vibrate(
            VibrationEffect.createOneShot(
                duration,
                vibrationAmplitude
            )
        )
    }
}