package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.TileService

class AddDDLTileService : TileService() {
    override fun onTileAdded() {
        super.onTileAdded()
    }

    override fun onStartListening() {
        super.onStartListening()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()

        // 创建一个Intent，启动AddDDLActivity
        val intent = Intent(this, AddDDLActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // 创建一个 PendingIntent，用于启动活动
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 启动活动并折叠控制面板
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(pendingIntent)
        } else {
            startActivityAndCollapse(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        TileService.requestListeningState(this,
            ComponentName(this, AddDDLTileService::class.java)
        )
        return super.onBind(intent)!!
    }
}