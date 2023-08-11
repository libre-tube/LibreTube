package com.github.libretube.workers

import android.app.Notification
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * The notification worker which checks for new streams in a certain frequency
 */
class NotificationWorker(appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {
    private val notificationManager = appContext.getSystemService<NotificationManager>()!!

    override suspend fun doWork(): Result {
        if (!checkTime()) return Result.success()
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
        }

        val lastUserSeenVideoId = PreferenceHelper.getLastSeenVideoId()
        val mostRecentStreamId = videoFeed.firstOrNull()?.url?.toID() ?: return true
        // save the latest streams that got notified about
        PreferenceHelper.setLastSeenVideoId(mostRecentStreamId)

        // first time notifications are enabled or no new video available
        if (lastUserSeenVideoId.isEmpty() || lastUserSeenVideoId == mostRecentStreamId) return true

        val channelsToIgnore = PreferenceHelper.getIgnorableNotificationChannels()
        val enableShortsNotification =
            PreferenceHelper.getBoolean(PreferenceKeys.SHORTS_NOTIFICATIONS, false)

        val channelGroups = videoFeed.asSequence()
            // filter the new videos until the last seen video in the feed
            .takeWhile { it.url!!.toID() != lastUserSeenVideoId }
            // don't show notifications for shorts videos if not enabled
            .filter { enableShortsNotification || !it.isShort }
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
        // return whether the work succeeded
        return true
    }

    /**
     * Group of notifications created when new streams are found in a given channel.
     *
     * For more information, see https://developer.android.com/develop/ui/views/notifications/group
     */
    private suspend fun createNotificationsForChannel(group: String, streams: List<StreamItem>) {
        val summaryId = group.hashCode()
        val intent = Intent(applicationContext, MainActivity::class.java)
            .setFlags(INTENT_FLAGS)
            .putExtra(IntentData.channelId, group)
        val pendingIntent = PendingIntentCompat
            .getActivity(applicationContext, summaryId, intent, FLAG_UPDATE_CURRENT, false)

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
            // Show channel avatar on Android versions below 7.0.
            .setLargeIcon(downloadImage(streams[0].uploaderAvatar))
            .build()

        // Create stream notifications. These are automatically grouped on Android 7.0 and later.
        val notificationsAndIds = withContext(Dispatchers.IO) {
            streams.map { async { createStreamNotification(group, it) } }
                .awaitAll()
                .sortedBy { (_, uploaded, _) -> uploaded }
                .map { (notificationId, _, notification) -> notificationId to notification }
        } + (summaryId to summaryNotification)
        notificationsAndIds.forEach { (notificationId, notification) ->
            notificationManager.notify(notificationId, notification)
        }
    }

    private suspend fun createStreamNotification(
        group: String,
        stream: StreamItem
    ): Triple<Int, Long?, Notification> { // Notification ID, uploaded date and notification object
        val videoId = stream.url!!.toID()
        val intent = Intent(applicationContext, MainActivity::class.java)
            .setFlags(INTENT_FLAGS)
            .putExtra(IntentData.videoId, videoId)
        val notificationId = videoId.hashCode()
        val pendingIntent = PendingIntentCompat
            .getActivity(applicationContext, notificationId, intent, FLAG_UPDATE_CURRENT, false)

        // Load stream thumbnails if the relevant toggle is enabled.
        val thumbnail = downloadImage(stream.thumbnail)

        val notificationBuilder = createNotificationBuilder(group)
            .setContentTitle(stream.title)
            .setContentText(stream.uploaderName)
            // The intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setLargeIcon(thumbnail)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(thumbnail)
                    .bigLargeIcon(null as Bitmap?) // Hides the icon when expanding
            )

        return Triple(notificationId, stream.uploaded, notificationBuilder.build())
    }

    private suspend fun downloadImage(url: String?): Bitmap? {
        return if (PreferenceHelper.getBoolean(PreferenceKeys.SHOW_STREAM_THUMBNAILS, false)) {
            ImageHelper.getImage(applicationContext, url).drawable?.toBitmap()
        } else {
            null
        }
    }

    private fun createNotificationBuilder(group: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, PUSH_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(group)
            .setCategory(Notification.CATEGORY_SOCIAL)
    }

    companion object {
        private const val INTENT_FLAGS = Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
}
