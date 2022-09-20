package com.github.libretube.services

import android.app.DownloadManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import com.github.libretube.R
import java.io.File

class UpdateService : Service() {
    private lateinit var downloadUrl: String
    private var downloadId: Long = -1
    private lateinit var file: File
    private lateinit var downloadManager: DownloadManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        downloadUrl = intent?.getStringExtra("downloadUrl")!!

        downloadApk(downloadUrl)

        return super.onStartCommand(intent, flags, startId)
    }

    private fun downloadApk(downloadUrl: String) {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        // val dir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        file = File(dir, "release.apk")

        val request: DownloadManager.Request =
            DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle(getString(R.string.downloading_apk))
                .setDescription("")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        downloadManager =
            applicationContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager

        downloadId = downloadManager.enqueue(request)

        // listener for the download to end
        registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                // install the apk after download finished
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(
                        Uri.fromFile(file),
                        downloadManager.getMimeTypeForDownloadedFile(downloadId)
                    )
                }

                try {
                    startActivity(installIntent)
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        R.string.downloadsucceeded,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(onDownloadComplete)
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}
