package com.github.libretube.player.parser

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import misc.Common.FormatId
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import video_streaming.ClientAbrStateOuterClass.ClientAbrState
import video_streaming.FormatInitializationMetadataOuterClass.FormatInitializationMetadata
import video_streaming.MediaHeaderOuterClass.MediaHeader
import video_streaming.NextRequestPolicyOuterClass.NextRequestPolicy
import video_streaming.PlaybackCookieOuterClass.PlaybackCookie
import video_streaming.SabrContextSendingPolicyOuterClass.SabrContextSendingPolicy
import video_streaming.SabrContextUpdateOuterClass.SabrContextUpdate
import video_streaming.SabrContextUpdateOuterClass.SabrContextUpdate.SabrContextWritePolicy
import video_streaming.SabrErrorOuterClass.SabrError
import video_streaming.SabrRedirectOuterClass.SabrRedirect
import video_streaming.StreamProtectionStatusOuterClass.StreamProtectionStatus
import video_streaming.StreamerContextOuterClass.StreamerContext
import video_streaming.StreamerContextOuterClass.StreamerContext.SabrContext
import video_streaming.UmpPartId.UMPPartId
import video_streaming.VideoPlaybackAbrRequestOuterClass.VideoPlaybackAbrRequest

/**
 * A segment of a media stream.
 *
 * Contains metadata, such as the position in the stream, its own duration, as well the raw
 * media data.
 */
data class Segment(
    /** Header of the media segment containing metadata. */
    val header: MediaHeader,
    /** Sequence number indicating the position of the segment in the media stream. */
    val sequenceNumber: Long,
    /** Raw media data for the segment. */
    val data: MutableList<ByteArray>,
    /** Duration of the segment in milliseconds. */
    val duration: Long,
) {
    /**
     * Length of the media data.
     */
    fun length(): Int = data.sumOf { it.size }

    /**
     * Media data as a single array.
     */
    fun data(): ByteArray {
        val result = ByteArray(length())
        var offset = 0
        for (chunk in data) {
            System.arraycopy(chunk, 0, result, offset, chunk.size)
            offset += chunk.size
        }
        return result
    }
}

/**
 * An initialized format within a video stream.
 */
private data class InitializedFormat(
    /** Identifier of the format. */
    val id: FormatId,
    /** Segments that have been downloaded for this format. */
    val downloadedSegments: MutableMap<Long, Segment> = mutableMapOf(),
    /** Sequence number of the last segment in the format. */
    val endSegmentNumber: Long,
    /** Sequence number for the last segment that has been successfully downloaded. */
    var lastDownloadedSegment: Long = 0,
    /** Duration of the format in milliseconds. */
    val duration: Long,
    /** Duration of the length of downloaded segments in milliseconds. */
    var downloadedDuration: Long = 0,
) {
    /** Returns a list of all downloaded segments for the format. */
    fun data(): List<Segment> {
        val segments = mutableListOf<Segment>()
        for (seq in lastDownloadedSegment..endSegmentNumber) {
            val segment = downloadedSegments.remove(seq)
            if (segment == null) {
                Log.d("InitializedFormat", "data: Missing downloaded segment $seq for ${id.itag}")
                // retry this segment next time
                lastDownloadedSegment = seq
                break
            }
            segments.add(segment)
        }
        return segments
    }

    /**
     * Whether the format has non-retrieved data.
     */
    fun hasFreshData(): Boolean =
        downloadedSegments.containsKey(lastDownloadedSegment)
}

/**
 * A SABR/UMP streaming client.
 *
 * Handles the fetching and processing of streaming media data using the UMP protocol.
 */
