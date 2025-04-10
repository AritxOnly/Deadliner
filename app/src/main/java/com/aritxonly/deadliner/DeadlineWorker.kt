package com.aritxonly.deadliner

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime

class DeadlineWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val databaseHelper = DatabaseHelper.getInstance(applicationContext)

    override suspend fun doWork(): Result {
        Log.d("DeadlineWorker", "Checking deadlines for notifications...")
        checkDeadlinesAndNotify(applicationContext)
        return Result.success()
    }

    private fun checkDeadlinesAndNotify(context: Context) {
        val allDDLs = databaseHelper.getAllDDLs()
        val now = LocalDateTime.now()

        for (ddl in allDDLs) {
            val endTime = GlobalUtils.safeParseDateTime(ddl.endTime)
            val remainingMinutes = Duration.between(now, endTime).toMinutes()

            if (remainingMinutes in 0..60 && !ddl.isCompleted) {
                Log.d("DeadlineWorker", "${ddl.name} remaining $remainingMinutes")
                sendDeadlineNotification(context, ddl.name)
            }
        }
    }
}