package com.aritxonly.deadliner

import android.app.PendingIntent
import android.app.UiModeManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.RemoteViews
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

class SingleDeadlineWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context,
                         appWidgetManager: AppWidgetManager,
                         appWidgetId: Int) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }

        // 尝试解析时间字符串的函数
        fun parseDateTime(dateTimeString: String): LocalDateTime {
            val formatters = listOf(
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

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.single_deadline_widget)
    val sharedPreferences = context.getSharedPreferences("app_settings", MODE_PRIVATE)
    val direction = sharedPreferences.getBoolean("widget_progress_dir", false)

    // 设置点击事件，点击小组件打开 MainActivity
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)

    val dbHelper = DatabaseHelper(context)
    val allDDLs = dbHelper.getAllDDLs()

    val now = LocalDateTime.now()
    val parsedDDLs = allDDLs.map { ddl ->
        val startTime = SingleDeadlineWidget.parseDateTime(ddl.startTime)
        val endTime = SingleDeadlineWidget.parseDateTime(ddl.endTime)
        val remainingMillis = ChronoUnit.MILLIS.between(now, endTime)
        val isCompleted = ddl.isCompleted
        ParsedDDL(ddl, startTime, endTime, remainingMillis, isCompleted)
    }

    // 按剩余时间排序
    val sortedDDLs = parsedDDLs.sortedWith(compareBy<ParsedDDL> { it.isCompleted }
        .thenBy {
            it.remainingMillis
        })

    Log.d("Widget", "DDLs $parsedDDLs")
    // 取前3个
    val showList = sortedDDLs.take(3)

    val itemContainers = listOf(
        Triple(R.id.ddl_item_1, R.id.item_title_1, R.id.item_progress_1),
        Triple(R.id.ddl_item_2, R.id.item_title_2, R.id.item_progress_2),
        Triple(R.id.ddl_item_3, R.id.item_title_3, R.id.item_progress_3)
    )
    val progressTextIds = listOf(
        R.id.item_progress_text_1,
        R.id.item_progress_text_2,
        R.id.item_progress_text_3
    )

    // 隐藏所有Item
    itemContainers.forEach { (containerId, _, _) ->
        views.setViewVisibility(containerId, View.GONE)
    }

    // 显示数据
    for ((index, parsed) in showList.withIndex()) {
        val (containerId, titleId, progressId) = itemContainers[index]
        val progressTextId = progressTextIds[index]

        if (parsed.isCompleted) {
            continue    // 设置已完成的不显示
        }

        views.setViewVisibility(containerId, View.VISIBLE)
        views.setTextViewText(titleId, parsed.ddl.name)

        val progress = calculateProgress(parsed.startTime, parsed.endTime)
        views.setProgressBar(
            progressId,
            100,
            if (!direction) {
                100 - progress
            } else {
                progress
            },
            false)

        // 将剩余时间转换为小时和分钟格式
        val remainingMillis = parsed.remainingMillis
        val timeText = if (remainingMillis < 0) {
            views.setProgressBar(progressId, 100, 0, false)
            "逾期" // 如果已过期或剩余时间为负数，则显示0.0h
        } else {
            val hours: Double = remainingMillis.toDouble() / 3600000
            "%.1f".format(hours) + "h"
        }

        views.setTextViewText(progressTextId, timeText)
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

data class ParsedDDL(
    val ddl: DDLItem,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val remainingMillis: Long,
    val isCompleted: Boolean
)

private fun calculateProgress(start: LocalDateTime, end: LocalDateTime): Int {
    val now = LocalDateTime.now()
    val totalMillis = ChronoUnit.MILLIS.between(start, end)
    val currentMillis = ChronoUnit.MILLIS.between(start, now)

    return when {
        currentMillis <= 0 -> 0
        currentMillis >= totalMillis -> 100
        else -> ((currentMillis.toDouble() / totalMillis) * 100).toInt()
    }
}