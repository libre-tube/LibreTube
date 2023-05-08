package com.github.libretube.workers

import android.app.NotificationManager
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.libretube.R
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PUSH_CHANNEL_ID
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.ImageHelper
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
    private val notificationManager = appContext.getSystemService<NotificationManager>()!!

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
        if (!PreferenceHelper.getBoolean(PreferenceKeys.NOTIFICATION_TIME_ENABLED, false)) {
            return true
        }

        val start = getTimePickerPref(PreferenceKeys.NOTIFICATION_START_TIME)
        val end = getTimePickerPref(PreferenceKeys.NOTIFICATION_END_TIME)
        val currentTime = LocalTime.now()

        return if (start > end) {
            currentTime !in end..start
        } else {
            currentTime in start..end
        }
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
        }.filter {
            PreferenceHelper.getBoolean(PreferenceKeys.SHORTS_NOTIFICATIONS, false) || !it.isShort
        }

        val lastSeenStreamId = PreferenceHelper.getLastSeenVideoId()
        val latestFeedStreamId = videoFeed.firstOrNull()?.url?.toID() ?: return true

        // first time notifications are enabled or no new video available
        if (lastSeenStreamId.isEmpty() || lastSeenStreamId == latestFeedStreamId) {
            PreferenceHelper.setLatestVideoId(lastSeenStreamId)
            return true
        }

        val channelsToIgnore = PreferenceHelper.getIgnorableNotificationChannels()

        val channelGroups = videoFeed.asSequence()
            // filter the new videos until the last seen video in the feed
            .takeWhile { it.url!!.toID() != lastSeenStreamId }
            // hide for notifications unsubscribed channels
            .filter { it.uploaderUrl!!.toID() !in channelsToIgnore }
            // group the new streams by the uploader
            .groupBy { it.uploaderUrl!!.toID() }

        // return if the previous video didn't get found or all the channels have notifications disabled
        if (channelGroups.isEmpty()) return true

        Log.d(TAG(), "Create notifications for new videos")

        // create a notification for each new stream
        channelGroups.forEach { (channelId, streams) ->
            createNotificationsForChannel(channelId, streams)
        }
        // save the latest streams that got notified about
        PreferenceHelper.setLatestVideoId(videoFeed.first().url!!.toID())
        // return whether the work succeeded
        return true
    }

    /**
     * Group of notifications created when new streams are found in a given channel.
     *
     * For more information, see https://developer.android.com/develop/ui/views/notifications/group
     */
    private suspend fun createNotificationsForChannel(group: String, streams: List<StreamItem>) {
        // Create stream notifications. These are automatically grouped on Android 7.0 and later.
        if (streams.size == 1) {
            showStreamNotification(group, streams[0], true)
        } else {
            streams.forEach {
                showStreamNotification(group, it, false)
            }

            val notificationId = group.hashCode()
            val intent = Intent(applicationContext, MainActivity::class.java)
                .setFlags(INTENT_FLAGS)
                .putExtra(IntentData.channelId, group)
            val pendingIntent = PendingIntentCompat
                .getActivity(applicationContext, notificationId, intent, FLAG_UPDATE_CURRENT, false)

            // Create summary notification containing new streams for Android versions below 7.0.
            val newStreams = applicationContext.resources
                .getQuantityString(R.plurals.channel_new_streams, streams.size, streams.size)
            val summary = NotificationCompat.InboxStyle()
            streams.forEach {
                summary.addLine(it.title)
            }
            val summaryNotification = createNotificationBuilder(group)
                .setContentTitle(streams[0].uploaderName)
                .setContentText(newStreams)
                // The intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setGroupSummary(true)
                .setStyle(summary)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .build()

            notificationManager.notify(notificationId, summaryNotification)
        }
    }

    private suspend fun showStreamNotification(
        group: String,
        stream: StreamItem,
        isSingleNotification: Boolean
    ) {
        val videoId = stream.url!!.toID()
        val intent = Intent(applicationContext, MainActivity::class.java)
            .setFlags(INTENT_FLAGS)
            .putExtra(IntentData.videoId, videoId)
        val notificationId = videoId.hashCode()
        val pendingIntent = PendingIntentCompat
            .getActivity(applicationContext, notificationId, intent, FLAG_UPDATE_CURRENT, false)

        val notificationBuilder = createNotificationBuilder(group)
            .setContentTitle(stream.title)
            .setContentText(stream.uploaderName)
            // The intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setSilent(!isSingleNotification)

        // Load stream thumbnails if the relevant toggle is enabled.
        if (PreferenceHelper.getBoolean(PreferenceKeys.SHOW_STREAM_THUMBNAILS, false)) {
            val thumbnail = withContext(Dispatchers.IO) {
                ImageHelper.getImage(applicationContext, stream.thumbnail).drawable?.toBitmap()
            }

            notificationBuilder
                .setLargeIcon(thumbnail)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(thumbnail)
                        .bigLargeIcon(null as Bitmap?) // Hides the icon when expanding
                )
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationBuilder(group: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, PUSH_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(group)
    }

    companion object {
        private const val INTENT_FLAGS = Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
}
