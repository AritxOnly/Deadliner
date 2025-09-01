package com.aritxonly.deadliner.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import com.aritxonly.deadliner.localutils.GlobalUtils

object SyncScheduler {

    private const val UNIQUE_PERIODIC_WORK = "webdav_periodic_sync"
    private const val UNIQUE_ONETIME_WORK  = "webdav_kick_sync"

    /**
     * 根据 GlobalUtils 构建约束：
     * - Wi-Fi：UNMETERED（仅非计费网络）
     * - 否则：CONNECTED（任意可用网络）
     * - 充电时：requiresCharging = true
     */
    private fun constraintsFromGlobals(): Constraints {
        val builder = Constraints.Builder()
            .setRequiredNetworkType(
                if (GlobalUtils.syncWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            )

        if (GlobalUtils.syncChargingOnly) {
            builder.setRequiresCharging(true)
        }
        // 可按需启用：低电量不执行
        // builder.setRequiresBatteryNotLow(true)

        return builder.build()
    }

    /**
     * 构建周期请求
     *
     * @param minutes 用户选择的分钟数；会被纠正到 >= 15 分钟
     */
    private fun buildPeriodic(minutes: Int): PeriodicWorkRequest {
        val periodMin = max(15, minutes) // WorkManager 下限 15 分钟
        val flexMin = min(15, periodMin / 3) // flex 取 1/3，上限 15 分钟

        return PeriodicWorkRequestBuilder<SyncWorker>(
            periodMin.toLong(), TimeUnit.MINUTES,
            flexMin.toLong(), TimeUnit.MINUTES
        )
            .setConstraints(constraintsFromGlobals())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS // 失败指数退避
            )
            .addTag(UNIQUE_PERIODIC_WORK)
            .build()
    }

    /**
     * 读取 GlobalUtils 的设置并安排周期任务。
     * - cloudSyncEnable = false 或 interval <= 0：直接取消周期任务。
     */
    fun enqueuePeriodic(context: Context) {
        if (!GlobalUtils.cloudSyncEnable) {
            cancelPeriodic(context)
            return
        }
        val minutes = GlobalUtils.syncIntervalMinutes
        if (minutes <= 0) {
            // 用户选择“仅手动”
            cancelPeriodic(context)
            return
        }
        val request = buildPeriodic(minutes)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * 指定分钟数安排周期任务（不改动 GlobalUtils 的存储值）。
     * 常用于设置面板里临时选中后立即生效。
     */
    fun enqueuePeriodic(context: Context, minutes: Int) {
        if (!GlobalUtils.cloudSyncEnable || minutes <= 0) {
            cancelPeriodic(context)
            return
        }
        val request = buildPeriodic(minutes)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * 立刻触发一次同步（不影响周期任务）。
     * - 使用与周期相同的约束（Wi-Fi / 充电），避免用户意外消耗流量/电量。
     */
    fun enqueueOneTimeNow(context: Context) {
        if (!GlobalUtils.cloudSyncEnable) return

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraintsFromGlobals())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS
            )
            .addTag(UNIQUE_ONETIME_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONETIME_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** 仅取消周期任务（保留一次性触发队列）。 */
    fun cancelPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK)
    }

    /** 取消所有与同步相关的任务。 */
    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(UNIQUE_PERIODIC_WORK)
        WorkManager.getInstance(context).cancelAllWorkByTag(UNIQUE_ONETIME_WORK)
    }
}