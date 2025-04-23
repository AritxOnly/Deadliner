package com.aritxonly.deadliner

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
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

object GlobalUtils {
    private const val PREF_NAME = "app_settings"

    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, AppCompatActivity.MODE_PRIVATE)
        loadSettings()  // 初始化时加载设置
    }

    var vibration: Boolean
        get() = sharedPreferences.getBoolean("vibration", true)
        set(value) {
            sharedPreferences.edit().putBoolean("vibration", value).apply()
        }

    var progressDir: Boolean
        get() = sharedPreferences.getBoolean("main_progress_dir", false)
        set(value) {
            sharedPreferences.edit().putBoolean("main_progress_dir", value).apply()
        }

    var progressWidget: Boolean
        get() = sharedPreferences.getBoolean("widget_progress_dir", false)
        set(value) {
            sharedPreferences.edit().putBoolean("widget_progress_dir", value).apply()
        }

    var deadlineNotification: Boolean
        get() = sharedPreferences.getBoolean("deadline_notification", false)
        set(value) {
            sharedPreferences.edit().putBoolean("deadline_notification", value).apply()
        }

    var dailyStatsNotification: Boolean
        get() = sharedPreferences.getBoolean("daily_stats_notification", false)
        set(value) {
            sharedPreferences.edit().putBoolean("daily_stats_notification", value).apply()
        }

    var motivationalQuotes: Boolean
        get() = sharedPreferences.getBoolean("motivational_quotes", true)
        set(value) {
            sharedPreferences.edit().putBoolean("motivational_quotes", value).apply()
        }

    var fireworksOnFinish: Boolean
        get() = sharedPreferences.getBoolean("fireworks_anim", true)
        set(value) {
            sharedPreferences.edit().putBoolean("fireworks_anim", value).apply()
        }

    var autoArchiveTime: Int
        get() = sharedPreferences.getInt("archive_time", 7)
        set(value) {
            sharedPreferences.edit().putInt("archive_time", value).apply()
        }

    var firstRun: Boolean
        get() = sharedPreferences.getBoolean("first_run_v2", true)
        set(value) {
            sharedPreferences.edit().putBoolean("first_run_v2", value).apply()
        }

    var showIntroPage: Boolean
        get() = sharedPreferences.getBoolean("show_intro_page", true)
        set(value) {
            sharedPreferences.edit().putBoolean("show_intro_page", value).apply()
        }

    var detailDisplayMode: Boolean
        get() = sharedPreferences.getBoolean("detail_display_mode", true)
        set(value) {
            sharedPreferences.edit().putBoolean("detail_display_mode", value).apply()
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
                ?: HabitMetaData(emptySet(), DeadlineFrequency.DAILY, 1, 0, LocalDate.now().toString())
        } catch (e: Exception) {
            HabitMetaData(emptySet(), DeadlineFrequency.DAILY, 1, 0, LocalDate.now().toString())
        }

        return habitMeta
    }
}