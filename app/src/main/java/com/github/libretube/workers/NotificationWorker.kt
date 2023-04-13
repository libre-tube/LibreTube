package com.github.libretube.workers

import android.app.NotificationManager
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
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
import com.github.libretube.constants.DOWNLOAD_PROGRESS_NOTIFICATION_ID
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

    // the id where notification channels start
    private var notificationId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        notificationManager.activeNotifications.size + DOWNLOAD_PROGRESS_NOTIFICATION_ID
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

        val lastSeenStreamId = PreferenceHelper.getLastSeenVideoId()
        val latestFeedStreamId = videoFeed.firstOrNull()?.url?.toID() ?: return true

        // first time notifications are enabled or no new video available
        if (lastSeenStreamId.isEmpty() || lastSeenStreamId == latestFeedStreamId) {
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
        channelGroups.forEach { (uploaderUrl, streams) ->
            createNotificationsForChannel(uploaderUrl!!, streams)
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
        val intentFlags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TASK

        // Create stream notifications. These are automatically grouped on Android 7.0 and later.
        streams.forEach {
            val intent = Intent(applicationContext, MainActivity::class.java)
                .setFlags(intentFlags)
                .putExtra(IntentData.videoId, it.url!!.toID())
            val code = ++notificationId
            val pendingIntent = PendingIntentCompat
                .getActivity(applicationContext, code, intent, FLAG_UPDATE_CURRENT, false)

            val thumbnail = withContext(Dispatchers.IO) {
                ImageHelper.getImage(applicationContext, it.thumbnail).drawable?.toBitmap()
            }
            val notification = createNotificationBuilder(group)
                .setContentTitle(it.title)
                .setContentText(it.uploaderName)
                // The intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setLargeIcon(thumbnail)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(thumbnail)
                        .bigLargeIcon(null as Bitmap?) // Hides the icon when expanding
                )
                .build()

            notificationManager.notify(code, notification)
        }

        val summaryId = ++notificationId

        val intent = Intent(applicationContext, MainActivity::class.java)
            .setFlags(intentFlags)
            .putExtra(IntentData.channelId, group.toID())

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
            .build()

        notificationManager.notify(summaryId, summaryNotification)
    }

    private fun createNotificationBuilder(group: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, PUSH_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(group)
    }
}
