package com.github.libretube.services

import android.app.DownloadManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.libretube.R
import com.github.libretube.constants.DOWNLOAD_CHANNEL_ID
import com.github.libretube.constants.DOWNLOAD_FAILURE_NOTIFICATION_ID
import com.github.libretube.constants.DOWNLOAD_SUCCESS_NOTIFICATION_ID
import com.github.libretube.constants.DownloadType
import com.github.libretube.extensions.TAG
import com.github.libretube.util.DownloadHelper
import java.io.File

class DownloadService : Service() {

    private lateinit var videoName: String
    private lateinit var videoUrl: String
    private lateinit var audioUrl: String
    private var downloadType: Int = 3
    private var videoDownloadId: Long? = null
    private var audioDownloadId: Long? = null

    override fun onCreate() {
        super.onCreate()
        IS_DOWNLOAD_RUNNING = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        videoName = intent?.getStringExtra("videoName")!!
        videoUrl = intent.getStringExtra("videoUrl")!!
        audioUrl = intent.getStringExtra("audioUrl")!!

        downloadType = when {
            videoUrl != "" && audioUrl != "" -> DownloadType.AUDIO_VIDEO
            audioUrl != "" -> DownloadType.AUDIO
            videoUrl != "" -> DownloadType.VIDEO
            else -> DownloadType.NONE
        }

        if (downloadType != DownloadType.NONE) {
            downloadManager(videoName)
        } else {
            onDestroy()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun downloadManager(videoName: String) {
        // initialize and create the directories to download into

        val videoDownloadDir = DownloadHelper.getVideoDir(this)
        val audioDownloadDir = DownloadHelper.getAudioDir(this)

        // start download
        try {
            registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
            if (downloadType in listOf(DownloadType.VIDEO, DownloadType.AUDIO_VIDEO)) {
                videoDownloadId = downloadManagerRequest(
                    "[Video] $videoName",
                    getString(R.string.downloading),
                    videoUrl,
                    Uri.fromFile(
                        File(videoDownloadDir, videoName)
                    )
                )
            }
            if (downloadType in listOf(DownloadType.AUDIO, DownloadType.AUDIO_VIDEO)) {
                audioDownloadId = downloadManagerRequest(
                    "[Audio] $videoName",
                    getString(R.string.downloading),
                    audioUrl,
                    Uri.fromFile(
                        File(audioDownloadDir, videoName)
                    )
                )
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG(), "download error $e")
            downloadFailedNotification()
        }
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Fetching the download id received with the broadcast
            // Checking if the received broadcast is for our enqueued download by matching download id
            when (
                intent.getLongExtra(
                    DownloadManager.EXTRA_DOWNLOAD_ID,
                    -1
                )
            ) {
                videoDownloadId -> videoDownloadId = null
                audioDownloadId -> audioDownloadId = null
            }

            if (audioDownloadId != null || videoDownloadId != null) return

            downloadSucceededNotification()
            onDestroy()
        }
    }

    private fun downloadManagerRequest(
        title: String,
        descriptionText: String,
        url: String,
        destination: Uri
    ): Long {
        val request: DownloadManager.Request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle(title) // Title of the Download Notification
                .setDescription(descriptionText) // Description of the Download Notification
                .setDestinationUri(destination)
                .setAllowedOverMetered(true) // Set if download is allowed on Mobile network
                .setAllowedOverRoaming(true)

        val downloadManager: DownloadManager =
            applicationContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }

    private fun downloadFailedNotification() {
        val builder = NotificationCompat.Builder(this, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(resources.getString(R.string.downloadfailed))
            .setContentText(getString(R.string.fail))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(DOWNLOAD_FAILURE_NOTIFICATION_ID, builder.build())
        }
    }

    private fun downloadSucceededNotification() {
        Log.i(TAG(), "Download succeeded")
        val builder = NotificationCompat.Builder(this, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(resources.getString(R.string.success))
            .setContentText(getString(R.string.downloadsucceeded))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(DOWNLOAD_SUCCESS_NOTIFICATION_ID, builder.build())
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(onDownloadComplete)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        IS_DOWNLOAD_RUNNING = false

        stopService(Intent(this, DownloadService::class.java))
        super.onDestroy()
    }

    companion object {
        var IS_DOWNLOAD_RUNNING = false
    }
}
