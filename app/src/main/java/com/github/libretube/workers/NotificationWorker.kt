package com.github.libretube.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.constants.PUSH_CHANNEL_ID
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.extensions.toID
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.views.TimePickerPreference
import com.github.libretube.util.PreferenceHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.time.LocalTime

/**
 * The notification worker which checks for new streams in a certain frequency
 */
class NotificationWorker(appContext: Context, parameters: WorkerParameters) :
    Worker(appContext, parameters) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // the id where notification channels start
    private var notificationId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        notificationManager.activeNotifications.size + 5
    } else {
        5
    }

    override fun doWork(): Result {
        if (!checkTime()) Result.success()
        // check whether there are new streams and notify if there are some
        val result = checkForNewStreams()
        // return success if the API request succeeded
        return if (result) Result.success() else Result.retry()
    }

    /**
     * Determine whether the time is valid to notify
     */
    private fun checkTime(): Boolean {
        if (!PreferenceHelper.getBoolean(
                PreferenceKeys.NOTIFICATION_TIME_ENABLED,
                false
            )
        ) {
            return true
        }

        val start = getTimePickerPref(PreferenceKeys.NOTIFICATION_START_TIME)
        val end = getTimePickerPref(PreferenceKeys.NOTIFICATION_END_TIME)

        val currentTime = LocalTime.now()
        val isOverNight = start > end

        val startValid = if (isOverNight) start > currentTime else start < currentTime
        val endValid = if (isOverNight) end < currentTime else start > currentTime

        return (startValid && endValid)
    }

    private fun getTimePickerPref(key: String): LocalTime {
        return LocalTime.parse(
            PreferenceHelper.getString(key, TimePickerPreference.DEFAULT_VALUE)
        )
    }

    /**
     * check whether new streams are available in subscriptions
     */
    private fun checkForNewStreams(): Boolean {
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

            // hide for notifications unsubscribed channels
            val channelsToIgnore = PreferenceHelper.getIgnorableNotificationChannels()
            val filteredVideos = newVideos.filter {
                channelsToIgnore.none { channelId ->
                    channelId == it.uploaderUrl?.toID()
                }
            }

            // group the new streams by the uploader
            val channelGroups = filteredVideos.groupBy { it.uploaderUrl }
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
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, PUSH_CHANNEL_ID)
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

        with(NotificationManagerCompat.from(applicationContext)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }
    }
}
