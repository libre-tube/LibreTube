package com.github.libretube.util

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(appContext: Context, parameters: WorkerParameters) : Worker(appContext, parameters) {
    private val TAG = "NotificationWorker"

    override fun doWork(): Result {
        NotificationHelper.enqueueWork(applicationContext)
        NotificationHelper.checkForNewStreams(applicationContext)
        return Result.success()
    }
}
