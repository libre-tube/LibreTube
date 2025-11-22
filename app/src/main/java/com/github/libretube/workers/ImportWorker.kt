package com.github.libretube.workers

import android.content.Context
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.enums.ImportFormat
import com.github.libretube.enums.ImportType
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.ImportHelper.IMPORT_THUMBNAIL_QUALITY
import com.github.libretube.helpers.ImportHelper.VIDEO_ID_LENGTH
import com.github.libretube.helpers.ImportHelper.YOUTUBE_IMG_URL
import com.github.libretube.obj.YouTubeWatchHistoryFileItem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.collections.orEmpty
import kotlin.collections.reversed

class ImportWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val importType = inputData.getString("IMPORT_TYPE") ?: ImportType.IMPORT_WATCH_HISTORY.toString()
        val importEnum: ImportType = enumValueOf<ImportType>(importType)
        return Result.success()
    }


    private fun ImportWatchHistory(): Unit{
        val fileUrisString = inputData.getString("files")
        val importFormatString = inputData.getString("import_format") ?: ImportFormat.YOUTUBEJSON.toString()
        val importFormat = enumValueOf<ImportFormat>(importFormatString)
        fileUrisString?.let {a->
            val fileUrisDeserialized = JsonHelper.json.decodeFromString<List<Uri>>(a)
            val videos = when (importFormat) {
                ImportFormat.YOUTUBEJSON -> {
                    fileUrisDeserialized.flatMap { uri->
                        applicationContext.contentResolver.openInputStream(uri)?.use {
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
                }

                else -> emptyList()
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun importWatchHistory(context: Context, uris: List<Uri>, importFormat: ImportFormat) {


        for (video in videos) {
            DatabaseHelper.addToWatchHistory(video)
        }

        if (videos.isEmpty()) {
            context.toastFromMainDispatcher(R.string.emptyList)
        } else {
            context.toastFromMainDispatcher(R.string.success)
        }
    }
}