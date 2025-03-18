package com.aritxonly.deadliner

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.Duration
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
        get() = sharedPreferences.getBoolean("first_run", true)
        set(value) {
            sharedPreferences.edit().putBoolean("first_run", value).apply()
        }

    private fun loadSettings() {
        Log.d("GlobalUtils", "Settings loaded from SharedPreferences")
    }

    fun dpToPx(dp: Float, context: Context): Float {
        return dp * context.resources.displayMetrics.density
    }

    fun parseDateTime(dateTimeString: String): LocalDateTime {
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

    fun filterArchived(item: DDLItem): Boolean {
        try {
            val completeTime = parseDateTime(item.completeTime)
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
}