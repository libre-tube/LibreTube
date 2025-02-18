package com.github.libretube.api

import android.content.Context
import com.github.libretube.R
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.api.obj.Message
import com.github.libretube.api.obj.MetaInfo
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.PreviewFrames
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Streams
import com.github.libretube.api.obj.Subtitle
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.toKotlinInstant
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
val uploaderAvatarCache = ConcurrentHashMap<String, String>()

fun VideoStream.toPipedStream() = PipedStream(
    url = content,
    codec = codec,
    format = format?.toString(),
    height = height,
    width = width,
    quality = getResolution(),
    mimeType = format?.mimeType,
    bitrate = bitrate,
    initStart = initStart,
    initEnd = initEnd,
    indexStart = indexStart,
    indexEnd = indexEnd,
    fps = fps,
    contentLength = itagItem?.contentLength ?: 0L
)
fun StreamInfoItem.toStreamItem(
    uploaderAvatarUrl: String? = null
) = StreamItem(
    type = StreamItem.TYPE_STREAM,
    url = url.toID(),
    title = name,
    uploaded = uploadDate?.offsetDateTime()?.toEpochSecond()?.times(1000) ?: -1,
    uploadedDate = textualUploadDate ?: uploadDate?.offsetDateTime()?.toLocalDateTime()?.toLocalDate()
        ?.toString(),
    uploaderName = uploaderName,
    uploaderUrl = uploaderUrl.toID(),
    uploaderAvatar = uploaderAvatarUrl ?: uploaderAvatars.maxByOrNull { it.height }?.url?.also {
        uploaderUrl.toID().let { id -> uploaderAvatarCache[id] = it }
    } ?: uploaderAvatarCache[uploaderUrl.toID()],
    thumbnail = thumbnails.maxByOrNull { it.height }?.url,
    duration = duration,
    views = viewCount,
    uploaderVerified = isUploaderVerified,
    shortDescription = shortDescription,
    isShort = isShortFormContent
)

object StreamsExtractor {
    private val extractorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  suspend inline fun extractStreams(videoId: String): Streams = withContext(Dispatchers.IO) {
        if (!PlayerHelper.disablePipedProxy || !PlayerHelper.localStreamExtraction) {
            return@withContext RetrofitInstance.api.getStreams(videoId)
        }

        val resp = StreamInfo.getInfo("$YOUTUBE_FRONTEND_URL/watch?v=$videoId")
        val deferredOperations = coroutineScope {
            val dislikes = async {
                if (PlayerHelper.localRYD) runCatching {
                    RetrofitInstance.externalApi.getVotes(videoId).dislikes
                }.getOrElse { -1 } else -1
            }

            val relatedStreams =async {
                resp.relatedItems
                    .filterIsInstance<StreamInfoItem>()
                    .map { item -> async { item.toStreamItem() } }
                    .awaitAll()
            }




            val audioStreams = async {
                resp.audioStreams.map { stream ->
                    PipedStream(
                        url = stream.content,
                        format = stream.format?.toString(),
                        quality = "${stream.averageBitrate} bits",
                        bitrate = stream.bitrate,
                        mimeType = stream.format?.mimeType,
                        initStart = stream.initStart,
                        initEnd = stream.initEnd,
                        indexStart = stream.indexStart,
                        indexEnd = stream.indexEnd,
                        contentLength = stream.itagItem?.contentLength ?: 0L,
                        codec = stream.codec,
                        audioTrackId = stream.audioTrackId,
                        audioTrackName = stream.audioTrackName,
                        audioTrackLocale = stream.audioLocale?.toLanguageTag(),
                        audioTrackType = stream.audioTrackType?.name,
                        videoOnly = false
                    )
                }
            }

            val videoStreams = async {
                resp.videoOnlyStreams.map { it.toPipedStream().copy(videoOnly = true) } +
                        resp.videoStreams.map { it.toPipedStream().copy(videoOnly = false) }
            }

            Triple(
                dislikes.await(),
                relatedStreams.await(),
                Pair(audioStreams.await(), videoStreams.await())
            )
        }

        val (dislikes, relatedStreams, streams) = deferredOperations

        Streams(
            title = resp.name,
            description = resp.description.content,
            uploader = resp.uploaderName,
            uploaderAvatar = resp.uploaderAvatars.maxBy { it.height }.url,
            uploaderUrl = resp.uploaderUrl.toID(),
            uploaderVerified = resp.isUploaderVerified,
            uploaderSubscriberCount = resp.uploaderSubscriberCount,
            category = resp.category,
            views = resp.viewCount,
            likes = resp.likeCount,
            dislikes = dislikes,
            license = resp.licence,
            hls = resp.hlsUrl,
            dash = resp.dashMpdUrl,
            tags = resp.tags,
            metaInfo = resp.metaInfo.map {
                MetaInfo(
                    it.title,
                    it.content.content,
                    it.urls.map { url -> url.toString() },
                    it.urlTexts
                )
            },
            visibility = resp.privacy.name.lowercase(),
            duration = resp.duration,
            uploadTimestamp = resp.uploadDate.offsetDateTime().toInstant().toKotlinInstant(),
            uploaded = resp.uploadDate.offsetDateTime().toEpochSecond() * 1000,
            thumbnailUrl = resp.thumbnails.maxBy { it.height }.url,
            relatedStreams = relatedStreams,
            chapters = resp.streamSegments.map {
                ChapterSegment(
                    title = it.title,
                    image = it.previewUrl.orEmpty(),
                    start = it.startTimeSeconds.toLong()
                )
            },
            audioStreams = streams.first,
            videoStreams = streams.second,
            previewFrames = resp.previewFrames.map {
                PreviewFrames(
                    it.urls,
                    it.frameWidth,
                    it.frameHeight,
                    it.totalCount,
                    it.durationPerFrame.toLong(),
                    it.framesPerPageX,
                    it.framesPerPageY
                )
            },
            subtitles = resp.subtitles.map {
                Subtitle(
                    it.content,
                    it.format?.mimeType,
                    it.displayLanguageName,
                    it.languageTag,
                    it.isAutoGenerated
                )
            }
        )
    }
    fun getExtractorErrorMessageString(context: Context, exception: Exception): String {
        return when (exception) {
            is IOException -> context.getString(R.string.unknown_error)
            is HttpException -> exception.response()?.errorBody()?.string()?.runCatching {
                JsonHelper.json.decodeFromString<Message>(this).message
            }?.getOrNull() ?: context.getString(R.string.server_error)
            else -> exception.localizedMessage.orEmpty()
        }
    }
}