package com.aritxonly.deadliner.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.aritxonly.deadliner.DatabaseHelper
import com.aritxonly.deadliner.LauncherActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.HabitMetaData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [HabitWidgetConfigureActivity]
 */
class HabitMiniWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppMiniHabitWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val provider = ComponentName(context, javaClass)
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            deleteIdPref(context, appWidgetId, provider)
        }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_CONFIGURATION_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                // 日期/时间/时区变化时，刷新全部 → 按当天状态更新“打卡/已完成”
                refreshAllWidgets(context)
                return
            }
        }

        if (intent.action == ACTION_CHECK_IN) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val habitId = intent.getLongExtra("extra_habit_id", -1L)
            if (widgetId == -1 || habitId == -1L) return

            val db = DatabaseHelper.getInstance(context)
            val habitItem = db.getDDLById(habitId) ?: run {
                refreshOneWidget(context, widgetId) // 尝试刷新兜底
                return
            }
            val habitMeta = com.aritxonly.deadliner.localutils.GlobalUtils.parseHabitMetaData(habitItem.note)

            val canPerformClick = canPerformClickHelper(habitItem, habitMeta)
            val underTotalLimit = (habitMeta.total == 0) || (habitItem.habitTotalCount < habitMeta.total)

            if (!canPerformClick) {
                // 不能打卡：直接刷新按钮文案为“已完成”，并提示
                android.widget.Toast.makeText(
                    context,
                    if (!underTotalLimit) context.getString(R.string.snackbar_done)
                    else context.getString(R.string.snackbar_already_checkin),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                refreshOneWidget(context, widgetId)
                return
            }

            // 能打卡 → 执行打卡逻辑
            com.aritxonly.deadliner.localutils.GlobalUtils.checkInFromWidget(
                context, habitItem, habitMeta, canPerformClick,
                object : com.aritxonly.deadliner.localutils.GlobalUtils.OnWidgetCheckInGlobalListener {
                    override fun onCheckInFailedGlobal(ctx: Context, item: com.aritxonly.deadliner.model.DDLItem) {
                        // 失败（并不常见）→ 刷新状态
                        refreshOneWidget(ctx, widgetId)
                    }

                    override fun onCheckInSuccessGlobal(
                        ctx: Context,
                        item: com.aritxonly.deadliner.model.DDLItem,
                        meta: com.aritxonly.deadliner.model.HabitMetaData
                    ) {
                        android.widget.Toast.makeText(ctx, "打卡成功 🎉", android.widget.Toast.LENGTH_SHORT).show()
                        refreshOneWidget(ctx, widgetId)
                    }
                }
            )
        }
    }

    private fun refreshOneWidget(context: Context, appWidgetId: Int) {
        val awm = AppWidgetManager.getInstance(context)
        val info = awm.getAppWidgetInfo(appWidgetId)
        if (info != null) {
            when (info.provider.className) {
                HabitMiniWidget::class.java.name -> updateAppMiniHabitWidget(context, awm, appWidgetId)
                HabitMediumWidget::class.java.name -> updateMediumAppWidget(context, awm, appWidgetId)
                else -> updateAppMiniHabitWidget(context, awm, appWidgetId) // 默认兜底
            }
        } else {
            // 极少数情况（系统已移除 info），可以尝试两类都刷一下（安全起见一般不需要）
            updateAppMiniHabitWidget(context, awm, appWidgetId)
        }
    }

    private fun refreshAllWidgets(context: Context?) {
        if (context == null) return
        val awm = AppWidgetManager.getInstance(context)

        // 刷新 Mini
        awm.getAppWidgetIds(ComponentName(context, HabitMiniWidget::class.java))
            .forEach { updateAppMiniHabitWidget(context, awm, it) }

        // 刷新 Medium
        awm.getAppWidgetIds(ComponentName(context, HabitMediumWidget::class.java))
            .forEach { updateMediumAppWidget(context, awm, it) }
    }

    companion object {
        fun updateWidget(context: Context,
                         appWidgetManager: AppWidgetManager,
                         appWidgetId: Int) {
            updateMediumAppWidget(context, appWidgetManager, appWidgetId)
            updateAppMiniHabitWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal const val ACTION_CHECK_IN = "com.aritxonly.deadliner.ACTION_CHECK_IN"

internal fun canPerformClickHelper(habitItem: DDLItem, habitMeta: HabitMetaData): Boolean {
    val completedDates = habitMeta.completedDates.map { LocalDate.parse(it) }.toSet()
    val today = LocalDate.now()

    // —— 总次数限制 ——
    val underTotalLimit = habitMeta.total == 0 || habitItem.habitTotalCount < habitMeta.total

    // —— 今日频率限制 ——
    val underDailyLimit = when (habitMeta.frequencyType) {
        DeadlineFrequency.TOTAL -> true
        DeadlineFrequency.DAILY -> today !in completedDates
        DeadlineFrequency.WEEKLY -> {
            // 判断本周内是否已达 frequency 次（可用 completedDates.count { in same week } < frequency）
            val weekStart = today.with(DayOfWeek.MONDAY)
            val thisWeekCount = completedDates.count { it in weekStart..today }
            thisWeekCount < habitMeta.frequency
        }
        DeadlineFrequency.MONTHLY -> {
            val month = YearMonth.from(today)
            val thisMonthCount = completedDates.count { YearMonth.from(it) == month }
            thisMonthCount < habitMeta.frequency
        }
    }

    return underTotalLimit && underDailyLimit
}

internal fun updateAppMiniHabitWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) {
    val awm = AppWidgetManager.getInstance(context)
    val info = awm.getAppWidgetInfo(appWidgetId)
    val provider = info.provider

    val habitId = loadIdPref(context, appWidgetId, provider)

    val mainIntent = Intent(context, LauncherActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val containerPi = PendingIntent.getActivity(
        context,
        0,
        mainIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val checkInIntent = Intent(context, HabitMiniWidget::class.java).apply {
        action = ACTION_CHECK_IN
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        putExtra("extra_habit_id", habitId)
    }
    val checkInPi = PendingIntent.getBroadcast(
        context,
        appWidgetId,
        checkInIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )


    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.habit_mini_widget).apply {
        val habit = DatabaseHelper.getInstance(context).getDDLById(habitId) ?: return
        setTextViewText(R.id.mini_text, habit.name)

        val habitMeta = com.aritxonly.deadliner.localutils.GlobalUtils.parseHabitMetaData(habit.note)

        val canClick = canPerformClickHelper(habit, habitMeta)
        setFloat(R.id.btn_check_in, "setAlpha", if (canClick) 1f else 0.6f)

        setOnClickPendingIntent(R.id.widget_container, containerPi)
        setOnClickPendingIntent(R.id.btn_check_in, checkInPi)
    }

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}