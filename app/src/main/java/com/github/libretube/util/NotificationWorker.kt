package com.github.libretube.util

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * The notification worker which checks for new streams in a certain frequency
 */
class NotificationWorker(appContext: Context, parameters: WorkerParameters) : Worker(appContext, parameters) {
    private val TAG = "NotificationWorker"

    override fun doWork(): Result {
        // schedule the next task of the worker
        NotificationHelper.enqueueWork(applicationContext)
        // check whether there are new streams and notify if there are some
        NotificationHelper.checkForNewStreams(applicationContext)
        return Result.success()
    }
}
