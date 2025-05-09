package com.aritxonly.deadliner

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MultiDeadlineWidget : AppWidgetProvider() {
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
    }
}

fun getThemeColor(context: Context, attrResId: Int): Int {
    val typedValue = TypedValue()
    val theme = context.theme
    theme.resolveAttribute(attrResId, typedValue, true)
    return if (typedValue.resourceId != 0) {
        ContextCompat.getColor(context, typedValue.resourceId)
    } else {
        typedValue.data
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.multi_deadline_widget)
    val sharedPreferences = context.getSharedPreferences("app_settings", MODE_PRIVATE)
    val direction = sharedPreferences.getBoolean("widget_progress_dir", false)

    // 设置点击事件，点击小组件打开 MainActivity
    val intent = Intent(context, LauncherActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)

    val dbHelper = DatabaseHelper.getInstance(context)
    val allDDLs = dbHelper.getDDLsByType(DeadlineType.TASK)

    val color = getThemeColor(context, android.R.attr.textColorPrimary)
    views.setInt(R.id.widgetFinishIcon, "setColorFilter", color)

    val now = LocalDateTime.now()
    val parsedDDLs = allDDLs.map { ddl ->
        val startTime = GlobalUtils.safeParseDateTime(ddl.startTime)
        val endTime = GlobalUtils.safeParseDateTime(ddl.endTime)
        val remainingMillis = ChronoUnit.MILLIS.between(now, endTime)
        val isCompleted = ddl.isCompleted
        val isStared = ddl.isStared
        ParsedDDL(ddl, startTime, endTime, remainingMillis, isCompleted, isStared)
    }

    // 按剩余时间排序
    val sortedDDLs = parsedDDLs.sortedWith(compareBy<ParsedDDL> { it.isCompleted }
        .thenBy { !it.isStared }
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
    val starIconIds = listOf(
        R.id.star_icon_1,
        R.id.star_icon_2,
        R.id.star_icon_3
    )

    // 隐藏所有Item
    itemContainers.forEach { (containerId, _, _) ->
        views.setViewVisibility(containerId, View.GONE)
    }

    var showCount = 0

    // 显示数据
    for ((index, parsed) in showList.withIndex()) {
        val (containerId, titleId, progressId) = itemContainers[index]
        val progressTextId = progressTextIds[index]
        val starIconId = starIconIds[index]

        if (parsed.isCompleted) {
            continue    // 设置已完成的不显示
        }

        views.setViewVisibility(
            starIconId,
            if (parsed.isStared) View.VISIBLE else View.GONE
        )

        val colorStar = getThemeColor(context, android.R.attr.textColorPrimaryInverse)
        views.setInt(starIconId, "setColorFilter", colorStar)


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
            val days: Double = remainingMillis.toDouble() / (3600000 * 24)
            if (days < 1.0f) {
                val hours: Double = remainingMillis.toDouble() / 3600000
                "%.1f".format(hours) + "小时" + " $progress%"
            } else {
                "%.1f".format(days) + "天" + " $progress%"
            }
        }

        showCount++
        views.setTextViewText(progressTextId, timeText)
    }

    views.setViewVisibility(R.id.widgetFinishNotice, if (showCount == 0) {
        View.VISIBLE
    } else {
        View.GONE
    })

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

data class ParsedDDL(
    val ddl: DDLItem,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val remainingMillis: Long,
    val isCompleted: Boolean,
    val isStared: Boolean
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