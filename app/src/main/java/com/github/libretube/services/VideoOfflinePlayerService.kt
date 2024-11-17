package com.github.libretube.services

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.FileDataSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.enums.FileType
import com.github.libretube.extensions.setMetadata
import com.github.libretube.extensions.toAndroidUri
import com.github.libretube.extensions.updateParameters
import kotlin.io.path.exists

@OptIn(UnstableApi::class)
class VideoOfflinePlayerService: OfflinePlayerService() {
    override val isAudioOnlyPlayer = false

    override fun setMediaItem(downloadWithItems: DownloadWithItems) {
        val downloadFiles = downloadWithItems.downloadItems.filter { it.path.exists() }

        val videoUri = downloadFiles.firstOrNull { it.type == FileType.VIDEO }?.path?.toAndroidUri()
        val audioUri = downloadFiles.firstOrNull { it.type == FileType.AUDIO }?.path?.toAndroidUri()
        val subtitleInfo = downloadFiles.firstOrNull { it.type == FileType.SUBTITLE }

        val subtitle = subtitleInfo?.let {
            SubtitleConfiguration.Builder(it.path.toAndroidUri())
                .setMimeType(MimeTypes.APPLICATION_TTML)
                .setLanguage(it.language ?: "en")
                .build()
        }

        when {
            videoUri != null && audioUri != null -> {
                val videoItem = MediaItem.Builder()
                    .setUri(videoUri)
                    .setMetadata(downloadWithItems)
                    .setSubtitleConfigurations(listOfNotNull(subtitle))
                    .build()

                val videoSource = ProgressiveMediaSource.Factory(FileDataSource.Factory())
                    .createMediaSource(videoItem)

                val audioSource = ProgressiveMediaSource.Factory(FileDataSource.Factory())
                    .createMediaSource(MediaItem.fromUri(audioUri))

                var mediaSource = MergingMediaSource(audioSource, videoSource)
                if (subtitle != null) {
                    val subtitleSource = SingleSampleMediaSource.Factory(FileDataSource.Factory())
                        .createMediaSource(subtitle, C.TIME_UNSET)

                    mediaSource = MergingMediaSource(mediaSource, subtitleSource)
                }

                exoPlayer?.setMediaSource(mediaSource)
            }

            videoUri != null -> exoPlayer?.setMediaItem(
                MediaItem.Builder()
                    .setUri(videoUri)
                    .setMetadata(downloadWithItems)
                    .setSubtitleConfigurations(listOfNotNull(subtitle))
                    .build()
            )

            audioUri != null -> exoPlayer?.setMediaItem(
                MediaItem.Builder()
                    .setUri(audioUri)
                    .setMetadata(downloadWithItems)
                    .setSubtitleConfigurations(listOfNotNull(subtitle))
                    .build()
            )
        }

        trackSelector?.updateParameters {
            setPreferredTextRoleFlags(C.ROLE_FLAG_CAPTION)
            setPreferredTextLanguage(subtitle?.language)
        }
    }
}