class SabrClient(
    /** Unique identifier for the SABR stream resource. */
    private val videoId: String,
    /** The URL pointing to the SABR/UMP stream. */
    private var url: String,
    /** UStreamer configuration data. */
    private val ustreamerConfig: ByteString,
    /** Po (Proof of Origin) Token. */
    private var poToken: ByteString? = null,
) {
    private val TAG = "SabrStream"
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)

    private lateinit var audioFormat: FormatId
    private var videoFormat: FormatId? = null

    /**
     * Initialized formats.
     *
     * A format is initialized when the stream sends a FORMAT_INITIALIZATION_METADATA part,
     * containing the metadata of the format.
     *
     * Each format is identified by its `itag`.
     */
    private val initializedFormats = mutableMapOf<Int, InitializedFormat>()

    /**
     * Partial segments that are being processed.
     *
     * Segments are stored here temporarily until they are fully processed.
     */
    private val partialSegments = mutableMapOf<Int, Segment>()

    /** HTTP Client for requesting UMP data. */
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Content-Type", CONTENT_TYPE)
                .addHeader("Accept-Encoding", ENCODING)
                .addHeader("Accept", ACCEPT)
                .addHeader("User-Agent", USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .build()

    /**
     * PlaybackCookie
     *
     * This cookie needs to be passed to subsequent requests.
     */
    private var playbackCookie: PlaybackCookie? = null

    /**
     * Back off time until the server accepts the next request in milliseconds.
     *
     * When set, the client should wait the specified amount of time before making another
     * request. The server will not send any further data during this period.
     */
    private var backoffTime: Int? = null

    /** SABR contexts for the stream. */
    private val sabrContexts = mutableMapOf<Int, SabrContext>()

    /** Active SABR contexts that should be sent with requests. */
    private val activeSabrContexts = mutableSetOf<Int>()

    /** Position of the player within the stream. */
    private var playerTime: Long = 0

    @OptIn(UnstableApi::class)
    fun selectFormat(itag: Int, lastModified: Long, xtags: String? = null, format: Format) {
        val formatId = FormatId.newBuilder().setItag(itag).setLastModified(lastModified).setXtags(xtags ?: "").build()
        if (MimeTypes.isAudio(format.sampleMimeType)) {
            audioFormat = formatId
        } else if (MimeTypes.isVideo(format.sampleMimeType)) {
            videoFormat = formatId
        }
    }

    fun data(itag: Int): List<Segment> {
        return runBlocking {
            // ensure that the data is only ever accessed by a single thread
            withContext(dispatcher) {
                var format = initializedFormats[itag]
                if (format == null || !format.hasFreshData()) {
                    // refetch new data
                    if (media()) {
                        // finished playback for these formats
                        return@withContext emptyList()
                    }
                }
                format = format ?: initializedFormats[itag]
                return@withContext format?.data() ?: emptyList()
            }
        }
    }


    /**
     * Extracts the raw media data from the stream.
     *
     * The data is returned as a pair of lists: (audio segments, video segments).
     */
    private suspend fun media(): Boolean {
        // Check if we've downloaded all segments
        val audioComplete = initializedFormats[audioFormat.itag]?.let {
            playerTime >= it.duration
        } ?: false

        val videoComplete = videoFormat?.let { format ->
            initializedFormats[format.itag]?.let { playerTime >= it.duration }
        } ?: true

        if (audioComplete && videoComplete) {
            assert(partialSegments.isEmpty()) { "SabrClient has partial segments left" }
            return true
        }

        // update currently held UMP data
        val data = fetchStreamData(audioFormat, videoFormat)

        val parser = UmpParser(data)
        while (true) {
            val part = parser.readPart() ?: break
            Log.v(TAG, "media: Parsing ${part.type}")
            processPart(part)
        }
        assert(parser.data().isEmpty()) { "Parser has left-over data" }

        // update player time to the end of downloaded formats
        val updatedPlayerTime = initializedFormats.values
            .mapNotNull { format ->
                val diff = format.downloadedDuration - playerTime
                if (diff > 0) diff else null
            }
            .minOrNull()

        if (updatedPlayerTime != null) {
            Log.d(TAG, "media: Advancing player time by ${updatedPlayerTime}ms")
            playerTime += updatedPlayerTime
        }
        return false
    }

    /**
     * Fetches streaming data from the URL.
     */
    private suspend fun fetchStreamData(
        audioFormat: FormatId,
        videoFormat: FormatId?,
    ): ByteArray {
        backoffTime?.let { backoff ->
            Log.i(TAG, "fetchStreamData: Waiting for ${backoff}ms before making a request")
            delay(backoff.toLong())
            backoffTime = null
        }

        val clientState = ClientAbrState.newBuilder()
            .setPlayerTimeMs(playerTime)
            .setEnabledTrackTypesBitfield(if (videoFormat == null) 1 else 0)
            .setPlaybackRate(1.0f)
            .setDrcEnabled(true)
            .build()

        val playbackRequest = VideoPlaybackAbrRequest.newBuilder().setClientAbrState(clientState)
            .addAllSelectedFormatIds(
                initializedFormats.values.map { it.id }.toList()
            ).setVideoPlaybackUstreamerConfig(ustreamerConfig)
            .addAllPreferredAudioFormatIds(listOf(audioFormat))
            .addAllPreferredVideoFormatIds(videoFormat?.let { listOf(it) } ?: emptyList())
            .setStreamerContext(
                StreamerContext.newBuilder().setPoToken(poToken)
                    .setClientInfo(
                        StreamerContext.ClientInfo.newBuilder()
                            .setClientName(1)
                            .setClientVersion("2.20250122.04.00")
                            .setOsName("Windows")
                            .setOsVersion("10")
                            .build()
                    )
                    .addAllSabrContexts(activeSabrContexts.mapNotNull { sabrContexts[it] })
                    .addAllUnsentSabrContexts(
                        sabrContexts.keys.filter { it !in activeSabrContexts })
                    .setPlaybackCookie(playbackCookie?.toByteString() ?: ByteString.empty())
                    .build()
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(
                playbackRequest.toByteArray()
                    .toRequestBody(CONTENT_TYPE.toMediaType())
            )
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP request failed: ${response.code}")
        }

        return response.body?.bytes() ?: throw Exception("Empty response body")
    }

    /**
     * Parse a UMP Part, handling its contents as appropriate.
     *
     * @throws Exception if parsing fails or the part is invalid
     */
    private fun processPart(part: Part) {
        when (part.type) {
            UMPPartId.MEDIA_HEADER -> {
                val header = MediaHeader.parseFrom(part.data)
                val videoId = header.videoId
                val headerId = header.headerId
                val sequenceNumber = header.sequenceNumber
                val duration = if (header.hasDurationMs()) header.durationMs else {
                    ((header.timeRange.durationTicks.toDouble() / header.timeRange.timescale.toDouble()) * 1000.0).toLong()
                }

                if (videoId != this.videoId) {
                    Log.e(TAG, "processPart: Received unexpected media header for $videoId")
                    throw Exception("Header mismatch")
                }

                val format = initializedFormats[header.formatId.itag]!!

                if (format.downloadedSegments.containsKey(sequenceNumber)) {
                    Log.w(TAG, "processPart: Segment $sequenceNumber is already downloaded. Ignoring.")
                    return
                }

                Log.v(TAG, "processPart: Enqueuing partial segment $headerId")
                partialSegments[headerId] = Segment(
                    header = header,
                    sequenceNumber = sequenceNumber,
                    data = mutableListOf(),
                    duration = duration
                )
            }

            UMPPartId.MEDIA -> {
                val parser = UmpParser(part.data)
                val headerId = parser.readVarint()?.toInt()!!

                // repeated segment are skipped, when their header is found and their not added
                // to the partial segment queue
                val segment = partialSegments[headerId] ?: return
                segment.data.add(parser.data())
            }

            UMPPartId.MEDIA_END -> {
                val parser = UmpParser(part.data)
                val headerId = parser.readVarint()?.toInt()!!
                val segment = partialSegments.remove(headerId) ?: return
                Log.v(TAG, "processPart: Dequeuing partial segment $headerId")

                val segmentLength = segment.length()
                if (segmentLength != segment.header.contentLength.toInt()) {
                    Log.w(
                        TAG,
                        "processPart: Content length mismatch for segment $headerId: expected ${segment.header.contentLength}, got $segmentLength"
                    )
                    throw Exception("Content length mismatch")
                }

                val format = initializedFormats[segment.header.itag]!!
                format.downloadedDuration += segment.duration
                format.downloadedSegments[segment.sequenceNumber] = segment
            }

            UMPPartId.NEXT_REQUEST_POLICY -> {
                val policy = NextRequestPolicy.parseFrom(part.data)
                backoffTime = policy.backoffTimeMs
                playbackCookie = policy.playbackCookie
            }

            UMPPartId.FORMAT_INITIALIZATION_METADATA -> {
                val metadata = FormatInitializationMetadata.parseFrom(part.data)

                val duration = metadata.endTimeMs
                val endSegmentNumber = metadata.endSegmentNumber
                val formatId = metadata.formatId
                val itag = formatId.itag

                if (initializedFormats.containsKey(itag)) {
                    Log.w(TAG, "processPart: Skipping already initialized format `$itag`")
                    return
                }

                val format = InitializedFormat(
                    id = formatId,
                    endSegmentNumber = endSegmentNumber,
                    duration = duration
                )
                initializedFormats[itag] = format
            }

            UMPPartId.SABR_REDIRECT -> {
                val redirect = SabrRedirect.parseFrom(part.data)
                url = redirect.url
            }

            UMPPartId.SABR_CONTEXT_UPDATE -> {
                val contextUpdate = SabrContextUpdate.parseFrom(part.data)

                if (contextUpdate.writePolicy == SabrContextWritePolicy.KEEP_EXISTING &&
                    sabrContexts.containsKey(contextUpdate.type)) {
                    return
                }

                if (contextUpdate.sendByDefault) {
                    activeSabrContexts.add(contextUpdate.type)
                }

                sabrContexts[contextUpdate.type] =
                    SabrContext.newBuilder().setType(contextUpdate.type)
                        .setValue(contextUpdate.value).build()
            }

            UMPPartId.SABR_CONTEXT_SENDING_POLICY -> {
                val policy = SabrContextSendingPolicy.parseFrom(part.data)

                policy.startPolicyList.forEach { startPolicy ->
                    if (!activeSabrContexts.contains(startPolicy)) {
                        Log.v(TAG, "processPart: Server requested to enable SABR Context Update ($startPolicy)")
                        activeSabrContexts.add(startPolicy)
                    }
                }

                policy.stopPolicyList.forEach { stopPolicy ->
                    if (activeSabrContexts.contains(stopPolicy)) {
                        Log.v(TAG, "processPart: Server requested to disable SABR Context Update ($stopPolicy)")
                        activeSabrContexts.remove(stopPolicy)
                    }
                }

                policy.discardPolicyList.forEach { discardPolicy ->
                    if (activeSabrContexts.contains(discardPolicy)) {
                        Log.v(TAG, "processPart: Server requested to discard SABR Context Update ($discardPolicy)")
                        sabrContexts.remove(discardPolicy)
                    }
                }
            }

            UMPPartId.STREAM_PROTECTION_STATUS -> {
                val status = StreamProtectionStatus.parseFrom(part.data)
                // https://github.com/coletdjnz/yt-dlp-dev/blob/5c0c2963396009a92101bc6e038b61844368409d/yt_dlp/extractor/youtube/_streaming/sabr/part.py
                when (status.status) {
                    1 -> Log.i(TAG, "processPart: [StreamProtectionStatus] OK")
                    2 -> Log.i(TAG, "processPart: [StreamProtectionStatus] Attestation pending.")
                    3 -> throw Exception("Attestation required")
                    else -> Log.e(TAG, "processPart: Unknown StreamProtectionStatus (${status.status})")
                }
            }

            UMPPartId.SABR_ERROR -> {
                val error = SabrError.parseFrom(part.data)
                Log.e(TAG, "processPart: Received SABR error: ${error.type} (${error.code})")
                throw Exception("SABR error: ${error.type}")
            }

            else -> {}
        }
    }

    companion object {
        private const val CONTENT_TYPE = "application/x-protobuf"
        private const val ENCODING = "identity"
        private const val ACCEPT = "application/vnd.yt-ump"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    }
}