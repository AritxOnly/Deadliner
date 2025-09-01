package com.aritxonly.deadliner.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aritxonly.deadliner.AppSingletons
import com.aritxonly.deadliner.localutils.GlobalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(appContext: Context, params: WorkerParameters)
    : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // 开关保护 & 凭据完整性校验
        if (!GlobalUtils.cloudSyncEnable ||
            GlobalUtils.webDavBaseUrl.isBlank() ||
            GlobalUtils.webDavUser.isBlank() ||
            GlobalUtils.webDavPass.isBlank()
        ) {
            return@withContext Result.success()
        }

        val ok = runCatching { AppSingletons.sync.syncOnce() }.getOrDefault(false)
        if (ok) Result.success() else Result.retry()
    }
}