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
import com.aritxonly.deadliner.model.updateNoteWithDate
import java.time.LocalDate

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [HabitMiniWidgetConfigureActivity]
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
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            deleteIdPref(context, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        when (intent?.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED, // 应用升级（覆盖安装）
            Intent.ACTION_CONFIGURATION_CHANGED, // 设置改变
            Intent.ACTION_BOOT_COMPLETED -> {  // 开机后
                refreshAllWidgets(context)
            }
        }

        if (intent?.action == ACTION_CHECK_IN) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val habitId = intent.getLongExtra("extra_habit_id", -1L)
            if (widgetId != -1 && habitId != -1L) {
                val habitItem = DatabaseHelper.getInstance(context?:return).getDDLById(habitId)?:return
                val habitMeta = GlobalUtils.parseHabitMetaData(habitItem.note)

                val canPerformClick = canPerformClickHelper(habitItem, habitMeta)

                GlobalUtils.checkInFromWidget(context, habitItem, habitMeta, canPerformClick, object : GlobalUtils.OnWidgetCheckInGlobalListener {
                    override fun onCheckInFailedGlobal(
                        context: Context,
                        habitItem: DDLItem,
                    ) {
                        Toast.makeText(context, R.string.snackbar_already_checkin, Toast.LENGTH_LONG).show()
                    }

                    override fun onCheckInSuccessGlobal(
                        context: Context,
                        habitItem: DDLItem,
                        habitMeta: HabitMetaData,
                    ) {
                        Toast.makeText(context, "打卡成功 🎉", Toast.LENGTH_LONG).show()
                    }
                })

                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateAppMiniHabitWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    private fun refreshAllWidgets(context: Context?) {
        if (context == null) return
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, MultiDeadlineWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidget(context: Context,
                         appWidgetManager: AppWidgetManager,
                         appWidgetId: Int) {
            updateAppMiniHabitWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal const val ACTION_CHECK_IN = "com.aritxonly.deadliner.ACTION_CHECK_IN"

internal fun canPerformClickHelper(habitItem: DDLItem, habitMeta: HabitMetaData): Boolean {
    val completedDates: Set<LocalDate> = habitMeta.completedDates.map { LocalDate.parse(it) }.toSet()

    val canCheckIn = (habitMeta.total != 0 && (if (habitMeta.frequencyType != DeadlineFrequency.TOTAL) {
        (habitItem.habitCount < habitMeta.frequency) && (completedDates.size < habitMeta.total)
    } else true) && (habitItem.habitTotalCount < habitMeta.total)) || (habitMeta.total == 0)

    val alreadyChecked = when (habitMeta.frequencyType) {
        DeadlineFrequency.TOTAL -> false
        else -> habitMeta.frequency <= habitItem.habitCount
    }
    val canPerformClick = canCheckIn && !alreadyChecked

    return canPerformClick
}

internal fun updateAppMiniHabitWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) {
    val habitId = loadIdPref(context, appWidgetId)

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
        val habit = DatabaseHelper.getInstance(context).getDDLById(habitId)
        setTextViewText(R.id.mini_text, (habit?:return).name)

        setOnClickPendingIntent(R.id.widget_container, containerPi)
        setOnClickPendingIntent(R.id.btn_check_in, checkInPi)
    }

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}