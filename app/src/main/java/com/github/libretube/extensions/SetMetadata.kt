package com.github.libretube.extensions

import android.support.v4.media.MediaMetadataCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.github.libretube.api.obj.Streams
import com.github.libretube.db.obj.Download

fun MediaItem.Builder.setMetadata(streams: Streams) = apply {
    val extras = bundleOf(
        MediaMetadataCompat.METADATA_KEY_TITLE to streams.title,
        MediaMetadataCompat.METADATA_KEY_ARTIST to streams.uploader
    )
    setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(streams.title)
            .setArtist(streams.uploader)
            .setArtworkUri(streams.thumbnailUrl.toUri())
            .setExtras(extras)
            .build()
    )
}

fun MediaItem.Builder.setMetadata(download: Download) = apply {
    val extras = bundleOf(
        MediaMetadataCompat.METADATA_KEY_TITLE to download.title,
        MediaMetadataCompat.METADATA_KEY_ARTIST to download.uploader
    )
    setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(download.title)
            .setArtist(download.uploader)
            .setArtworkUri(download.thumbnailPath?.toAndroidUri())
            .setExtras(extras)
            .build()
    )
}
