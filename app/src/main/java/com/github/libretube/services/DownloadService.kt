package com.github.libretube.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.DOWNLOAD_CHANNEL_ID
import com.github.libretube.constants.DOWNLOAD_PROGRESS_NOTIFICATION_ID
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.Download
import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.enums.FileType
import com.github.libretube.extensions.awaitQuery
import com.github.libretube.extensions.getContentLength
import com.github.libretube.extensions.query
import com.github.libretube.extensions.toDownloadItems
import com.github.libretube.obj.DownloadStatus
import com.github.libretube.receivers.NotificationReceiver
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.util.DownloadHelper
import com.github.libretube.util.ImageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Download service with custom implementation of downloading using [OkHttpClient].
 */
class DownloadService : Service() {

    private val binder = LocalBinder()
    private val jobMain = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + jobMain)

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var summaryNotificationBuilder: NotificationCompat.Builder

    private val jobs = mutableMapOf<Int, Job>()
    private val _downloadFlow = MutableSharedFlow<Pair<Int, DownloadStatus>>()
    val downloadFlow: SharedFlow<Pair<Int, DownloadStatus>> = _downloadFlow

    override fun onCreate() {
        super.onCreate()
        IS_DOWNLOAD_RUNNING = true
        notifyForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RESUME -> resume(intent.getIntExtra("id", -1))
            ACTION_PAUSE -> pause(intent.getIntExtra("id", -1))
        }

        val videoId = intent?.getStringExtra(IntentData.videoId) ?: return START_NOT_STICKY
        val fileName = intent.getStringExtra(IntentData.fileName) ?: videoId
        val videoFormat = intent.getStringExtra(IntentData.videoFormat)
        val videoQuality = intent.getStringExtra(IntentData.videoQuality)
        val audioFormat = intent.getStringExtra(IntentData.audioFormat)
        val audioQuality = intent.getStringExtra(IntentData.audioQuality)
        val subtitleCode = intent.getStringExtra(IntentData.subtitleCode)

        scope.launch {
            try {
                val streams = RetrofitInstance.api.getStreams(videoId)

                query {
                    DatabaseHolder.Database.downloadDao().insertDownload(
                        Download(
                            videoId = videoId,
                            title = streams.title ?: "",
                            thumbnailPath = File(
                                DownloadHelper.getDownloadDir(this@DownloadService, DownloadHelper.THUMBNAIL_DIR),
                                fileName
                            ).absolutePath,
                            description = streams.description ?: "",
                            uploadDate = streams.uploadDate
                        )
                    )
                }
                streams.thumbnailUrl?.let { url ->
                    ImageHelper.downloadImage(this@DownloadService, url, fileName)
                }

                val downloadItems = streams.toDownloadItems(
                    videoId,
                    fileName,
                    videoFormat,
                    videoQuality,
                    audioFormat,
                    audioQuality,
                    subtitleCode
                )
                downloadItems.forEach { start(it) }
            } catch (e: Exception) {
                return@launch
            }
        }

        return START_NOT_STICKY
    }

    private fun start(item: DownloadItem) {
        val file: File = when (item.type) {
            FileType.AUDIO -> {
                val audioDownloadDir = DownloadHelper.getDownloadDir(this, DownloadHelper.AUDIO_DIR)
                File(audioDownloadDir, item.fileName)
            }
            FileType.VIDEO -> {
                val videoDownloadDir = DownloadHelper.getDownloadDir(this, DownloadHelper.VIDEO_DIR)
                File(videoDownloadDir, item.fileName)
            }
            FileType.SUBTITLE -> {
                val subtitleDownloadDir = DownloadHelper.getDownloadDir(this, DownloadHelper.SUBTITLE_DIR)
                File(subtitleDownloadDir, item.fileName)
            }
        }
        file.createNewFile()
        item.path = file.absolutePath

        item.id = awaitQuery {
            DatabaseHolder.Database.downloadDao().insertDownloadItem(item)
        }.toInt()

        jobs[item.id] = scope.launch {
            downloadFile(item)
        }
    }

    private suspend fun downloadFile(item: DownloadItem) {
        notificationBuilder
            .setContentText(item.fileName)
            .setWhen(System.currentTimeMillis())
            .clearActions()
            .addAction(getPauseAction(item.id))

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(DownloadHelper.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DownloadHelper.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build()

        val file = File(item.path)
        var totalRead = file.length()
        val url = URL(item.url ?: return)

        url.getContentLength().let { size ->
            if (size > 0 && size != item.downloadSize) {
                item.downloadSize = size
                query {
                    DatabaseHolder.Database.downloadDao().updateDownloadItem(item)
                }
            }
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=$totalRead-").build()
        var lastTime = System.currentTimeMillis() / 1000
        var lastRead: Long = 0
        val sink: BufferedSink = file.sink(true).buffer()

        try {
            val response = okHttpClient.newCall(request).execute()
            val sourceBytes = response.body!!.source()

            while (coroutineContext.isActive &&
                sourceBytes
                    .read(sink.buffer, DownloadHelper.DOWNLOAD_CHUNK_SIZE)
                    .also { lastRead = it } != -1L
            ) {
                sink.emit()
                totalRead += lastRead
                _downloadFlow.emit(item.id to DownloadStatus.Progress(totalRead, item.downloadSize))

                if (item.downloadSize != -1L &&
                    System.currentTimeMillis() / 1000 > lastTime) {
                    notificationBuilder.setProgress(item.downloadSize.toInt(), totalRead.toInt(), false)
                    notificationManager.notify(item.id, notificationBuilder.build())
                    lastTime = SystemClock.elapsedRealtime()
                }
            }
            sourceBytes.close()
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT)
                .show()
            _downloadFlow.emit(item.id to DownloadStatus.Error(e.message.toString(), e))
        } finally {
            sink.flush()
            sink.close()
        }

        notificationBuilder
            .setOngoing(false)
            .clearActions()
        if (totalRead == item.downloadSize) {
            _downloadFlow.emit(item.id to DownloadStatus.Completed)
            notificationBuilder
                .setContentTitle("Completed")
        } else {
            _downloadFlow.emit(item.id to DownloadStatus.Paused)
            notificationBuilder
                .setContentTitle("Paused")
                .addAction(getResumeAction(item.id))
        }
        notificationManager.notify(item.id, notificationBuilder.build())
    }

    fun resume(id: Int) {
        jobs[id]?.cancel()
        val downloadItem = awaitQuery {
            DatabaseHolder.Database.downloadDao().findDownloadItemById(id)
        }
        jobs[id] = scope.launch {
            downloadFile(downloadItem)
        }
    }

    fun pause(id: Int) {
        jobs[id]?.cancel()
    }

    fun isDownloading(id: Int): Boolean {
        return jobs[id]?.isActive ?: false
    }

    private fun notifyForeground() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }

        val activityIntent =
            PendingIntent.getActivity(
                this@DownloadService,
                0,
                Intent(this@DownloadService, MainActivity::class.java).apply {
                    putExtra("fragmentToOpen", "downloads")
                },
                flag
            )

        notificationBuilder = NotificationCompat
            .Builder(this, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.downloading))
            .setProgress(0, 100, true)
            .setContentIntent(activityIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setGroup(GROUP_KEY_DOWNLOADS)

        summaryNotificationBuilder = NotificationCompat
            .Builder(this, DOWNLOAD_CHANNEL_ID)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setGroup(GROUP_KEY_DOWNLOADS)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setOnlyAlertOnce(true)
            .setGroupSummary(true)

        startForeground(DOWNLOAD_PROGRESS_NOTIFICATION_ID, summaryNotificationBuilder.build())
    }

    private fun getResumeAction(id: Int): NotificationCompat.Action {
        val intent = Intent(this, NotificationReceiver::class.java)

        intent.action = ACTION_RESUME
        intent.putExtra("id", id)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return NotificationCompat.Action.Builder(
            R.drawable.ic_pause,
            "Resume",
            PendingIntent.getBroadcast(this, id + 1, intent, flags)
        ).build()
    }

    private fun getPauseAction(id: Int): NotificationCompat.Action {
        val intent = Intent(this, NotificationReceiver::class.java)

        intent.action = ACTION_PAUSE
        intent.putExtra("id", id)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return NotificationCompat.Action.Builder(
            R.drawable.ic_pause,
            "Pause",
            PendingIntent.getBroadcast(this, id + 2, intent, flags)
        ).build()
    }

    override fun onDestroy() {
        jobMain.cancel()
        IS_DOWNLOAD_RUNNING = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    companion object {
        private const val GROUP_KEY_DOWNLOADS = "downloads"
        const val ACTION_RESUME = "com.github.libretube.services.DownloadService.ACTION_RESUME"
        const val ACTION_PAUSE = "com.github.libretube.services.DownloadService.ACTION_PAUSE"
        var IS_DOWNLOAD_RUNNING = false
    }
}
