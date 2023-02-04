package com.github.libretube.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.libretube.R
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.compat.PendingIntentCompat
import com.github.libretube.constants.DOWNLOAD_PROGRESS_NOTIFICATION_ID
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PUSH_CHANNEL_ID
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.views.TimePickerPreference
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The notification worker which checks for new streams in a certain frequency
 */
class NotificationWorker(appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {

    private val notificationManager = NotificationManagerCompat.from(appContext)

    // the id where notification channels start
    private var notificationId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val nManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nManager.activeNotifications.size + DOWNLOAD_PROGRESS_NOTIFICATION_ID
    } else {
        DOWNLOAD_PROGRESS_NOTIFICATION_ID
    }

    override suspend fun doWork(): Result {
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
    private suspend fun checkForNewStreams(): Boolean {
        Log.d(TAG(), "Work manager started")

        // fetch the users feed
        val videoFeed = try {
            withContext(Dispatchers.IO) {
                SubscriptionHelper.getFeed()
            }
        } catch (e: Exception) {
            return false
        }

        val lastSeenStreamId = PreferenceHelper.getLastSeenVideoId()
        val latestFeedStreamId = videoFeed.firstOrNull()?.url?.toID() ?: return true

        // first time notifications are enabled or no new video available
        if (lastSeenStreamId == "" || lastSeenStreamId == latestFeedStreamId) {
            PreferenceHelper.setLatestVideoId(lastSeenStreamId)
            return true
        }

        // filter the new videos until the last seen video in the feed
        val newStreams = videoFeed.takeWhile { it.url!!.toID() != lastSeenStreamId }

        // return if the previous video didn't get found
        if (newStreams.isEmpty()) return true

        // hide for notifications unsubscribed channels
        val channelsToIgnore = PreferenceHelper.getIgnorableNotificationChannels()
        val filteredVideos = newStreams.filter {
            channelsToIgnore.none { channelId ->
                channelId == it.uploaderUrl?.toID()
            }
        }

        // group the new streams by the uploader
        val channelGroups = filteredVideos.groupBy { it.uploaderUrl }

        Log.d(TAG(), "Create notifications for new videos")

        // create a notification for each new stream
        channelGroups.forEach { (_, streams) ->
            createNotification(
                group = streams.first().uploaderUrl!!.toID(),
                title = streams.first().uploaderName.toString(),
                urlPath = streams.first().uploaderUrl!!,
                isGroupSummary = true
            )

            streams.forEach { streamItem ->
                createNotification(
                    title = streamItem.title.toString(),
                    description = streamItem.uploaderName.toString(),
                    urlPath = streamItem.url!!,
                    group = streamItem.uploaderUrl!!.toID()
                )
            }
        }
        // save the latest streams that got notified about
        PreferenceHelper.setLatestVideoId(videoFeed.first().url!!.toID())
        // return whether the work succeeded
        return true
    }

    /**
     * Notification that is created when new streams are found
     */
    private fun createNotification(
        title: String,
        group: String,
        urlPath: String,
        description: String? = null,
        isGroupSummary: Boolean = false
    ) {
        // increase the notification ID to guarantee uniqueness
        notificationId += 1

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (isGroupSummary) {
                putExtra(IntentData.channelId, urlPath.toID())
            } else {
                putExtra(IntentData.videoId, urlPath.toID())
            }
        }

        val pendingIntent = PendingIntentCompat.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(applicationContext, PUSH_CHANNEL_ID)
            .setContentTitle(title)
            .setGroup(group)
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // The intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isGroupSummary) {
            builder.setGroupSummary(true)
        } else {
            builder.setContentText(description)
        }

        // [notificationId] is a unique int for each notification that you must define
        notificationManager.notify(notificationId, builder.build())
    }
}
