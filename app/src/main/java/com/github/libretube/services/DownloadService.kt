package com.github.libretube.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.github.libretube.R
import com.github.libretube.api.CronetHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.DOWNLOAD_CHANNEL_ID
import com.github.libretube.constants.DOWNLOAD_PROGRESS_NOTIFICATION_ID
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.db.obj.Download
import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.enums.FileType
import com.github.libretube.extensions.awaitQuery
import com.github.libretube.extensions.formatAsFileSize
import com.github.libretube.extensions.getContentLength
import com.github.libretube.extensions.query
import com.github.libretube.extensions.toDownloadItems
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.obj.DownloadStatus
import com.github.libretube.receivers.NotificationReceiver
import com.github.libretube.receivers.NotificationReceiver.Companion.ACTION_DOWNLOAD_PAUSE
import com.github.libretube.receivers.NotificationReceiver.Companion.ACTION_DOWNLOAD_RESUME
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.util.DownloadHelper
import com.github.libretube.util.DownloadHelper.getNotificationId
import com.github.libretube.util.ImageHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors

/**
 * Download service with custom implementation of downloading using [HttpURLConnection].
 */
class DownloadService : Service() {

    private val binder = LocalBinder()
    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val jobMain = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + jobMain)

    private lateinit var notificationManager: NotificationManager
    private lateinit var summaryNotificationBuilder: NotificationCompat.Builder

    private val jobs = mutableMapOf<Int, Job>()
    private val downloadQueue = mutableMapOf<Int, Boolean>()
    private val _downloadFlow = MutableSharedFlow<Pair<Int, DownloadStatus>>()
    val downloadFlow: SharedFlow<Pair<Int, DownloadStatus>> = _downloadFlow

    override fun onCreate() {
        super.onCreate()
        IS_DOWNLOAD_RUNNING = true
        notifyForeground()
        sendBroadcast(Intent(ACTION_SERVICE_STARTED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD_RESUME -> resume(intent.getIntExtra("id", -1))
            ACTION_DOWNLOAD_PAUSE -> pause(intent.getIntExtra("id", -1))
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

                awaitQuery {
                    Database.downloadDao().insertDownload(
                        Download(
                            videoId = videoId,
                            title = streams.title ?: "",
                            thumbnailPath = File(
                                DownloadHelper.getDownloadDir(this@DownloadService, DownloadHelper.THUMBNAIL_DIR),
                                fileName
                            ).absolutePath,
                            description = streams.description ?: "",
                            uploadDate = streams.uploadDate,
                            uploader = streams.uploader ?: ""
                        )
                    )
                }
                streams.thumbnailUrl?.let { url ->
                    ImageHelper.downloadImage(
                        this@DownloadService,
                        url,
                        File(
                            DownloadHelper.getDownloadDir(
                                this@DownloadService,
                                DownloadHelper.THUMBNAIL_DIR
                            ),
                            fileName
                        ).absolutePath
                    )
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

    /**
     * Initiate download [Job] using [DownloadItem] by creating file according to [FileType]
     * for the requested file.
     */
    private fun start(item: DownloadItem) {
        val file: File = when (item.type) {
            FileType.AUDIO -> {
                val audioDownloadDir = DownloadHelper.getDownloadDir(
                    this,
                    DownloadHelper.AUDIO_DIR
                )
                File(audioDownloadDir, item.fileName)
            }
            FileType.VIDEO -> {
                val videoDownloadDir = DownloadHelper.getDownloadDir(
                    this,
                    DownloadHelper.VIDEO_DIR
                )
                File(videoDownloadDir, item.fileName)
            }
            FileType.SUBTITLE -> {
                val subtitleDownloadDir = DownloadHelper.getDownloadDir(
                    this,
                    DownloadHelper.SUBTITLE_DIR
                )
                File(subtitleDownloadDir, item.fileName)
            }
        }
        file.createNewFile()
        item.path = file.absolutePath

        item.id = awaitQuery {
            Database.downloadDao().insertDownloadItem(item)
        }.toInt()

        jobs[item.id] = scope.launch {
            downloadFile(item)
        }
    }

    /**
     * Download file and emit [DownloadStatus] to the collectors of [downloadFlow]
     * and notification.
     */
    private suspend fun downloadFile(item: DownloadItem) {
        downloadQueue[item.id] = true
        val notificationBuilder = getNotificationBuilder(item)
        setResumeNotification(notificationBuilder, item)
        val file = File(item.path)
        var totalRead = file.length()
        val url = URL(item.url ?: return)

        url.getContentLength().let { size ->
            if (size > 0 && size != item.downloadSize) {
                item.downloadSize = size
                query {
                    Database.downloadDao().updateDownloadItem(item)
                }
            }
        }

        try {
            // Set start range where last downloading was held.
            val con = CronetHelper.getCronetEngine().openConnection(url) as HttpURLConnection
            con.requestMethod = "GET"
            con.setRequestProperty("Range", "bytes=$totalRead-")
            con.connectTimeout = DownloadHelper.DEFAULT_TIMEOUT
            con.readTimeout = DownloadHelper.DEFAULT_TIMEOUT

            withContext(Dispatchers.IO) {
                // Retry connecting to server for n times.
                for (i in 1..DownloadHelper.DEFAULT_RETRY) {
                    try {
                        con.connect()
                        break
                    } catch (_: SocketTimeoutException) {
                        val message = getString(R.string.downloadfailed) + " " + i
                        _downloadFlow.emit(item.id to DownloadStatus.Error(message))
                        toastFromMainThread(message)
                    }
                }
            }

            // If link is expired try to regenerate using available info.
            if (con.responseCode == 403) {
                regenerateLink(item)
                con.disconnect()
                downloadFile(item)
                return
            } else if (con.responseCode !in 200..299) {
                val message = getString(R.string.downloadfailed) + ": " + con.responseMessage
                _downloadFlow.emit(item.id to DownloadStatus.Error(message))
                toastFromMainThread(message)
                con.disconnect()
                pause(item.id)
                return
            }

            val sink: BufferedSink = file.sink(true).buffer()
            val sourceByte = con.inputStream.source()

            var lastTime = System.currentTimeMillis() / 1000
            var lastRead: Long = 0

            try {
                // Check if downloading is still active and read next bytes.
                while (downloadQueue[item.id] == true &&
                    sourceByte
                        .read(sink.buffer, DownloadHelper.DOWNLOAD_CHUNK_SIZE)
                        .also { lastRead = it } != -1L
                ) {
                    sink.emit()
                    totalRead += lastRead
                    _downloadFlow.emit(
                        item.id to DownloadStatus.Progress(
                            lastRead,
                            totalRead,
                            item.downloadSize
                        )
                    )
                    if (item.downloadSize != -1L &&
                        System.currentTimeMillis() / 1000 > lastTime
                    ) {
                        notificationBuilder
                            .setContentText("${totalRead.formatAsFileSize()} / ${item.downloadSize.formatAsFileSize()}")
                            .setProgress(item.downloadSize.toInt(), totalRead.toInt(), false)
                        notificationManager.notify(item.getNotificationId(), notificationBuilder.build())
                        lastTime = System.currentTimeMillis() / 1000
                    }
                }
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                toastFromMainThread("${getString(R.string.download)}: ${e.message}")
                _downloadFlow.emit(item.id to DownloadStatus.Error(e.message.toString(), e))
            }

            withContext(Dispatchers.IO) {
                sink.flush()
                sink.close()
                sourceByte.close()
                con.disconnect()
            }
        } catch (_: Exception) { }

        val completed = when {
            totalRead < item.downloadSize -> {
                _downloadFlow.emit(item.id to DownloadStatus.Paused)
                false
            }
            else -> {
                _downloadFlow.emit(item.id to DownloadStatus.Completed)
                true
            }
        }
        setPauseNotification(notificationBuilder, item, completed)
        pause(item.id)
    }

    /**
     * Resume download which may have been paused.
     */
    fun resume(id: Int) {
        // If file is already downloading then avoid new download job.
        if (downloadQueue[id] == true) return

        val downloadItem = awaitQuery {
            Database.downloadDao().findDownloadItemById(id)
        }
        scope.launch {
            downloadFile(downloadItem)
        }
    }

    /**
     * Pause downloading job for given [id]. If no downloads are active, stop the service.
     */
    fun pause(id: Int) {
        downloadQueue[id] = false

        // Stop the service if no downloads are active.
        if (downloadQueue.none { it.value }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            }
            sendBroadcast(Intent(ACTION_SERVICE_STOPPED))
            stopSelf()
        }
    }

    /**
     * Regenerate stream url using available info format and quality.
     */
    private suspend fun regenerateLink(item: DownloadItem) {
        val streams = RetrofitInstance.api.getStreams(item.videoId)
        val stream = when (item.type) {
            FileType.AUDIO -> streams.audioStreams
            FileType.VIDEO -> streams.videoStreams
            else -> null
        }
        stream?.find { it.format == item.format && it.quality == item.quality }?.let {
            item.url = it.url
        }
        query {
            Database.downloadDao().updateDownloadItem(item)
        }
    }

    /**
     * Check whether the file downloading or not.
     */
    fun isDownloading(id: Int): Boolean {
        return downloadQueue[id] ?: false
    }

    private fun notifyForeground() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        summaryNotificationBuilder = NotificationCompat
            .Builder(this, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.downloading))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setGroup(DOWNLOAD_NOTIFICATION_GROUP)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setGroupSummary(true)

        startForeground(DOWNLOAD_PROGRESS_NOTIFICATION_ID, summaryNotificationBuilder.build())
    }

    private fun getNotificationBuilder(item: DownloadItem): NotificationCompat.Builder {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        } else {
            PendingIntent.FLAG_CANCEL_CURRENT
        }

        val activityIntent =
            PendingIntent.getActivity(
                this@DownloadService,
                0,
                Intent(this@DownloadService, MainActivity::class.java).apply {
                    putExtra("fragmentToOpen", "downloads")
                },
                flags
            )

        return NotificationCompat
            .Builder(this, DOWNLOAD_CHANNEL_ID)
            .setContentTitle("[${item.type}] ${item.fileName}")
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setContentIntent(activityIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setGroup(DOWNLOAD_NOTIFICATION_GROUP)
    }

    private fun setResumeNotification(notificationBuilder: NotificationCompat.Builder, item: DownloadItem) {
        notificationBuilder
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .clearActions()
            .addAction(getPauseAction(item.id))

        notificationManager.notify(item.getNotificationId(), notificationBuilder.build())
    }

    private fun setPauseNotification(
        notificationBuilder: NotificationCompat.Builder,
        item: DownloadItem,
        isCompleted: Boolean = false
    ) {
        notificationBuilder
            .setProgress(0, 0, false)
            .setOngoing(false)
            .clearActions()

        if (isCompleted) {
            notificationBuilder
                .setSmallIcon(R.drawable.ic_done)
                .setContentText(getString(R.string.download_completed))
        } else {
            notificationBuilder
                .setSmallIcon(R.drawable.ic_pause)
                .setContentText(getString(R.string.download_paused))
                .addAction(getResumeAction(item.id))
        }
        notificationManager.notify(item.getNotificationId(), notificationBuilder.build())
    }

    private fun getResumeAction(id: Int): NotificationCompat.Action {
        val intent = Intent(this, NotificationReceiver::class.java)

        intent.action = ACTION_DOWNLOAD_RESUME
        intent.putExtra("id", id)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return NotificationCompat.Action.Builder(
            R.drawable.ic_play,
            getString(R.string.resume),
            PendingIntent.getBroadcast(this, id, intent, flags)
        ).build()
    }

    private fun getPauseAction(id: Int): NotificationCompat.Action {
        val intent = Intent(this, NotificationReceiver::class.java)

        intent.action = ACTION_DOWNLOAD_PAUSE
        intent.putExtra("id", id)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return NotificationCompat.Action.Builder(
            R.drawable.ic_pause,
            getString(R.string.pause),
            PendingIntent.getBroadcast(this, id, intent, flags)
        ).build()
    }

    override fun onDestroy() {
        downloadQueue.clear()
        IS_DOWNLOAD_RUNNING = false
        sendBroadcast(Intent(ACTION_SERVICE_STOPPED))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        val ids = intent?.getIntArrayExtra("ids")
        ids?.forEach { id -> resume(id) }
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    companion object {
        private const val DOWNLOAD_NOTIFICATION_GROUP = "download_notification_group"
        const val ACTION_SERVICE_STARTED =
            "com.github.libretube.services.DownloadService.ACTION_SERVICE_STARTED"
        const val ACTION_SERVICE_STOPPED =
            "com.github.libretube.services.DownloadService.ACTION_SERVICE_STOPPED"
        var IS_DOWNLOAD_RUNNING = false
    }
}
