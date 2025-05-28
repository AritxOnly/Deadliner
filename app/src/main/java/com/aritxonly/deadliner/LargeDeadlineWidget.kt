package com.aritxonly.deadliner

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class LargeDeadlineWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateLargeAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED, // 应用升级（覆盖安装）
            Intent.ACTION_CONFIGURATION_CHANGED, // 设置改变
            Intent.ACTION_BOOT_COMPLETED -> {  // 开机后
                refreshAllWidgets(context)
            }
        }
    }

    private fun refreshAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, LargeDeadlineWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidget(context: Context,
                         appWidgetManager: AppWidgetManager,
                         appWidgetId: Int) {
            updateLargeAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateLargeAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.large_deadline_widget)
    val sharedPreferences = context.getSharedPreferences("app_settings", MODE_PRIVATE)
    val direction = sharedPreferences.getBoolean("widget_progress_dir", false)

    // 打开 AddDDLActivity 的 PendingIntent
    val addIntent = Intent(context, AddDDLActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val addPi = PendingIntent.getActivity(
        context,
        0,
        addIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.btn_add_ddl, addPi)


    val mainIntent = Intent(context, LauncherActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val containerPi = PendingIntent.getActivity(
        context,
        0,
        mainIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.large_item_container, containerPi)

    // 清空容器
    views.removeAllViews(R.id.large_item_container)

    // 获取数据
    val allDdls: List<DDLItem> = DatabaseHelper
        .getInstance(context)
        .getDDLsByType(DeadlineType.TASK)
        .filter { !it.isCompleted && !it.isArchived }

    // 解析并排序
    val now = LocalDateTime.now()
    val parsed = allDdls.map { ddl ->
        val start = GlobalUtils.safeParseDateTime(ddl.startTime)
        val end = GlobalUtils.safeParseDateTime(ddl.endTime)
        val remaining = ChronoUnit.MILLIS.between(now, end)
        ParsedDDL(ddl, start, end, remaining, false, false)
    }

    val sorted = parsed
        .sortedWith(compareBy<ParsedDDL> { it.ddl.isStared.not() }
            .thenBy { it.remainingMillis })

    // 动态添加每条 item
    for (item in sorted) {
        val itemRv = RemoteViews(context.packageName, R.layout.deadline_item)
        // 标题
        itemRv.setTextViewText(R.id.item_title, item.ddl.name)
        // 星标
        val starVisibility = if (item.ddl.isStared) View.VISIBLE else View.GONE
        itemRv.setViewVisibility(R.id.starIcon, starVisibility)
        // 进度
        val total = ChronoUnit.MILLIS.between(item.startTime, item.endTime)
        val maxTotal = maxOf(0, total)
        val done = ChronoUnit.MILLIS.between(item.startTime, now).coerceIn(0, maxTotal)
        val percent = (done * 100 / total).toInt()
        itemRv.setProgressBar(
            R.id.item_progress,
            100,
            if (direction) percent else 100 - percent,
            false
        )
        // 剩余时间文本
        val text = if (item.remainingMillis < 0) {
            itemRv.setProgressBar(R.id.item_progress, 100, 0, false)
            "逾期"
        } else {
            val days: Double = item.remainingMillis.toDouble() / (3600000 * 24)
            if (days < 1.0f) {
                val hours: Double = item.remainingMillis.toDouble() / 3600000
                "%.1f".format(hours) + "小时" + " $percent%"
            } else {
                "%.1f".format(days) + "天" + " $percent%"
            }
        }
        itemRv.setTextViewText(R.id.item_progress_text, text)

        views.addView(R.id.large_item_container, itemRv)
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}