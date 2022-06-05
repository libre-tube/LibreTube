package com.github.libretube

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
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
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File

var IS_DOWNLOAD_RUNNING = false

class DownloadService : Service() {
    val TAG = "DownloadService"
    private var downloadId: Long = -1
    private lateinit var videoId: String
    private lateinit var videoUrl: String
    private lateinit var audioUrl: String
    private lateinit var extension: String
    private var duration: Int = 0

    private lateinit var audioDir: File
    private lateinit var videoDir: File
    private lateinit var notification: NotificationCompat.Builder
    private lateinit var downloadType: String
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
        extension = intent.getStringExtra("extension")!!
        duration = intent.getIntExtra("duration", 1)
        downloadType = if (audioUrl != "" && videoUrl != "") "mux"
        else if (audioUrl != "") "audio"
        else if (videoUrl != "") "video"
        else "none"
        if (downloadType != "none") {
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

        // create LibreTube folder in Downloads
        libretubeDir = File(
            Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS),
            "LibreTube"
        )
        if (!libretubeDir.exists()) libretubeDir.mkdirs()

        // start download
        try {
            registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
            when (downloadType) {
                "mux" -> {
                    audioDir = File(tempDir, "$videoId-audio")
                    videoDir = File(tempDir, "$videoId-video")
                    downloadId = downloadManagerRequest("Video", "Downloading", videoUrl, videoDir)
                }
                "video" -> {
                    videoDir = File(libretubeDir, "$videoId-video")
                    downloadId = downloadManagerRequest("Video", "Downloading", videoUrl, videoDir)
                }
                "audio" -> {
                    audioDir = File(libretubeDir, "$videoId-audio")
                    downloadId = downloadManagerRequest("Audio", "Downloading", audioUrl, audioDir)
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
                if (downloadType == "mux") {
                    downloadManagerRequest("Audio", "Downloading", audioUrl, audioDir)
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

    private fun createNotificationChannel(
        id: String,
        name: String,
        descriptionText: String,
        importance: Int
    ): String {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            id
        } else ""
    }

    private fun downloadNotification(intent: Intent) {
        // Creating the notification channel
        val channelId = createNotificationChannel(
            "service", "service", "DownloadService",
            NotificationManager.IMPORTANCE_NONE
        )

        var pendingIntent: PendingIntent? = null
        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }
        // Creating a notification and setting its various attributes
        notification =
            NotificationCompat.Builder(this@DownloadService, channelId)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("LibreTube")
                .setContentText("Downloading")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
        startForeground(2, notification.build())
    }

    private fun downloadFailedNotification() {
        val builder = NotificationCompat.Builder(this@DownloadService, "failed")
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(resources.getString(R.string.downloadfailed))
            .setContentText("failure")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        createNotificationChannel(
            "failed", "failed", "Download Failed",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        with(NotificationManagerCompat.from(this@DownloadService)) {
            // notificationId is a unique int for each notification that you must define
            notify(69, builder.build())
        }
    }

    private fun downloadSucceededNotification() {
        Log.i(TAG, "Download succeeded")
        val builder = NotificationCompat.Builder(this@DownloadService, "failed")
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(resources.getString(R.string.success))
            .setContentText("success")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        createNotificationChannel(
            "success", "success", "Download succeeded",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        with(NotificationManagerCompat.from(this@DownloadService)) {
            // notificationId is a unique int for each notification that you must define
            notify(70, builder.build())
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
            }, {
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
        } catch (e: Exception) { }

        IS_DOWNLOAD_RUNNING = false
        Log.d(TAG, "dl finished!")
        stopForeground(true)
        stopService(Intent(this@DownloadService, DownloadService::class.java))
        super.onDestroy()
    }
}
