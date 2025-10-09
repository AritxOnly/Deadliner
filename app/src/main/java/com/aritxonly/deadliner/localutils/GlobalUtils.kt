package com.aritxonly.deadliner.localutils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
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
import com.aritxonly.deadliner.model.UiStyle
import com.aritxonly.deadliner.model.updateNoteWithDate
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object GlobalUtils {

    private const val PREF_NAME = "app_settings"

    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        loadSettings(context)  // 初始化时加载设置
    }

    fun getDeadlinerAIConfig(): DeadlinerAIConfig {
        return DeadlinerAIConfig(sharedPreferences)
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

    var deadlineNotificationBefore: Long
        get() = sharedPreferences.getLong("deadline_notify_before", 12L)
        set(value) {
            sharedPreferences.edit { putLong("deadline_notify_before", value) }
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

    @Deprecated("Update to SDK 35; Edge to edge is forced to enable.")
    var experimentalEdgeToEdge: Boolean = true
        get() = true
        private set

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

    var deadlinerAIEnable: Boolean
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

    var splitPlaceholderEnable: Boolean
         get() = sharedPreferences.getBoolean("split_placeholder", true)
         set(value) {
             sharedPreferences.edit { putBoolean("split_placeholder", value) }
         }

    var dynamicSplit: Boolean
        get() = sharedPreferences.getBoolean("dynamic_split", false)
        set(value) {
            sharedPreferences.edit { putBoolean("dynamic_split", value) }
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

    var syncIntervalMinutes: Int
        get() = sharedPreferences.getInt("sync_interval", 0)
        set(value) {
            sharedPreferences.edit { putInt("sync_interval", value) }
        }

    var syncWifiOnly: Boolean
        get() = sharedPreferences.getBoolean("sync_wifi_only", false)
        set(value) {
            sharedPreferences.edit { putBoolean("sync_wifi_only", value) }
        }

    var syncChargingOnly: Boolean
        get() = sharedPreferences.getBoolean("sync_charging_only", false)
        set(value) {
            sharedPreferences.edit { putBoolean("sync_charging_only", value) }
        }

    var clipboardEnable: Boolean
        get() = sharedPreferences.getBoolean("clipboard", true)
        set(value) {
            sharedPreferences.edit { putBoolean("clipboard", value) }
        }

    private var _styleFlow: MutableStateFlow<UiStyle>? = null
    val styleFlow: StateFlow<UiStyle>
        get() = _styleFlow ?: MutableStateFlow(UiStyle.Classic).also {
            _styleFlow = it
        }

    var style: String
        get() = if (::sharedPreferences.isInitialized)
            sharedPreferences.getString("style", UiStyle.Classic.key) ?: UiStyle.Classic.key
        else
            UiStyle.Classic.key
        set(value) {
            check(::sharedPreferences.isInitialized) { "GlobalUtils not initialized" }
            sharedPreferences.edit().putString("style", value).apply()
        }

    fun setStyle(newStyle: UiStyle) {
        style = newStyle.key
        _styleFlow?.value = newStyle
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
            sharedPreferences.edit { putInt("filter_selection", value) }
        }

    // null pointer对应的safe解析时间：第一次启动时间
    var timeNull: LocalDateTime
        get() = parseDateTime(sharedPreferences.getString("time_null", LocalDateTime.now().toString())?: LocalDateTime.now().toString())!!
        set(value) {
            sharedPreferences.edit().putString("time_null", value.toString()).apply()
        }

    private fun loadSettings(context: Context) {
        Log.d("GlobalUtils", "Settings loaded from SharedPreferences")
        hideDividerUi = hideDivider

        val current = UiStyle.fromKey(style)
        if (_styleFlow == null) {
            _styleFlow = MutableStateFlow(current)
        } else {
            _styleFlow!!.value = current
        }
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
     * @param afterDateTime 若不为 null，则限制只能选择该时间之后（含当天）的日期和时间
     */
    fun showDateTimePicker(
        fragmentManager: FragmentManager,
        afterDateTime: LocalDateTime? = null,
        makeToast: (String) -> Unit = {},
        onDateTimeSelected: (LocalDateTime) -> Unit,
    ) {
        val zone = ZoneId.systemDefault()

        // 是否需要限制
        val minDayStartMillisUtc: Long? = afterDateTime?.toLocalDate()
            ?.atStartOfDay(zone)
            ?.toInstant()
            ?.toEpochMilli()

        val todayUtcMillis = MaterialDatePicker.todayInUtcMilliseconds()
        val defaultSelection = if (minDayStartMillisUtc != null) {
            maxOf(todayUtcMillis, minDayStartMillisUtc)
        } else {
            todayUtcMillis
        }

        val builder = MaterialDatePicker.Builder.datePicker()
            .setSelection(defaultSelection)

        if (minDayStartMillisUtc != null) {
            val constraints = CalendarConstraints.Builder()
                .setStart(minDayStartMillisUtc)
                .setValidator(DateValidatorPointForward.from(minDayStartMillisUtc))
                .build()
            builder.setCalendarConstraints(constraints)
        }

        val datePicker = builder.build()

        datePicker.addOnPositiveButtonClickListener { selectedDateUtcMillis ->
            val selectedLocalDate = Instant.ofEpochMilli(selectedDateUtcMillis)
                .atZone(zone)
                .toLocalDate()

            val baseDateTime = selectedLocalDate.atStartOfDay()
            val minAllowedTime: LocalTime? =
                if (afterDateTime != null && selectedLocalDate == afterDateTime.toLocalDate()) {
                    afterDateTime.toLocalTime()
                } else null

            showTimePickerWithGuard(
                fragmentManager = fragmentManager,
                datePart = baseDateTime,
                minAllowedTime = minAllowedTime,
                makeToast = makeToast,
                onDateTimeSelected = onDateTimeSelected
            )
        }

        datePicker.show(fragmentManager, "datePicker_after_${minDayStartMillisUtc ?: "none"}")
    }

    /**
     * 时间选择器，带可选的最小时间限制
     */
    private fun showTimePickerWithGuard(
        fragmentManager: FragmentManager,
        datePart: LocalDateTime,
        minAllowedTime: LocalTime?,
        makeToast: (String) -> Unit = {},
        onDateTimeSelected: (LocalDateTime) -> Unit,
    ) {
        val now = LocalTime.now()
        val initialTime = when (minAllowedTime) {
            null -> now
            else -> if (now.isBefore(minAllowedTime)) minAllowedTime else now
        }

        fun buildAndShow(hour: Int, minute: Int) {
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val picked = LocalTime.of(timePicker.hour, timePicker.minute)

                if (minAllowedTime != null && picked.isBefore(minAllowedTime)) {
                    makeToast("${minAllowedTime.hour.toString().padStart(2, '0')}:${minAllowedTime.minute.toString().padStart(2, '0')}")
                    buildAndShow(minAllowedTime.hour, minAllowedTime.minute)
                    return@addOnPositiveButtonClickListener
                }

                onDateTimeSelected(datePart.withHour(picked.hour).withMinute(picked.minute))
            }

            timePicker.show(fragmentManager, "timePicker_${datePart.toLocalDate()}_$hour:$minute")
        }

        buildAndShow(initialTime.hour, initialTime.minute)
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

    /**
     * 生成展示用的“剩余/开始于/已过期”文案。
     * - startTimeStr / endTimeStr：你的字符串时间（与 GlobalUtils.safeParseDateTime 同源）
     * - displayFullContent：对应旧逻辑中的 full/short 文案选择
     *
     * 依赖的 string 资源需与旧版保持一致：
     *  - R.string.ddl_overdue_full / ddl_overdue_short
     *  - R.string.starts_in_prefix / remaining_prefix
     *  - R.string.remaining_days / _hours / _minutes
     *  - R.string.remaining_days_short / _hours_short / _minutes_short
     *  - R.string.starts_in_compact_days / _short
     *  - R.string.remaining_compact_days / _short
     */
    fun buildRemainingTime(
        context: Context,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        displayFullContent: Boolean,
        now: LocalDateTime = LocalDateTime.now()
    ): String {
        val afterEnd = endTime?.isBefore(now) == true              // 已过结束
        val beforeStart = startTime?.isAfter(now) == true          // 尚未开始

        if (afterEnd) {
            return if (displayFullContent)
                context.getString(R.string.ddl_overdue_full)
            else
                context.getString(R.string.ddl_overdue_short)
        }

        // 需要展示正向“还有多久”（到开始 或 到结束）
        val target = if (beforeStart && startTime != null) startTime else (endTime ?: now)
        val remainMin = Duration.between(now, target).toMinutes().coerceAtLeast(0).toInt()

        val days = remainMin / (24 * 60)
        val hours = (remainMin % (24 * 60)) / 60
        val minutesPart = remainMin % 60
        val compactDays = remainMin.toFloat() / (24f * 60f)

        return if (beforeStart) {
            // —— 到开始 —— //
            if (displayFullContent) {
                if (GlobalUtils.detailDisplayMode) {
                    buildString {
                        append(context.getString(R.string.starts_in_prefix))
                        if (days != 0) append(context.getString(R.string.remaining_days, days))
                        if (hours != 0) append(context.getString(R.string.remaining_hours, hours))
                        append(context.getString(R.string.remaining_minutes, minutesPart))
                    }
                } else {
                    context.getString(R.string.starts_in_compact_days, compactDays)
                }
            } else {
                if (GlobalUtils.detailDisplayMode) {
                    buildString {
                        append(context.getString(R.string.starts_in_prefix))
                        if (days != 0) append(context.getString(R.string.remaining_days_short, days))
                        if (hours != 0) append(context.getString(R.string.remaining_hours_short, hours))
                        if (days == 0) append(context.getString(R.string.remaining_minutes_short, minutesPart))
                    }
                } else {
                    context.getString(R.string.starts_in_compact_days_short, compactDays)
                }
            }
        } else {
            // —— 到结束 —— //
            if (displayFullContent) {
                if (GlobalUtils.detailDisplayMode) {
                    buildString {
                        append(context.getString(R.string.remaining_prefix))
                        if (days != 0) append(context.getString(R.string.remaining_days, days))
                        if (hours != 0) append(context.getString(R.string.remaining_hours, hours))
                        append(context.getString(R.string.remaining_minutes, minutesPart))
                    }
                } else {
                    context.getString(R.string.remaining_compact_days, compactDays)
                }
            } else {
                if (GlobalUtils.detailDisplayMode) {
                    buildString {
                        if (days != 0) append(context.getString(R.string.remaining_days_short, days))
                        if (hours != 0) append(context.getString(R.string.remaining_hours_short, hours))
                        if (days == 0) append(context.getString(R.string.remaining_minutes_short, minutesPart))
                    }
                } else {
                    context.getString(R.string.remaining_compact_days_short, compactDays)
                }
            }
        }
    }
}