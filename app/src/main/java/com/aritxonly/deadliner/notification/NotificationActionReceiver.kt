package com.aritxonly.deadliner.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import java.time.Duration
import java.time.LocalDateTime

const val ACTION_MARK_COMPLETE = "com.aritxonly.deadliner.notification.ACTION_MARK_COMPLETE"
const val ACTION_DELETE        = "com.aritxonly.deadliner.notification.ACTION_DELETE"
const val EXTRA_DDL_ID         = "com.aritxonly.deadliner.notification.EXTRA_DDL_ID"
const val ACTION_LATER         = "com.aritxonly.deadliner.notification.ACTION_LATER"

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        val ddlId = intent?.getLongExtra(EXTRA_DDL_ID, -1) ?: return

        if (ddlId == -1L || context == null) return

        val databaseHelper = DatabaseHelper.getInstance(context)
        val item = databaseHelper.getDDLById(ddlId) ?: return

        when (intent.action) {
            ACTION_MARK_COMPLETE -> {
                DDLRepository().updateDDL(item.copy(
                    isCompleted = !item.isCompleted,
                    completeTime = LocalDateTime.now().toString()
                ))
            }

            ACTION_DELETE -> {
                databaseHelper.deleteDDL(ddlId)
                Toast.makeText(context, R.string.toast_deletion, Toast.LENGTH_LONG).show()
            }

            ACTION_LATER -> {
                DeadlineAlarmScheduler.cancelAlarm(context, ddlId.toLong())
                GlobalUtils.NotificationStatusManager.clearNotified(ddlId)

                val endTime = GlobalUtils.safeParseDateTime(item.endTime)
                val now = LocalDateTime.now()
                val totalHours = Duration.between(now, endTime).toHours()
                val delayHours = maxOf(1, totalHours / 2)
                DeadlineAlarmScheduler.scheduleExactAlarm(context, item, delayHours)

                Toast.makeText(context, context.getString(R.string.remind_in, delayHours), Toast.LENGTH_LONG).show()
            }

            else -> return
        }

        NotificationManagerCompat.from(context)
            .cancel(ddlId.hashCode())
    }
}
