package com.aritxonly.deadliner

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GlobalUtils {
    // TODO: 保证代码的可复用性
    companion object {
        // TODO: 设置变量，常用的工具函数

        var autoArchiveTime = 7

        fun writeConfigInSettings(context: Context) {
            val sharedPreferences = context.getSharedPreferences("app_settings", MODE_PRIVATE)
            sharedPreferences.edit().putInt("archive_time", autoArchiveTime).apply()
            Log.d("GlobalUtils", "writing: $autoArchiveTime")
        }

        fun readConfigInSettings(context: Context) {
            val sharedPreferences = context.getSharedPreferences("app_settings", MODE_PRIVATE)
            autoArchiveTime = sharedPreferences.getInt("archive_time", 7)
            Log.d("GlobalUtils", "reading: $autoArchiveTime")
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
    }
}