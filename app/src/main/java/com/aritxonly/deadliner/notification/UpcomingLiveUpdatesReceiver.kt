package com.aritxonly.deadliner.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.data.DatabaseHelper

class UpcomingLiveUpdatesReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 获取DDL ID
        val ddlId = intent.getLongExtra("DDL_ID", -1)

        Log.d("AlarmDebug", "LiveUpdates收到闹钟广播！DDL_ID: $ddlId")

        // 获取DDL项
        val dbHelper = DatabaseHelper.getInstance(context)
        val ddl = dbHelper.getDDLById(ddlId)

        if (ddl != null) {
            // 计算剩余时间
            val remainingTime = DeadlineAlarmScheduler.calculateRemainingTime(ddl)

            // 发送通知
            NotificationUtil.sendUpcomingDDLNotification(context, ddl, remainingTime)
        }
    }
}