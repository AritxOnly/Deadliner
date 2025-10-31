package com.aritxonly.deadliner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.notification.NotificationUtil.sendImmediateNotification
import java.time.Duration
import java.time.LocalDateTime

class DeadlineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
//        if (intent.action?.startsWith("ACTION_DDL_ALARM_") == false) return

        Log.d("AlarmDebug", "action: ${intent.action}")

        val ddlId = intent.getLongExtra("DDL_ID", -1)
        if (ddlId == (-1).toLong()) {
            Log.e("AlarmDebug", "传参不成功")
            return
        }

        val ddl = DatabaseHelper.getInstance(context).getDDLById(ddlId.toLong()) ?: return

        if (ddl.type == DeadlineType.HABIT && ddl.isCompleted) return
	
        val endTime = GlobalUtils.parseDateTime(ddl.endTime)
        val duration = Duration.between(LocalDateTime.now(), endTime)
        if (duration.toMinutes() < 0) return

        Log.d("AlarmDebug", "收到闹钟广播！DDL: $ddl")

        sendImmediateNotification(context, ddl)

        DeadlineAlarmScheduler.cancelExactAlarm(context, ddl.id)

        GlobalUtils.NotificationStatusManager.markAsNotified(ddl.id)
    }
}
