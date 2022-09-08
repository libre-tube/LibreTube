package com.github.libretube.services

import android.app.DownloadManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.DIRECTORY_MOVIES
import android.os.Environment.DIRECTORY_MUSIC
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.libretube.constants.DOWNLOAD_CHANNEL_ID
import com.github.libretube.constants.DOWNLOAD_FAILURE_NOTIFICATION_ID
import com.github.libretube.constants.DOWNLOAD_PENDING_NOTIFICATION_ID
import com.github.libretube.constants.DOWNLOAD_SUCCESS_NOTIFICATION_ID
import com.github.libretube.Globals
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.extensions.TAG
import com.github.libretube.obj.DownloadType
import com.github.libretube.util.PreferenceHelper
import java.io.File

class DownloadService : Service() {

    private lateinit var notification: NotificationCompat.Builder

    private var downloadId: Long = -1
    private lateinit var videoName: String
    private lateinit var videoUrl: String
    private lateinit var audioUrl: String
    private var downloadType: Int = 3

    private lateinit var audioDir: File
    private lateinit var videoDir: File
    private lateinit var libretubeDir: File
    private lateinit var tempDir: File
    override fun onCreate() {
        super.onCreate()
        Globals.IS_DOWNLOAD_RUNNING = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        videoName = intent?.getStringExtra("videoName")!!
        videoUrl = intent.getStringExtra("videoUrl")!!
        audioUrl = intent.getStringExtra("audioUrl")!!

        downloadType = if (audioUrl != "") {
            DownloadType.AUDIO
        } else if (videoUrl != "") {
            DownloadType.VIDEO
        } else {
            DownloadType.NONE
        }
        if (downloadType != DownloadType.NONE) {
            downloadNotification(intent)
            downloadManager()
        } else {
            onDestroy()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun downloadManager() {
        // create folder for temporary files
        tempDir = File(
            applicationContext.getExternalFilesDir(DIRECTORY_DOWNLOADS),
            ".tmp"
        )
        if (!tempDir.exists()) {
            tempDir.mkdirs()
            Log.e(TAG(), "Directory make")
        } else {
            tempDir.deleteRecursively()
            tempDir.mkdirs()
            Log.e(TAG(), "Directory already have")
        }

        val downloadLocationPref = PreferenceHelper.getString(PreferenceKeys.DOWNLOAD_LOCATION, "")
        val folderName = PreferenceHelper.getString(PreferenceKeys.DOWNLOAD_FOLDER, "LibreTube")

        val location = when (downloadLocationPref) {
            "downloads" -> Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
            "music" -> Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC)
            "movies" -> Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES)
            "sdcard" -> Environment.getExternalStorageDirectory()
            else -> Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
        }

        libretubeDir = File(
            location,
            folderName
        )
        if (!libretubeDir.exists()) libretubeDir.mkdirs()
        Log.i(TAG(), libretubeDir.toString())

        // start download
        try {
            registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
            when (downloadType) {
                DownloadType.VIDEO -> {
                    videoDir = File(libretubeDir, videoName)
                    downloadId = downloadManagerRequest(
                        getString(R.string.video),
                        getString(R.string.downloading),
                        videoUrl,
                        videoDir
                    )
                }
                DownloadType.AUDIO -> {
                    audioDir = File(libretubeDir, videoName)
                    downloadId = downloadManagerRequest(
                        getString(R.string.audio),
                        getString(R.string.downloading),
                        audioUrl,
                        audioDir
                    )
                }
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG(), "download error $e")
            downloadFailedNotification()
        }
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Fetching the download id received with the broadcast
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            // Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadId == id) {
                if (downloadType == DownloadType.MUX) {
                    downloadManagerRequest(
                        getString(R.string.audio),
                        getString(R.string.downloading),
                        audioUrl,
                        audioDir
                    )
                } else {
                    downloadSucceededNotification()
                    onDestroy()
                }
            }
        }
    }

    private fun downloadManagerRequest(
        title: String,
        descriptionText: String,
        url: String,
        fileDir: File
    ): Long {
        val request: DownloadManager.Request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle(title) // Title of the Download Notification
                .setDescription(descriptionText) // Description of the Download Notification
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // Visibility of the download Notification
                .setDestinationUri(Uri.fromFile(fileDir))
                .setAllowedOverMetered(true) // Set if download is allowed on Mobile network
                .setAllowedOverRoaming(true) //
        val downloadManager: DownloadManager =
            applicationContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }

    private fun downloadNotification(intent: Intent) {
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }
        // Creating a notification and setting its various attributes
        notification =
            NotificationCompat.Builder(this@DownloadService, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("LibreTube")
                .setContentText(getString(R.string.downloading))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
        startForeground(DOWNLOAD_PENDING_NOTIFICATION_ID, notification.build())
    }

    private fun downloadFailedNotification() {
        val builder = NotificationCompat.Builder(this@DownloadService, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(resources.getString(R.string.downloadfailed))
            .setContentText(getString(R.string.fail))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        with(NotificationManagerCompat.from(this@DownloadService)) {
            // notificationId is a unique int for each notification that you must define
            notify(DOWNLOAD_FAILURE_NOTIFICATION_ID, builder.build())
        }
    }

    private fun downloadSucceededNotification() {
        Log.i(TAG(), "Download succeeded")
        val builder = NotificationCompat.Builder(this@DownloadService, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(resources.getString(R.string.success))
            .setContentText(getString(R.string.downloadsucceeded))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        with(NotificationManagerCompat.from(this@DownloadService)) {
            // notificationId is a unique int for each notification that you must define
            notify(DOWNLOAD_SUCCESS_NOTIFICATION_ID, builder.build())
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(onDownloadComplete)
        } catch (e: Exception) {
        }

        Globals.IS_DOWNLOAD_RUNNING = false
        Log.d(TAG(), "dl finished!")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopService(Intent(this@DownloadService, DownloadService::class.java))
        super.onDestroy()
    }
}
