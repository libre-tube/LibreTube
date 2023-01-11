package com.github.libretube.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.github.libretube.constants.NOTIFICATION_WORK_NAME
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.workers.NotificationWorker
import java.util.concurrent.TimeUnit

object NotificationHelper {
    /**
     * Enqueue the work manager task
     */
    fun enqueueWork(
        context: Context,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy
    ) {
        // get the notification preferences
        PreferenceHelper.initialize(context)
        val notificationsEnabled = PreferenceHelper.getBoolean(
            PreferenceKeys.NOTIFICATION_ENABLED,
            true
        )

        val checkingFrequency = PreferenceHelper.getString(
            PreferenceKeys.CHECKING_FREQUENCY,
            "60"
        ).toLong()

        // schedule the work manager request if logged in and notifications enabled
        if (!notificationsEnabled) {
            // cancel the work if notifications are disabled or the user is not logged in
            WorkManager.getInstance(context)
                .cancelUniqueWork(NOTIFICATION_WORK_NAME)
            return
        }

        // required network type for the work
        val networkType = when (
            PreferenceHelper.getString(PreferenceKeys.REQUIRED_NETWORK, "all")
        ) {
            "all" -> NetworkType.CONNECTED
            "wifi" -> NetworkType.UNMETERED
            "metered" -> NetworkType.METERED
            else -> NetworkType.CONNECTED
        }

        // requirements for the work
        // here: network needed to run the task
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()

        // create the worker
        val notificationWorker = PeriodicWorkRequest.Builder(
            NotificationWorker::class.java,
            checkingFrequency,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        // enqueue the task to the work manager instance
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                NOTIFICATION_WORK_NAME,
                existingPeriodicWorkPolicy,
                notificationWorker
            )

        // for testing the notifications by the work manager
        /*
        WorkManager.getInstance(context)
            .enqueue(
                OneTimeWorkRequest.Builder(NotificationWorker::class.java)
                    .setConstraints(constraints)
                    .build()
            )
        */
    }
}
