package com.github.libretube.extensions

import com.github.libretube.api.obj.Playlist
import com.github.libretube.db.obj.PlaylistBookmark

fun Playlist.toPlaylistBookmark(playlistId: String): PlaylistBookmark {
    return PlaylistBookmark(
        playlistId = playlistId,
        playlistName = name,
        thumbnailUrl = thumbnailUrl,
        uploader = uploader,
        uploaderAvatar = uploaderAvatar,
        uploaderUrl = uploaderUrl
    )
}
