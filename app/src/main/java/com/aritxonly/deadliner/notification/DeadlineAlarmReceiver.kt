package com.aritxonly.deadliner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.aritxonly.deadliner.notification.NotificationUtil.sendImmediateNotification
import java.time.Duration
import java.time.LocalDateTime

class DeadlineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action?.startsWith("ACTION_DDL_ALARM_") == false) return

        Log.d("AlarmDebug", "action: ${intent.action}")

        android.util.Log.d("AlarmDebug", "Actually, I reached here")

        val ddlIdStr = intent.action?.substringAfterLast("_")!!
        val ddlId = ddlIdStr.toIntOrNull()
        if (ddlId == null) {
            android.util.Log.e("AlarmDebug", "无法解析 DDL_ID: $ddlIdStr")
            return
        }

//        val ddlId = intent.getIntExtra("DDL_ID", -1)
        val ddl = DatabaseHelper.getInstance(context).getDDLById(ddlId.toLong()) ?: return

        if (ddl.type == DeadlineType.HABIT && ddl.isCompleted) return

        android.util.Log.d("AlarmDebug", "收到闹钟广播！DDL: $ddl")

        // 直接发送通知（移除保活服务逻辑）
        sendImmediateNotification(context, ddl)
    }
}