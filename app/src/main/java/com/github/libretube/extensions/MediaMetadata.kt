package com.github.libretube.extensions

import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaConstants

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun MediaMetadata.toMediaMetadataCompat(duration: Long, thumbnail: Bitmap?): MediaMetadataCompat {
    val builder = MediaMetadataCompat.Builder()

    title?.let {
        builder.putText(MediaMetadataCompat.METADATA_KEY_TITLE, it)
        builder.putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, it)
    }

    subtitle?.let {
        builder.putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, it)
    }

    description?.let {
        builder.putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, it)
    }

    artist?.let {
        builder.putText(MediaMetadataCompat.METADATA_KEY_ARTIST, it)
    }

    albumTitle?.let {
        builder.putText(MediaMetadataCompat.METADATA_KEY_ALBUM, it)
    }

    albumArtist?.let {
        builder.putText(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, it)
    }

    recordingYear?.toLong()?.let {
        builder.putLong(MediaMetadataCompat.METADATA_KEY_YEAR, it)
    }

    artworkUri?.toString()?.let {
        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, it)
        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, it)
    }

    builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, thumbnail)

    if (duration != C.TIME_UNSET) {
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
    }

    mediaType?.toLong()?.let {
        builder.putLong(MediaConstants.EXTRAS_KEY_MEDIA_TYPE_COMPAT, it)
    }

    return builder.build()
}
