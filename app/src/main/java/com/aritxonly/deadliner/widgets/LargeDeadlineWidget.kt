package com.aritxonly.deadliner.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.LauncherActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max

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

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        if (context != null && appWidgetManager != null)
            updateLargeAppWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
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
    val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    val direction = sharedPreferences.getBoolean("widget_progress_dir", false)
    val showAddButton = sharedPreferences.getBoolean("show_add_button_large_ddl_widget", true)
    val addButtonVisibility = if (showAddButton) View.VISIBLE else View.GONE

    // 获取小部件选项（包含尺寸信息）
    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

    // 获取小部件最小高度（单位为dp）
    val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

    // 转换为像素（考虑屏幕密度）
    val displayMetrics = context.resources.displayMetrics
    val density = displayMetrics.density
    val minHeightPx = (minHeightDp * density).toInt()

    // 估算容器可用高度（减去添加按钮和其他元素的高度）
    val buttonHeightPx = (60 * density).toInt()  // 添加按钮高度估算
    val marginsPx = (16 * density).toInt()       // 安全边距
    val containerHeightPx = minHeightPx - buttonHeightPx - marginsPx

    val itemHeightPx = 36f.dpToPx()

    val maxItems = when {
        containerHeightPx <= 0 -> 0
        else -> (containerHeightPx / itemHeightPx).coerceAtMost(10) // 最多显示10条
    } - 1   // -1用于平衡布局

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
    views.setViewVisibility(R.id.btn_add_ddl, addButtonVisibility)

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
    val allDdls: List<DDLItem> = DatabaseHelper.Companion
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
        .take(if (maxItems > 0) maxItems else 0)

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

    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra("EXTRA_SHOW_SEARCH", true)
        // 保证不会新开多个 Activity
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    views.setOnClickPendingIntent(R.id.btn_search, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

fun Float.dpToPx(): Int =
    (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()