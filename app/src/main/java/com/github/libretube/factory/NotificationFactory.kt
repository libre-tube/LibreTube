package com.github.libretube.factory

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.work.WorkManager
import coil3.ImageLoader
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.withLock
import java.util.UUID

private const val CHANNEL_ID_DEFAULT = "download"
private const val CHANNEL_ID_SILENT = "download_bg"
class NotificationFactory @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    @Assisted private val uuid: UUID,
    @Assisted val isSilent: Boolean) {

    private val builder = NotificationCompat.Builder(context, if (isSilent) CHANNEL_ID_SILENT else CHANNEL_ID_DEFAULT)

    suspend fun create(state: DownloadState?): Notification {
        if (state == null) {
            builder.setContentTitle(context.getString(R.string.manga_downloading_))
            builder.setContentText(context.getString(R.string.preparing_))
        } else {
            builder.setContentTitle(state.manga.title)
            builder.setContentText(context.getString(R.string.manga_downloading_))
        }
        builder.setProgress(1, 0, true)
        builder.setSmallIcon(android.R.drawable.stat_sys_download)
        builder.setContentIntent(queueIntent)
        builder.setStyle(null)
        builder.setLargeIcon(if (state != null) getCover(state.manga)?.toBitmap() else null)
        builder.clearActions()
        builder.setSubText(null)
        builder.setShowWhen(false)
        return builder.build()
    }
}


@AssistedFactory
interface Factory {

    fun create(uuid: UUID, isSilent: Boolean): NotificationFactory
}