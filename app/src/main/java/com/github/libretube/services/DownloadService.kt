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
import com.arthenica.ffmpegkit.FFmpegKit
import com.github.libretube.R
import com.github.libretube.obj.DownloadType
import com.github.libretube.preferences.PreferenceHelper
import java.io.File

var IS_DOWNLOAD_RUNNING = false

class DownloadService : Service() {
    val TAG = "DownloadService"

    private lateinit var notification: NotificationCompat.Builder

    private var downloadId: Long = -1
    private lateinit var videoId: String
    private lateinit var videoUrl: String
    private lateinit var audioUrl: String
    private lateinit var extension: String
    private var duration: Int = 0
    private var downloadType: Int = 3

    private lateinit var audioDir: File
    private lateinit var videoDir: File
    private lateinit var libretubeDir: File
    private lateinit var tempDir: File
    override fun onCreate() {
        super.onCreate()
        IS_DOWNLOAD_RUNNING = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        videoId = intent?.getStringExtra("videoId")!!
        videoUrl = intent.getStringExtra("videoUrl")!!
        audioUrl = intent.getStringExtra("audioUrl")!!
        duration = intent.getIntExtra("duration", 1)
        extension = PreferenceHelper.getString("video_format", ".mp4")!!
        downloadType = if (audioUrl != "" && videoUrl != "") DownloadType.MUX
        else if (audioUrl != "") DownloadType.AUDIO
        else if (videoUrl != "") DownloadType.VIDEO
        else DownloadType.NONE
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
            Log.e(TAG, "Directory make")
        } else {
            tempDir.deleteRecursively()
            tempDir.mkdirs()
            Log.e(TAG, "Directory already have")
        }

        val downloadLocationPref = PreferenceHelper.getString("download_location", "")
        val folderName = PreferenceHelper.getString("download_folder", "LibreTube")

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
        Log.i(TAG, libretubeDir.toString())

        // start download
        try {
            registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
            when (downloadType) {
                DownloadType.MUX -> {
                    audioDir = File(tempDir, "$videoId-audio")
                    videoDir = File(tempDir, "$videoId-video")
                    downloadId = downloadManagerRequest(
                        getString(R.string.video),
                        getString(R.string.downloading),
                        videoUrl,
                        videoDir
                    )
                }
                DownloadType.VIDEO -> {
                    videoDir = File(libretubeDir, "$videoId-video")
                    downloadId = downloadManagerRequest(
                        getString(R.string.video),
                        getString(R.string.downloading),
                        videoUrl,
                        videoDir
                    )
                }
                DownloadType.AUDIO -> {
                    audioDir = File(libretubeDir, "$videoId-audio")
                    downloadId = downloadManagerRequest(
                        getString(R.string.audio),
                        getString(R.string.downloading),
                        audioUrl,
                        audioDir
                    )
                }
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "download error $e")
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
            } else {
                muxDownloadedMedia()
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
        var pendingIntent: PendingIntent? = null
        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }
        // Creating a notification and setting its various attributes
        notification =
            NotificationCompat.Builder(this@DownloadService, "download_service")
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("LibreTube")
                .setContentText(getString(R.string.downloading))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
        startForeground(2, notification.build())
    }

    private fun downloadFailedNotification() {
        val builder = NotificationCompat.Builder(this@DownloadService, "download_service")
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(resources.getString(R.string.downloadfailed))
            .setContentText(getString(R.string.fail))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        with(NotificationManagerCompat.from(this@DownloadService)) {
            // notificationId is a unique int for each notification that you must define
            notify(3, builder.build())
        }
    }

    private fun downloadSucceededNotification() {
        Log.i(TAG, "Download succeeded")
        val builder = NotificationCompat.Builder(this@DownloadService, "download_service")
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(resources.getString(R.string.success))
            .setContentText(getString(R.string.fail))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        with(NotificationManagerCompat.from(this@DownloadService)) {
            // notificationId is a unique int for each notification that you must define
            notify(4, builder.build())
        }
    }

    private fun muxDownloadedMedia() {
        val command = "-y -i $videoDir -i $audioDir -c copy $libretubeDir/${videoId}$extension"
        notification.setContentTitle("Muxing")
        FFmpegKit.executeAsync(
            command,
            { session ->
                val state = session.state
                val returnCode = session.returnCode
                // CALLED WHEN SESSION IS EXECUTED
                Log.d(
                    TAG,
                    String.format(
                        "FFmpeg process exited with state %s and rc %s.%s",
                        state,
                        returnCode,
                        session.failStackTrace
                    )
                )
                tempDir.deleteRecursively()
                if (returnCode.toString() != "0") downloadFailedNotification()
                else downloadSucceededNotification()
                onDestroy()
            },
            {
                // CALLED WHEN SESSION PRINTS LOGS
                Log.e(TAG, it.message.toString())
            }
        ) {
            // CALLED WHEN SESSION GENERATES STATISTICS
            Log.e(TAG + "stat", it.time.toString())
            /*val progress = it.time/(10*duration!!)
            if (progress<1){
                notification
                    .setProgress(progressMax, progress.toInt(), false)
                service.notify(1,notification.build())
            }*/
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(onDownloadComplete)
        } catch (e: Exception) {
        }

        IS_DOWNLOAD_RUNNING = false
        Log.d(TAG, "dl finished!")
        stopForeground(true)
        stopService(Intent(this@DownloadService, DownloadService::class.java))
        super.onDestroy()
    }
}
