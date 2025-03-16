package com.aritxonly.deadliner

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
}