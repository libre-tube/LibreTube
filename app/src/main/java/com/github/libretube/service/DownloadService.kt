package com.github.libretube.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.github.libretube.R
import java.io.File

var IS_DOWNLOAD_RUNNING = false
private const val TAG = "DownloadService"

class DownloadService : Service() {
    private lateinit var videoId: String
    private lateinit var videoUrl: String
    private lateinit var audioUrl: String
    private lateinit var extension: String
    private var downloadId: Long = -1
    private var duration: Int = 0

    //private lateinit var command: String
    private lateinit var audioDir: File
    private lateinit var videoDir: File
    lateinit var service: NotificationManager
    lateinit var notification: NotificationCompat.Builder
    override fun onCreate() {
        super.onCreate()
        IS_DOWNLOAD_RUNNING = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        videoId = intent?.getStringExtra("videoId")!!
        videoUrl = intent.getStringExtra("videoUrl")!!
        audioUrl = intent.getStringExtra("audioUrl")!!
        extension = intent.getStringExtra("extension")!!
        //command = intent.getStringExtra("command")!!
        duration = intent.getIntExtra("duration", 1)
        service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val chan = NotificationChannel(
                    "service",
                    "DownloadService", NotificationManager.IMPORTANCE_NONE
                )
                chan.lightColor = Color.BLUE
                chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                service.createNotificationChannel(chan)
                "service"
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }
        val pendingIntent: PendingIntent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

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
        startForeground(1, notification.build())
        downloadManager()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun downloadManager() {
        val path = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val folderMain = ".tmp"
        val f = File(path, folderMain)
        if (!f.exists()) {
            f.mkdirs()
            Log.e(TAG, "Directory make")
        } else {
            f.deleteRecursively()
            f.mkdirs()
            Log.e(TAG, "Directory already have")
        }
        audioDir = File(f, "$videoId-audio")
        videoDir = File(f, "$videoId-video")
        try {
            Log.e(TAG, "Directory make")
            registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
            val request: DownloadManager.Request =
                DownloadManager.Request(Uri.parse(videoUrl))
                    .setTitle("Video")
                    .setDescription("Downloading")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(videoDir))
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
            val downloadManager: DownloadManager =
                applicationContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)
            if (audioUrl == "") {
                downloadId = 0L
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "download error $e")
            try {
                downloadId = 0L
                val request: DownloadManager.Request =
                    DownloadManager.Request(Uri.parse(audioUrl))
                        .setTitle("Audio")
                        .setDescription("Downloading")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                        .setDestinationUri(Uri.fromFile(audioDir))
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)
                val downloadManager: DownloadManager =
                    applicationContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(request)

            } catch (e: Exception) {
                Log.e(TAG, "audio download error $e")
                stopService(Intent(this, DownloadService::class.java))
            }
        }
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadId == id) {
                downloadId = 0L
                try {
                    val request: DownloadManager.Request =
                        DownloadManager.Request(Uri.parse(audioUrl))
                            .setTitle("Audio")
                            .setDescription("Downloading")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                            .setDestinationUri(Uri.fromFile(audioDir))
                            .setAllowedOverMetered(true)
                            .setAllowedOverRoaming(true)
                    val downloadManager: DownloadManager =
                        applicationContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    downloadManager.enqueue(request)
                } catch (e: Exception) {
                }
            } else if (downloadId == 0L) {
                val libreTube = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "LibreTube"
                )
                if (!libreTube.exists()) {
                    libreTube.mkdirs()
                    Log.e(TAG, "libreTube Directory make")
                } else {
                    Log.e(TAG, "libreTube Directory already have")
                }
                val command: String = when {
                    videoUrl == "" -> {
                        "-y -i $audioDir -c copy ${libreTube}/${videoId}-audio$extension"
                    }
                    audioUrl == "" -> {
                        "-y -i $videoDir -c copy ${libreTube}/${videoId}-video$extension"
                    }
                    else -> {
                        "-y -i $videoDir -i $audioDir -c copy ${libreTube}/${videoId}$extension"
                    }
                }
                notification.setContentTitle("Muxing")
                FFmpegKit.executeAsync(command,
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
                        val path =
                            applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        val folderMain = ".tmp"
                        val f = File(path, folderMain)
                        f.deleteRecursively()
                        if (returnCode.toString() != "0") {
                            val builder = NotificationCompat.Builder(this@DownloadService, "failed")
                                .setSmallIcon(R.drawable.ic_download)
                                .setContentTitle(resources.getString(R.string.downloadfailed))
                                .setContentText("failure")
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                            createNotificationChannel()
                            with(NotificationManagerCompat.from(this@DownloadService)) {
                                // notificationId is a unique int for each notification that you must define
                                notify(69, builder.build())
                            }
                        }
                        stopForeground(true)
                        stopService(Intent(this@DownloadService, DownloadService::class.java))
                    }, {
                        // CALLED WHEN SESSION PRINTS LOGS
                        Log.e(TAG, it.message.toString())
                    }) {
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
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "failed"
            val descriptionText = "Download Failed"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("failed", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(onDownloadComplete)
        } catch (e: Exception) {
            // TODO: Handle exception
        }
        IS_DOWNLOAD_RUNNING = false
        Log.d(TAG, "dl finished!")
        super.onDestroy()
    }
}
