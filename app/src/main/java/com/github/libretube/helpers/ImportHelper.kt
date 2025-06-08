package com.github.libretube.helpers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.enums.ImportFormat
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.obj.FreeTubeImportPlaylist
import com.github.libretube.obj.FreeTubeVideo
import com.github.libretube.obj.FreetubeSubscription
import com.github.libretube.obj.FreetubeSubscriptions
import com.github.libretube.obj.NewPipeSubscription
import com.github.libretube.obj.NewPipeSubscriptions
import com.github.libretube.obj.PipedImportPlaylist
import com.github.libretube.obj.PipedPlaylistFile
import com.github.libretube.obj.YouTubeWatchHistoryFileItem
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import com.github.libretube.util.TextUtils
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.util.stream.Collectors

object ImportHelper {
    private const val IMPORT_THUMBNAIL_QUALITY = "mqdefault"
    private const val VIDEO_ID_LENGTH = 11
    private const val YOUTUBE_IMG_URL = "https://img.youtube.com"

    // format: playlistName-videos.csv, where "videos" could also be i18ned to a different language
    private val csvPlaylistNameRegex = Regex("""(.*)-(\w+)\.csv""")

    /**
     * Import subscriptions by a file uri
     */
    suspend fun importSubscriptions(context: Context, uri: Uri, importFormat: ImportFormat) {
        try {
            SubscriptionHelper.importSubscriptions(getChannelsFromUri(context, uri, importFormat))
            context.toastFromMainDispatcher(R.string.importsuccess)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG(), e.toString())
            val type = context.contentResolver.getType(uri)
            val message = context.getString(R.string.unsupported_file_format, type)
            context.toastFromMainDispatcher(message)
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
            e.localizedMessage?.let {
                context.toastFromMainDispatcher(it)
            }
        }
    }

    /**
     * Get a list of channel IDs from a file [Uri]
     */
    @OptIn(ExperimentalSerializationApi::class)
    private fun getChannelsFromUri(
        context: Context,
        uri: Uri,
        importFormat: ImportFormat
    ): List<String> {
        return when (importFormat) {
            ImportFormat.NEWPIPE -> {
                val subscriptions = context.contentResolver.openInputStream(uri)?.use {
                    JsonHelper.json.decodeFromStream<NewPipeSubscriptions>(it)
                }
                subscriptions?.subscriptions.orEmpty().map {
                    it.url.replace("$YOUTUBE_FRONTEND_URL/channel/", "")
                }
            }

            ImportFormat.FREETUBE -> {
                val subscriptions = context.contentResolver.openInputStream(uri)?.use {
                    JsonHelper.json.decodeFromStream<FreetubeSubscriptions>(it)
                }
                subscriptions?.subscriptions.orEmpty().map {
                    it.url.replace("$YOUTUBE_FRONTEND_URL/channel/", "")
                }
            }

            ImportFormat.YOUTUBECSV -> {
                // import subscriptions from Google/YouTube Takeout
                context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().use { reader ->
                        reader.lines().map { line -> line.substringBefore(",") }
                            .filter { channelId -> channelId.length == 24 }
                            .collect(Collectors.toList())
                    }
                }.orEmpty()
            }

            else -> throw IllegalArgumentException()
        }
    }

    /**
     * Write the text to the document
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun exportSubscriptions(context: Context, uri: Uri, importFormat: ImportFormat) {
        val subs = SubscriptionHelper.getSubscriptions()

        when (importFormat) {
            ImportFormat.NEWPIPE -> {
                val newPipeChannels = subs.map {
                    NewPipeSubscription(it.name, 0, "$YOUTUBE_FRONTEND_URL/channel/${it.url}")
                }
                val newPipeSubscriptions = NewPipeSubscriptions(subscriptions = newPipeChannels)
                context.contentResolver.openOutputStream(uri)?.use {
                    JsonHelper.json.encodeToStream(newPipeSubscriptions, it)
                }
            }

            ImportFormat.FREETUBE -> {
                val freeTubeChannels = subs.map {
                    FreetubeSubscription(
                        it.name,
                        "",
                        "$YOUTUBE_FRONTEND_URL/channel/${it.url}"
                    )
                }
                val freeTubeSubscriptions = FreetubeSubscriptions(subscriptions = freeTubeChannels)
                context.contentResolver.openOutputStream(uri)?.use {
                    JsonHelper.json.encodeToStream(freeTubeSubscriptions, it)
                }
            }

            else -> throw IllegalArgumentException()
        }

        context.toastFromMainDispatcher(R.string.exportsuccess)
    }

    /**
     * Import Playlists
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun importPlaylists(context: Context, uri: Uri, importFormat: ImportFormat) {
        val importPlaylists = mutableListOf<PipedImportPlaylist>()

        when (importFormat) {
            ImportFormat.PIPED -> {
                val playlistFile = context.contentResolver.openInputStream(uri)?.use {
                    JsonHelper.json.decodeFromStream<PipedPlaylistFile>(it)
                }
                importPlaylists.addAll(playlistFile?.playlists.orEmpty())

                // convert the YouTube URLs to videoIds
                importPlaylists.forEach { playlist ->
                    playlist.videos = playlist.videos.map { it.takeLast(VIDEO_ID_LENGTH) }
                }
            }

            ImportFormat.FREETUBE -> {
                val playlistFile =
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val text = inputStream.bufferedReader().readText()
                        runCatching {
                            text.lines().map { line ->
                                JsonHelper.json.decodeFromString<FreeTubeImportPlaylist>(line)
                            }
                        }.getOrNull() ?: runCatching {
                            listOf(JsonHelper.json.decodeFromString<FreeTubeImportPlaylist>(text))
                        }.getOrNull()
                    }

                val playlists = playlistFile.orEmpty().map { playlist ->
                    // convert FreeTube videos to list of string
                    // convert FreeTube playlists to piped playlists
                    PipedImportPlaylist(
                        playlist.name,
                        null,
                        null,
                        playlist.videos.map { it.videoId }
                    )
                }
                importPlaylists.addAll(playlists)
            }

            ImportFormat.YOUTUBECSV -> {
                val playlist = PipedImportPlaylist()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val lines = inputStream.bufferedReader().readLines()
                    // invalid playlist file, hence returning
                    if (lines.size < 2) return

                    val playlistName = lines[1].split(",").reversed().getOrNull(2)
                    // the playlist name can be undefined in some cases, e.g. watch later lists
                    playlist.name = playlistName ?: extractYTPlaylistName(context, uri)
                            ?: TextUtils.getFileSafeTimeStampNow()

                    // start directly at the beginning if header playlist info such as name is missing
                    val startIndex = if (playlistName == null) {
                        1
                    } else {
                        // seek to the first blank line
                        var splitIndex = lines.indexOfFirst { line -> line.isBlank() }
                        while (lines.getOrElse(splitIndex) { return }.isBlank()) splitIndex++
                        // skip the line containing the names of the columns
                        splitIndex + 2
                    }
                    for (line in lines.subList(startIndex, lines.size)) {
                        if (line.isBlank()) continue

                        val videoId = line.split(",")
                            .firstOrNull()
                            ?.takeIf { it.isNotBlank() }

                        if (videoId != null) {
                            playlist.videos += videoId.trim().takeLast(VIDEO_ID_LENGTH)
                        }
                    }
                    importPlaylists.add(playlist)
                }
            }

            ImportFormat.URLSORIDS -> {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val playlist = PipedImportPlaylist(name = TextUtils.getFileSafeTimeStampNow())

                    playlist.videos = inputStream.bufferedReader().readLines()
                        .flatMap { it.split(",") }
                        .mapNotNull { videoUrlOrId ->
                            if (videoUrlOrId.length == VIDEO_ID_LENGTH) {
                                videoUrlOrId
                            } else {
                                TextUtils.getVideoIdFromUri(videoUrlOrId.toUri())
                            }
                        }

                    if (playlist.videos.isNotEmpty()) {
                        importPlaylists.add(playlist)
                    }
                }
            }

            else -> throw IllegalArgumentException()
        }

        if (importPlaylists.isEmpty()) {
            context.toastFromMainDispatcher(R.string.emptyList)
            return
        }

        try {
            PlaylistsHelper.importPlaylists(importPlaylists)
            context.toastFromMainDispatcher(R.string.success)
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
            e.localizedMessage?.let {
                context.toastFromMainDispatcher(it)
            }
        }
    }

    /**
     * Export Playlists
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun exportPlaylists(
        context: Context,
        uri: Uri,
        importFormat: ImportFormat,
        selectedPlaylistIds: List<String>? = null
    ) {
        val playlists = PlaylistsHelper.getAllPlaylistsWithVideos(selectedPlaylistIds)

        when (importFormat) {
            ImportFormat.PIPED -> {
                val playlistFile = PipedPlaylistFile(playlists = playlists.map {
                    val videos = it.relatedStreams.map { item ->
                        "$YOUTUBE_FRONTEND_URL/watch?v=${item.url!!.toID()}"
                    }
                    PipedImportPlaylist(it.name, "playlist", "private", videos)
                })

                context.contentResolver.openOutputStream(uri)?.use {
                    JsonHelper.json.encodeToStream(playlistFile, it)
                }
                context.toastFromMainDispatcher(R.string.exportsuccess)
            }

            ImportFormat.FREETUBE -> {
                val freeTubeExportDb = playlists.map { playlist ->
                    val videos = playlist.relatedStreams.map { videoInfo ->
                        FreeTubeVideo(
                            videoId = videoInfo.url.orEmpty().toID(),
                            title = videoInfo.title.orEmpty(),
                            author = videoInfo.uploaderName.orEmpty(),
                            authorId = videoInfo.uploaderUrl.orEmpty().toID(),
                            lengthSeconds = videoInfo.duration ?: 0L
                        )
                    }
                    FreeTubeImportPlaylist(playlist.name.orEmpty(), videos)
                }.joinToString("\n") { playlist ->
                    JsonHelper.json.encodeToString(playlist)
                }

                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(freeTubeExportDb.toByteArray())
                }
                context.toastFromMainDispatcher(R.string.exportsuccess)
            }

            ImportFormat.URLSORIDS -> {
                val urlListExport = playlists
                    .flatMap { it.relatedStreams }
                    .joinToString("\n") { YOUTUBE_FRONTEND_URL + "/watch?v=" + it.url!!.toID() }

                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(urlListExport.toByteArray())
                }
                context.toastFromMainDispatcher(R.string.exportsuccess)
            }

            else -> Unit
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun importWatchHistory(context: Context, uri: Uri, importFormat: ImportFormat) {
        val videos = when (importFormat) {
            ImportFormat.YOUTUBEJSON -> {
                context.contentResolver.openInputStream(uri)?.use {
                    JsonHelper.json.decodeFromStream<List<YouTubeWatchHistoryFileItem>>(it)
                }
                    .orEmpty()
                    .filter { it.activityControls.isNotEmpty() && it.subtitles.isNotEmpty() && it.titleUrl.isNotEmpty() }
                    .reversed()
                    .map {
                        val videoId = it.titleUrl.takeLast(VIDEO_ID_LENGTH)

                        WatchHistoryItem(
                            videoId = videoId,
                            title = it.title.replaceFirst("Watched ", ""),
                            uploader = it.subtitles.firstOrNull()?.name,
                            uploaderUrl = it.subtitles.firstOrNull()?.url?.let { url ->
                                url.substring(url.length - 24)
                            },
                            thumbnailUrl = "${YOUTUBE_IMG_URL}/vi/${videoId}/${IMPORT_THUMBNAIL_QUALITY}.jpg"
                        )
                    }
            }

            else -> emptyList()
        }

        for (video in videos) {
            DatabaseHelper.addToWatchHistory(video)
        }

        if (videos.isEmpty()) {
            context.toastFromMainDispatcher(R.string.emptyList)
        } else {
            context.toastFromMainDispatcher(R.string.success)
        }
    }

    private fun extractYTPlaylistName(context: Context, uri: Uri): String? {
        val fileName = DocumentFile.fromSingleUri(context, uri)?.name

        return csvPlaylistNameRegex.find(fileName.orEmpty())?.groupValues?.getOrNull(1)
            ?: fileName?.removeSuffix(".csv")
    }
}
