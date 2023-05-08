package com.github.libretube.helpers

import android.app.Activity
import android.net.Uri
import android.util.Log
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.obj.ImportPlaylist
import com.github.libretube.obj.ImportPlaylistFile
import com.github.libretube.obj.NewPipeSubscription
import com.github.libretube.obj.NewPipeSubscriptions
import kotlin.streams.toList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

object ImportHelper {
    /**
     * Import subscriptions by a file uri
     */
    suspend fun importSubscriptions(activity: Activity, uri: Uri) {
        try {
            SubscriptionHelper.importSubscriptions(getChannelsFromUri(activity, uri))
            activity.toastFromMainDispatcher(R.string.importsuccess)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG(), e.toString())
            val type = activity.contentResolver.getType(uri)
            val message = activity.getString(R.string.unsupported_file_format, type)
            activity.toastFromMainDispatcher(message)
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
            e.localizedMessage?.let {
                activity.toastFromMainDispatcher(it)
            }
        }
    }

    /**
     * Get a list of channel IDs from a file [Uri]
     */
    private fun getChannelsFromUri(activity: Activity, uri: Uri): List<String> {
        return when (val fileType = activity.contentResolver.getType(uri)) {
            "application/json", "application/*", "application/octet-stream" -> {
                // NewPipe subscriptions format
                val subscriptions = activity.contentResolver.openInputStream(uri)?.use {
                    JsonHelper.json.decodeFromStream<NewPipeSubscriptions>(it)
                }
                subscriptions?.subscriptions.orEmpty().map {
                    it.url.replace("https://www.youtube.com/channel/", "")
                }
            }
            "text/csv", "text/comma-separated-values" -> {
                // import subscriptions from Google/YouTube Takeout
                activity.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().use { reader ->
                        reader.lines().map { line -> line.substringBefore(",") }
                            .filter { channelId -> channelId.length == 24 }
                            .toList()
                    }
                }.orEmpty()
            }
            else -> throw IllegalArgumentException("Unsupported file type: $fileType")
        }
    }

    /**
     * Write the text to the document
     */
    suspend fun exportSubscriptions(activity: Activity, uri: Uri) {
        val token = PreferenceHelper.getToken()
        val subs = if (token.isNotEmpty()) {
            RetrofitInstance.authApi.subscriptions(token)
        } else {
            val subscriptions = Database.localSubscriptionDao().getAll().map { it.channelId }
            RetrofitInstance.authApi.unauthenticatedSubscriptions(subscriptions)
        }
        val newPipeChannels = subs.map {
            NewPipeSubscription(it.name, 0, "https://www.youtube.com${it.url}")
        }
        val newPipeSubscriptions = NewPipeSubscriptions(subscriptions = newPipeChannels)

        activity.contentResolver.openOutputStream(uri)?.use {
            JsonHelper.json.encodeToStream(newPipeSubscriptions, it)
        }

        activity.toastFromMainDispatcher(R.string.exportsuccess)
    }

    /**
     * Import Playlists
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun importPlaylists(activity: Activity, uri: Uri) {
        val importPlaylists = mutableListOf<ImportPlaylist>()

        when (val fileType = activity.contentResolver.getType(uri)) {
            "text/csv", "text/comma-separated-values" -> {
                val playlist = ImportPlaylist()
                activity.contentResolver.openInputStream(uri)?.use {
                    val lines = it.bufferedReader().use { reader -> reader.lines().toList() }
                    playlist.name = lines[1].split(",").reversed()[2]
                    var splitIndex = lines.indexOfFirst { line -> line.isBlank() }
                    // seek until playlist items table
                    while (lines.getOrNull(splitIndex + 1).orEmpty().isBlank()) {
                        splitIndex++
                    }
                    lines.subList(splitIndex + 2, lines.size).forEach { line ->
                        line.split(",").firstOrNull()?.let { videoId ->
                            if (videoId.isNotBlank()) playlist.videos += videoId.trim()
                        }
                    }
                    importPlaylists.add(playlist)
                }
            }
            "application/json", "application/*", "application/octet-stream" -> {
                val playlistFile = activity.contentResolver.openInputStream(uri)?.use {
                    JsonHelper.json.decodeFromStream<ImportPlaylistFile>(it)
                }
                importPlaylists.addAll(playlistFile?.playlists.orEmpty())
            }
            else -> {
                val message = activity.getString(R.string.unsupported_file_format, fileType)
                activity.toastFromMainDispatcher(message)
                return
            }
        }

        // convert the YouTube URLs to videoIds
        importPlaylists.forEach { playlist ->
            playlist.videos = playlist.videos.map { it.takeLast(11) }
        }
        try {
            PlaylistsHelper.importPlaylists(importPlaylists)
            activity.toastFromMainDispatcher(R.string.success)
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
            e.localizedMessage?.let {
                activity.toastFromMainDispatcher(it)
            }
        }
    }

    /**
     * Export Playlists
     */
    suspend fun exportPlaylists(activity: Activity, uri: Uri) {
        val playlists = PlaylistsHelper.exportPlaylists()
        val playlistFile = ImportPlaylistFile("Piped", 1, playlists)

        activity.contentResolver.openOutputStream(uri)?.use {
            JsonHelper.json.encodeToStream(playlistFile, it)
        }

        activity.toastFromMainDispatcher(R.string.exportsuccess)
    }
}
