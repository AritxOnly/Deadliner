package com.aritxonly.deadliner.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationManagerCompat
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import java.time.Duration
import java.time.LocalDateTime

class UpcomingLiveUpdateService : Service() {

    companion object {
        fun start(context: Context, ddl: DDLItem) {
            val i = Intent(context, UpcomingLiveUpdateService::class.java)
                .putExtra("DDL_ID", ddl.id)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else context.startService(i)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var ddl: DDLItem? = null
    private var notificationId = 0

    private val tick = object : Runnable {
        override fun run() {
            val d = ddl ?: return
            val latest = DatabaseHelper.getInstance(this@UpcomingLiveUpdateService)
                .getDDLById(d.id) ?: return stopSelf()

            ddl = latest
            val remaining = calculateRemainingSeconds(latest)

            if (remaining <= -300) {
                val nm = NotificationManagerCompat.from(this@UpcomingLiveUpdateService)
                nm.cancel(notificationId)
                stopSelf()
                return
            }

            // 到期或完成就做最后一版并退出
            if (remaining <= 0 || latest.isCompleted || latest.isArchived) {
                NotificationUtil.sendUpcomingDDLNotification(this@UpcomingLiveUpdateService, latest, remaining)
                stopSelf()
                return
            }

            // 常规 30s 刷新
            NotificationUtil.sendUpcomingDDLNotification(this@UpcomingLiveUpdateService, latest, remaining)
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationUtil.createNotificationChannels(this) // 你项目里已有的话保留
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ddlId = intent?.getLongExtra("DDL_ID", -1L) ?: -1L
        if (ddlId <= 0) return START_NOT_STICKY

        // 拉取最新 DDL
        val d = DatabaseHelper.getInstance(this).getDDLById(ddlId) ?: return START_NOT_STICKY
        ddl = d
        notificationId = d.id.hashCode()

        // 首帧：用 NotificationUtil 构建通知并前台化
        val firstRemaining = calculateRemainingSeconds(d)
        val first = NotificationUtil.createUpcomingDDLNotification(this, d, firstRemaining)
        startForeground(notificationId, first)

        // 周期刷新
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(tick, 30_000L)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun calculateRemainingSeconds(d: DDLItem): Long {
        val now = LocalDateTime.now()
        val end = GlobalUtils.safeParseDateTime(d.endTime)
        return Duration.between(now, end).seconds
    }
}