package com.github.libretube.helpers

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.obj.ImportPlaylist
import com.github.libretube.obj.ImportPlaylistFile
import com.github.libretube.obj.NewPipeSubscription
import com.github.libretube.obj.NewPipeSubscriptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import okio.use

object ImportHelper {
    /**
     * Import subscriptions by a file uri
     */
    fun importSubscriptions(activity: Activity, uri: Uri?) {
        if (uri == null) return
        try {
            val applicationContext = activity.applicationContext
            val channels = getChannelsFromUri(activity, uri)
            CoroutineScope(Dispatchers.IO).launch {
                SubscriptionHelper.importSubscriptions(channels)
            }.invokeOnCompletion {
                applicationContext.toastFromMainThread(R.string.importsuccess)
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG(), e.toString())
            activity.toastFromMainThread(
                activity.getString(R.string.unsupported_file_format) +
                    " (${activity.contentResolver.getType(uri)})"
            )
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
            Toast.makeText(activity, e.localizedMessage, Toast.LENGTH_SHORT).show()
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
                    it.bufferedReader().useLines { lines ->
                        lines.map { line -> line.substringBefore(",") }
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
    @OptIn(ExperimentalSerializationApi::class)
    fun exportSubscriptions(activity: Activity, uri: Uri?) {
        if (uri == null) return
        runBlocking(Dispatchers.IO) {
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

            activity.toastFromMainThread(R.string.exportsuccess)
        }
    }

    /**
     * Import Playlists
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun importPlaylists(activity: Activity, uri: Uri?) {
        if (uri == null) return

        val importPlaylists = mutableListOf<ImportPlaylist>()

        when (val fileType = activity.contentResolver.getType(uri)) {
            "text/csv", "text/comma-separated-values" -> {
                val playlist = ImportPlaylist()
                activity.contentResolver.openInputStream(uri)?.use {
                    val lines = it.bufferedReader().readLines()
                    playlist.name = lines[1].split(",").reversed()[2]
                    val splitIndex = lines.indexOfFirst { line -> line.isBlank() }
                    lines.subList(splitIndex + 2, lines.size).forEach { line ->
                        line.split(",").firstOrNull()?.let { videoId ->
                            if (videoId.isNotBlank()) playlist.videos = playlist.videos + videoId
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
                activity.applicationContext.toastFromMainThread("Unsupported file type $fileType")
                return
            }
        }

        // convert the YouTube URLs to videoIds
        importPlaylists.forEach { playlist ->
            playlist.videos = playlist.videos.map { it.takeLast(11) }
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PlaylistsHelper.importPlaylists(importPlaylists)
                activity.applicationContext.toastFromMainThread(R.string.success)
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                e.localizedMessage?.let {
                    activity.applicationContext.toastFromMainThread(it)
                }
            }
        }
    }

    /**
     * Export Playlists
     */
    fun exportPlaylists(activity: Activity, uri: Uri?) {
        if (uri == null) return

        runBlocking {
            val playlists = PlaylistsHelper.exportPlaylists()
            val playlistFile = ImportPlaylistFile("Piped", 1, playlists)

            activity.contentResolver.openOutputStream(uri)?.use {
                JsonHelper.json.encodeToStream(playlistFile, it)
            }

            activity.toastFromMainThread(R.string.exportsuccess)
        }
    }
}
