package com.aritxonly.deadliner

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE

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
    }
}