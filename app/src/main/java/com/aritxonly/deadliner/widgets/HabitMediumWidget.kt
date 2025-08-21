package com.aritxonly.deadliner.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.LauncherActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.HabitMetaData
import java.time.LocalDate

class HabitMediumWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            updateMediumAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val provider = ComponentName(context, javaClass)
        for (appWidgetId in appWidgetIds) {
            deleteIdPref(context, appWidgetId, provider)
        }
        super.onDeleted(context, appWidgetIds)
    }

    companion object {
        fun updateWidget(context: Context,
                         appWidgetManager: AppWidgetManager,
                         appWidgetId: Int) {
            updateMediumAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateMediumAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) {
    val provider = ComponentName(context, HabitMediumWidget::class.java)
    val habitId = loadIdPref(context, appWidgetId, provider)

    val views = RemoteViews(context.packageName, R.layout.habit_medium_widget)

    val habit = DatabaseHelper.getInstance(context).getDDLById(habitId)
    if (habit != null) {
        views.setTextViewText(R.id.medium_title, habit.name)
        val habitMeta = com.aritxonly.deadliner.localutils.GlobalUtils.parseHabitMetaData(habit.note)

        val freqDesc = when (habitMeta.frequencyType) {
            DeadlineFrequency.DAILY ->
                "每天${habitMeta.frequency}次"
            DeadlineFrequency.WEEKLY ->
                "每周${habitMeta.frequency}次"
            DeadlineFrequency.MONTHLY ->
                "每月${habitMeta.frequency}次"
            DeadlineFrequency.TOTAL -> {
                if (habitMeta.total == 0) "持续坚持"
                else "共计${habitMeta.total}次"
            }
        }
        views.setTextViewText(R.id.medium_description, freqDesc)

        val canClick = canPerformClickHelper(habit, habitMeta)
        val label = if (canClick) "打卡" else "完成"
        views.setTextViewText(R.id.tv_checkin, label)

        // 点击行为：能打卡 → 发 ACTION_CHECK_IN；否则 → 打开 App（或发一个提示广播）
        val pending = if (canClick) {
            PendingIntent.getBroadcast(
                context, appWidgetId,
                Intent(context, HabitMiniWidget::class.java).apply {
                    action = ACTION_CHECK_IN
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra("extra_habit_id", habitId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                context, appWidgetId,
                Intent(context, LauncherActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        views.setOnClickPendingIntent(R.id.btn_checkin, pending)

        // 你还可以用 alpha 表达禁用态（可选）
         views.setFloat(R.id.btn_checkin, "setAlpha", if (canClick) 1f else 0.6f)
    } else {
        views.setTextViewText(R.id.medium_title, context.getString(R.string.app_name))
        views.setTextViewText(R.id.tv_checkin, context.getString(R.string.add_widget))
        views.setOnClickPendingIntent(
            R.id.btn_checkin,
            PendingIntent.getActivity(
                context, appWidgetId,
                Intent(context, LauncherActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    // 容器点击 → 打开 App
    views.setOnClickPendingIntent(
        R.id.widget_container,
        PendingIntent.getActivity(
            context, 0,
            Intent(context, LauncherActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    )

    appWidgetManager.updateAppWidget(appWidgetId, views)
}