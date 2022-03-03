package com.github.libretube

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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File

class DownloadService : Service(){
    val TAG = "DownloadService"
    private var downloadId: Long =0
    private lateinit var videoId: String
    private lateinit var videoUrl: String
    private lateinit var audioUrl: String
    private var duration: Int = 0
    //private lateinit var command: String
    private lateinit var audioDir: File
    private lateinit var videoDir: File
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        videoId = intent?.getStringExtra("videoId")!!
        videoUrl = intent.getStringExtra("videoUrl")!!
        audioUrl = intent.getStringExtra("audioUrl")!!
        //command = intent.getStringExtra("command")!!
        duration = intent.getIntExtra("duration",1)
        downloadManager()

        return super.onStartCommand(intent, flags, startId)
    }
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun downloadManager() {
        val path = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val folder_main = ".tmp"
        val f = File(path, folder_main)
        if (!f.exists()) {
            f.mkdirs()
            Log.e(TAG, "Directory make")
        } else {
            Log.e(TAG, "Directory already have")
        }
        audioDir = File(f, "$videoId-audio")
        videoDir = File(f, "$videoId-video")
        try {
            Log.e(TAG, "Directory make")
            val request: DownloadManager.Request =
                DownloadManager.Request(Uri.parse(videoUrl))
                    .setTitle("Video") // Title of the Download Notification
                    .setDescription("Downloading") // Description of the Download Notification
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // Visibility of the download Notification
                    .setDestinationUri(Uri.fromFile(videoDir))
                    .setAllowedOverMetered(true) // Set if download is allowed on Mobile network
                    .setAllowedOverRoaming(true) //
            val downloadManager: DownloadManager =
                applicationContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "download error $e")
        }
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //Fetching the download id received with the broadcast
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadId == id) {
                Toast.makeText(this@DownloadService, "Download Completed", Toast.LENGTH_SHORT)
                    .show()
                downloadId=0
                val request: DownloadManager.Request =
                    DownloadManager.Request(Uri.parse(audioUrl))
                        .setTitle("Audio") // Title of the Download Notification
                        .setDescription("Downloading") // Description of the Download Notification
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // Visibility of the download Notification
                        .setDestinationUri(Uri.fromFile(audioDir))
                        .setAllowedOverMetered(true) // Set if download is allowed on Mobile network
                        .setAllowedOverRoaming(true) //
                val downloadManager: DownloadManager =
                    applicationContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(request)

            }else if (downloadId == 0L){
                val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val chan = NotificationChannel("service",
                            "DownloadService", NotificationManager.IMPORTANCE_NONE)
                        chan.lightColor = Color.BLUE
                        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                        service.createNotificationChannel(chan)
                        "service"
                    } else {
                        // If earlier version channel ID is not used
                        // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                        ""
                    }
                //Toast.makeText(this,command, Toast.LENGTH_SHORT).show()
                val pendingIntent: PendingIntent = PendingIntent.getActivity(
                    this@DownloadService, 0, intent, 0)
                //Sets the maximum progress as 100
                val progressMax = 100
                //Creating a notification and setting its various attributes
                val notification =
                    NotificationCompat.Builder(this@DownloadService, channelId)
                        .setSmallIcon(R.drawable.ic_download)
                        .setContentTitle("LibreTube")
                        .setContentText("Downloading")
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setProgress(progressMax, 0, true)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)

                FFmpegKit.executeAsync("-y -i $videoDir -i $audioDir -c copy ${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${videoId}.mkv",
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
                        val path = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        val folder_main = ".tmp"
                        val f = File(path, folder_main)
                        f.deleteRecursively()
                        stopForeground(true)
                    }, {
                        // CALLED WHEN SESSION PRINTS LOGS
                        Log.e(TAG,it.message.toString())
                    }) {
                    // CALLED WHEN SESSION GENERATES STATISTICS
                    Log.e(TAG+"stat",it.time.toString())
                    val progress = it.time/(10*duration!!)
                    if (progress<1){
                        notification
                            .setProgress(progressMax, progress.toInt(), false)
                        service.notify(1,notification.build())
                    }
                }
                startForeground(1,notification.build())
            }
        }
    }


    override fun onDestroy() {
        unregisterReceiver(onDownloadComplete)
        super.onDestroy()
    }

}
