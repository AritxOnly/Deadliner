package com.aritxonly.deadliner.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.DDLItem
import com.aritxonly.deadliner.DatabaseHelper
import java.time.Duration
import java.time.LocalDateTime

class KeepAliveService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wakeLock: PowerManager.WakeLock

    // 定义通知相关常量
    companion object {
        private const val NOTIFICATION_ID = 0x1002
        private const val CHANNEL_ID = "keep_alive_channel"
        private const val WAKE_LOCK_TAG = "Deadliner:KeepAliveLock"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Notification", "Here")
        val ddlId = intent?.getIntExtra("DDL_ID", -1) ?: return START_NOT_STICKY
        val triggerTime = intent.getSerializableExtra("TRIGGER_TIME") as LocalDateTime

        Log.d("Notification", "Reached here")

        // 创建持久唤醒锁
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            acquire(10 * 60 * 1000L) // 保持10分钟
        }

        // 前台通知配置
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            buildPersistentNotification()
        )

        // 启动定时检查
        startPolling(triggerTime, ddlId.toLong())
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keep app running in background"
                setShowBadge(false)
            }

            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildPersistentNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Deadliner 运行中")
            .setContentText("正在保障您的截止提醒")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startPolling(triggerTime: LocalDateTime, ddlId: Long) {
        val databaseHelper = DatabaseHelper.getInstance(this)

        handler.postDelayed(object : Runnable {
            override fun run() {
                val remaining = Duration.between(LocalDateTime.now(), triggerTime)
                if (remaining.toMillis() <= 0) {
                    // 获取最新的DDL数据
                    val ddl = databaseHelper.getDDLById(ddlId)
                    ddl?.let {
                        NotificationUtil.sendImmediateNotification(
                            this@KeepAliveService,
                            it
                        )
                    }
                    stopSelf()
                } else {
                    handler.postDelayed(this, 30_000) // 每30秒检查一次
                }
            }
        }, 0)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        super.onDestroy()
    }
}