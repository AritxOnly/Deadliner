package com.aritxonly.deadliner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aritxonly.deadliner.notification.NotificationUtil.sendDailyNotification

class DailyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null ||
            intent.action?.contains("com.aritxonly.deadliner.ACTION_DAILY_ALARM") == false)
            return

        Log.d("AlarmDebug", "DailyAlarmReceiver")

        sendDailyNotification(context)
    }
}