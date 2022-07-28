package com.github.libretube.util

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(appContext: Context, parameters: WorkerParameters) : Worker(appContext, parameters) {
    private val TAG = "NotificationWorker"

    override fun doWork(): Result {
        Log.e(TAG, "working")
        NotificationHelper.checkForNewStreams(applicationContext)
        NotificationHelper.enqueueWork(applicationContext)
        return Result.success()
    }
}
