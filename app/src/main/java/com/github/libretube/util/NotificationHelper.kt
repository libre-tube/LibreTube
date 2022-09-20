package com.github.libretube.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.github.libretube.R
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.constants.NOTIFICATION_WORK_NAME
import com.github.libretube.constants.PUSH_CHANNEL_ID
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.extensions.toID
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class NotificationHelper(
    private val context: Context
) {
    val NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // the id where notification channels start
    private var notificationId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        NotificationManager.activeNotifications.size + 5
    } else {
        5
    }

    /**
     * Enqueue the work manager task
     */
    fun enqueueWork(
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
        if (notificationsEnabled && PreferenceHelper.getToken() != "") {
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

            // enqueue the task
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    NOTIFICATION_WORK_NAME,
                    existingPeriodicWorkPolicy,
                    notificationWorker
                )
        } else {
            // cancel the work if notifications are disabled or the user is not logged in
            WorkManager.getInstance(context)
                .cancelUniqueWork(NOTIFICATION_WORK_NAME)
        }
    }

    /**
     * check whether new streams are available in subscriptions
     */
    fun checkForNewStreams(): Boolean {
        var success = true

        val token = PreferenceHelper.getToken()
        runBlocking {
            val task = async {
                if (token != "") {
                    RetrofitInstance.authApi.getFeed(token)
                } else {
                    RetrofitInstance.authApi.getUnauthenticatedFeed(
                        SubscriptionHelper.getFormattedLocalSubscriptions()
                    )
                }
            }
            // fetch the users feed
            val videoFeed = try {
                task.await()
            } catch (e: Exception) {
                success = false
                return@runBlocking
            }

            val lastSeenStreamId = PreferenceHelper.getLastSeenVideoId()
            val latestFeedStreamId = videoFeed[0].url!!.toID()

            // first time notifications enabled or no new video available
            if (lastSeenStreamId == "" || lastSeenStreamId == latestFeedStreamId) {
                PreferenceHelper.setLatestVideoId(lastSeenStreamId)
                return@runBlocking
            }

            // filter the new videos out
            val lastSeenStreamItem = videoFeed.filter { it.url!!.toID() == lastSeenStreamId }

            // previous video not found
            if (lastSeenStreamItem.isEmpty()) return@runBlocking

            val lastStreamIndex = videoFeed.indexOf(lastSeenStreamItem[0])
            val newVideos = videoFeed.filterIndexed { index, _ ->
                index < lastStreamIndex
            }

            // group the new streams by the uploader
            val channelGroups = newVideos.groupBy { it.uploaderUrl }
            // create a notification for each new stream
            channelGroups.forEach { (_, streams) ->
                createNotification(
                    group = streams[0].uploaderUrl!!.toID(),
                    title = streams[0].uploaderName.toString(),
                    isSummary = true
                )

                streams.forEach { streamItem ->
                    notificationId += 1
                    createNotification(
                        title = streamItem.title.toString(),
                        description = streamItem.uploaderName.toString(),
                        group = streamItem.uploaderUrl!!.toID()
                    )
                }
            }
            // save the latest streams that got notified about
            PreferenceHelper.setLatestVideoId(videoFeed[0].url!!.toID())
        }
        // return whether the work succeeded
        return success
    }

    /**
     * Notification that is created when new streams are found
     */
    private fun createNotification(
        title: String,
        group: String,
        description: String? = null,
        isSummary: Boolean = false
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, PUSH_CHANNEL_ID)
            .setContentTitle(title)
            .setGroup(group)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isSummary) {
            builder.setGroupSummary(true)
        } else {
            builder.setContentText(description)
        }

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }
    }
}
