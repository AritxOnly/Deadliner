package com.aritxonly.deadliner.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aritxonly.deadliner.DatabaseHelper
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.GlobalUtils

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // 重新注册所有alarms
                val allDDLs = DatabaseHelper.getInstance(context).getAllDDLs()
                allDDLs.forEach { DeadlineAlarmScheduler.scheduleExactAlarm(context, it) }
                DeadlineAlarmScheduler.scheduleDailyAlarm(context)
            }
        }
    }
}