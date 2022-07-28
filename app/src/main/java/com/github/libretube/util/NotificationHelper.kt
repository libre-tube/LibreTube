package com.github.libretube.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

object NotificationHelper {
    fun enqueueWork(
        context: Context
    ) {
        PreferenceHelper.setContext(context)
        val notificationsEnabled = PreferenceHelper.getBoolean(
            PreferenceKeys.NOTIFICATION_ENABLED,
            true
        )

        val checkingFrequency = PreferenceHelper.getString(
            PreferenceKeys.CHECKING_FREQUENCY,
            "60"
        ).toLong()

        val uniqueWorkName = "NotificationService"

        if (notificationsEnabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val myWorkBuilder = PeriodicWorkRequest.Builder(
                NotificationWorker::class.java,
                checkingFrequency,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)

            val myWork = myWorkBuilder.build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    uniqueWorkName,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    myWork
                )
        } else {
            WorkManager.getInstance(context)
                .cancelUniqueWork(uniqueWorkName)
        }
    }

    /**
     * check whether new streams are available in subscriptions
     */
    fun checkForNewStreams(context: Context) {
        val token = PreferenceHelper.getToken()
        if (token == "") return
        runBlocking {
            val task = async {
                RetrofitInstance.authApi.getFeed(token)
            }
            // fetch the users feed
            val videoFeed = try {
                task.await()
            } catch (e: Exception) {
                return@runBlocking
            }
            val lastSeenStreamId = PreferenceHelper.getLatestVideoId()
            val latestFeedStreamId = videoFeed[0].url?.replace("/watch?v=", "")
            // first time notifications enabled
            if (lastSeenStreamId == "") PreferenceHelper.setLatestVideoId(lastSeenStreamId)
            else if (lastSeenStreamId != latestFeedStreamId) {
                // get the index of the last user-seen video
                var newStreamIndex = -1
                videoFeed.forEachIndexed { index, stream ->
                    if (stream.url?.replace("/watch?v=", "") == lastSeenStreamId) {
                        newStreamIndex = index
                    }
                }
                if (newStreamIndex == -1) return@runBlocking
                val (title, description) = when (newStreamIndex) {
                    // only one new stream available
                    1 -> {
                        Pair(videoFeed[0].title, videoFeed[0].uploaderName)
                    }
                    else -> {
                        Pair(
                            // return the amount of new streams as title
                            context.getString(
                                R.string.new_streams_count,
                                newStreamIndex.toString()
                            ),
                            // return the first few uploader as description
                            context.getString(
                                R.string.new_streams_by,
                                videoFeed[0].uploaderName + ", " + videoFeed[1].uploaderName + ", " + videoFeed[2].uploaderName
                            )
                        )
                    }
                }
                createNotification(context, title!!, description!!)
            }
        }
    }

    /**
     * Notification that is created when new streams are found
     */
    fun createNotification(context: Context, title: String, description: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, "notification_worker")
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(2, builder.build())
        }
    }
}
