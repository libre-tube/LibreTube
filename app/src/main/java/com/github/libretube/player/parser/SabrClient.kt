package com.github.libretube.player.parser

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.github.libretube.LibreTubeApp
import com.github.libretube.api.poToken.PoTokenGenerator
import com.github.libretube.player.manifest.Representation
import com.github.libretube.player.manifest.SabrManifest
import com.github.libretube.ui.dialogs.ShareDialog
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
import video_streaming.BufferedRangeOuterClass.BufferedRange
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
import java.time.Instant

class PlaybackRequest(
    /* Format for which new media data is being requested */
    val format: FormatId,
    /* Position of the player in milliseconds */
    val playerPosition: Long,
    /* Multiplier applied to the speed at which content is played. */
    val playbackSpeed: Float,
    /* Sequence number of which segment is loaded */
    val segment: Long,
    /* List of segments which are buffered for the format */
    val segmentStartTimeMs: Long,
    /* List of segments which are buffered for the format */
    val bufferedSegments: List<Long>,
) {
    companion object {
        fun initRequest(
            format: FormatId, playerPosition: Long, playbackSpeed: Float,
        ): PlaybackRequest = PlaybackRequest(
            format, playerPosition, playbackSpeed, 0, 0, emptyList()
        )
    }
}

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
    /** Segments that have been downloaded for this format. */
    val bufferedSegments: MutableMap<Long, Segment> = mutableMapOf(),
    /** Sequence number of the last segment in the format. */
    val endSegmentNumber: Long,
    /** Initial segment containing metadata about the stream,
     *  such as the position of the other segments.
     **/
    var initSegment: Segment? = null,
    /** Duration of the format in milliseconds. */
    val duration: Long,
) {
    /** Returns a list of all downloaded segments for the format. */
    fun getSegment(sequenceNumber: Long): Segment? {
        val segment = downloadedSegments.remove(sequenceNumber)
            ?: initSegment?.takeIf { it.sequenceNumber == sequenceNumber }
            ?: return null
        // mark retrieved segment as buffered
        bufferedSegments[sequenceNumber] = segment
        return segment
    }

    /** Returns a list of all downloaded segments for the format. */
    fun buildBufferedRanges(): List<BufferedRange> =
        bufferedSegments.entries.union(downloadedSegments.entries).sortedBy { it.key }
        .fold(mutableListOf<MutableList<Pair<Long, Segment>>>()) { acc, (id, segment) ->
            val previousId = acc.lastOrNull()?.lastOrNull()?.first
            if (previousId == null || previousId + 1 != id) {
                //we found a discontinuity, create a new partition
                acc.add(mutableListOf());
            }
            acc.lastOrNull()!!.add(Pair(id, segment))
            acc
        }.map { partition ->
            val duration = partition.sumOf { it.second.duration }
            val (firstId, firstSegment) = partition.first()
            BufferedRange.newBuilder().setFormatId(id).setStartTimeMs(firstSegment.header.startMs)
                .setDurationMs(duration).setStartSegmentIndex(firstId.toInt())
                .setEndSegmentIndex(partition.last().first.toInt()).build()
        }

    /**
     * Whether the format has non-retrieved data.
     */
    fun hasSegment(segmentNumber: Long): Boolean =
        downloadedSegments.containsKey(segmentNumber)
}

/**
 * A SABR/UMP streaming client.
 *
 * Handles the fetching and processing of streaming media data using the UMP protocol.
 */
@OptIn(UnstableApi::class)
class SabrClient private constructor(
    /** Unique identifier for the SABR stream resource. */
    private val videoId: String,
    /** The URL pointing to the SABR/UMP stream. */
    var url: String,
    /** UStreamer configuration data. */
    private val ustreamerConfig: ByteString,
) {

    /** Generator to create PoTokens. */
    private var poTokenGenerator = PoTokenGenerator()
    /** Po (Proof of Origin) Token. */
    private var poToken: ByteString? = null

    private var fatalError: SabrError? = null
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)

    /** Audio format video format */
    private lateinit var audioFormat: Representation
    /** Optional video format */
    private var videoFormat: Representation? = null

    constructor(manifest: SabrManifest) : this(
        manifest.videoId,
        manifest.serverAbrStreamingUri.toString(),
        ByteString.copyFrom(manifest.videoPlaybackUstreamerConfig)
    )

    /**
     * Initialized formats.
     *
     * A format is initialized when the stream sends a [UMPPartId.FORMAT_INITIALIZATION_METADATA] part,
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
                .addHeader("Origin", ShareDialog.YOUTUBE_FRONTEND_URL)
                .addHeader("Referer", "${ShareDialog.YOUTUBE_FRONTEND_URL}/")
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

    /** Timestamp of the last seek */
    var lastSeekMs: Long? = null

    /** Timestamp when the last request was made  */
    private var lastRequestMs: Long? = null

    /**
     * Timestamp when the user/player last selected a format.
     *
     * For us, all format selections are manual, as we do not let the server decide the format.
     **/
    var lastManualFormatSelectionMs: Long? = null

    /**
     * Timestamp when the user last made an action.
     *
     * This is likely the same as [lastManualFormatSelectionMs] for us,
     * as we handle no other actions.
     **/
    var lastActionMs: Long? = null


    private val bandwidthEstimator = DefaultBandwidthMeter.getSingletonInstance(LibreTubeApp.instance)

    @OptIn(UnstableApi::class)
    fun selectFormat(representation: Representation) {
        if (MimeTypes.isAudio(representation.format.containerMimeType)) {
            audioFormat = representation
        } else if (MimeTypes.isVideo(representation.format.containerMimeType)) {
            videoFormat = representation
        }
    }

    fun getNextSegment(playbackRequest: PlaybackRequest): Segment? {
        if (fatalError != null) {
            throw Exception("SABR error: ${fatalError!!.type}")
        }
        val itag = playbackRequest.format.itag

        // the player should never request data past the end of the stream
        assert(
            playbackRequest.playerPosition < (initializedFormats[itag]?.duration ?: Long.MAX_VALUE)
        ) { "Requested segment for finished format" }

        Log.d(
            TAG,
            "getNextSegment: loading media data for $itag at position ${playbackRequest.playerPosition}"
        )

        // synchronize buffered segments with the actually buffered segments from the player
        initializedFormats[itag]?.bufferedSegments?.keys?.retainAll(playbackRequest.bufferedSegments)

        return runBlocking {
            // ensure that the data is only ever accessed by a single thread
            withContext(dispatcher) {
                var format = initializedFormats[itag]
                if (format == null || !format.hasSegment(playbackRequest.segment)) {
                    // fetch new data
                    media(playbackRequest)
                }
                format = format ?: initializedFormats[itag]
                return@withContext format?.getSegment(playbackRequest.segment)
            }
        }
    }


    /**
     * Extracts the raw media data from the stream.
     *
     * The data is returned as a pair of lists: (audio segments, video segments).
     */
    private suspend fun media(playbackRequest: PlaybackRequest) {
        // update currently held UMP data
        val data = fetchStreamData(playbackRequest, audioFormat, videoFormat)

        val parser = UmpParser(data)
        while (true) {
            val part = parser.readPart() ?: break
            processPart(part)
        }
        assert(parser.data().isEmpty()) { "Parser has left-over data" }
    }

    /**
     * Fetches streaming data from the URL.
     */
    private suspend fun fetchStreamData(
        playbackRequest: PlaybackRequest,
        audioFormat: Representation,
        videoFormat: Representation?,
    ): ByteArray {
        backoffTime?.let { backoff ->
            Log.i(TAG, "fetchStreamData: Waiting for ${backoff}ms before making a request")
            delay(backoff.toLong())
            backoffTime = null
        }

        if (poToken == null) {
            poToken = generatePoToken()
        }

        val now = Instant.now().toEpochMilli()
        val xtags = Xtags(audioFormat.formatId().xtags)

        val clientState = ClientAbrState.newBuilder()
            // we pretend we're slightly in the previous (n-1) segment, so we get n-th segment, instead of the (n+1)-th one
            .setPlayerTimeMs(playbackRequest.segmentStartTimeMs.minus(500).coerceAtLeast(0))
            .setEnabledTrackTypesBitfield(if (videoFormat == null) 1 else 0)
            .setPlaybackRate(playbackRequest.playbackSpeed)
            .setElapsedWallTimeMs(lastRequestMs?.let { now -  it } ?: 0 )
            .setTimeSinceLastSeek(lastSeekMs?.let { now - it } ?: 0)
            .setTimeSinceLastManualFormatSelectionMs(lastManualFormatSelectionMs?.let { now - it } ?: 0)
            .setTimeSinceLastActionMs(lastActionMs?.let { now - it } ?: 0)
            .setAudioTrackId(audioFormat.stream.audioTrackId ?: "")
            .setDrcEnabled(audioFormat.stream.isDrc ?: false || xtags.isDrcAudio())
            .setEnableVoiceBoost(xtags.isVoiceBoosted())
            .setClientViewportIsFlexible(false)
            .setBandwidthEstimate(bandwidthEstimator.bitrateEstimate)
            .setVisibility(1)
            .build()

        val playbackRequest = VideoPlaybackAbrRequest.newBuilder().setClientAbrState(clientState)
            .addAllSelectedFormatIds(initializedFormats.values.map { it.id }.toList())
            .setVideoPlaybackUstreamerConfig(ustreamerConfig)
            .addAllPreferredAudioFormatIds(listOf(audioFormat.formatId()))
            .addAllPreferredVideoFormatIds(listOfNotNull(videoFormat?.formatId()))
            .addAllSelectedFormatIds(initializedFormats.map { it.value.id }.toList())
            .addAllBufferedRanges(initializedFormats.values.flatMap { it.buildBufferedRanges() })
            .setStreamerContext(
                StreamerContext.newBuilder()
                    .setPoToken(poToken ?: ByteString.empty())
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

        // ideally we would use HTTP3 here, like the official, however okhttp does not support it
        val request = Request.Builder()
            .url(url)
            .post(
                playbackRequest.toByteArray()
                    .toRequestBody(CONTENT_TYPE.toMediaType())
            )
            .build()

        lastRequestMs = Instant.now().toEpochMilli()
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
                    ((header.timeRange.durationTicks.toDouble() / header.timeRange.timescale.toDouble()) * 1000).toLong()
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
                format.downloadedSegments[segment.sequenceNumber] = segment

                if (segment.header.isInitSeg) {
                    format.initSegment = segment
                }
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

            UMPPartId.RELOAD_PLAYER_RESPONSE -> {
                // this is called if the streams are expired or a new configuration feature needs to be set
                // in either case, we purposefully crash the player here, as the first one is a rare edge-case
                // and the second one cannot be handled
                throw Exception("Server requested player reload")
            }

            UMPPartId.STREAM_PROTECTION_STATUS -> {
                val status = StreamProtectionStatus.parseFrom(part.data)
                // https://github.com/coletdjnz/yt-dlp-dev/blob/5c0c2963396009a92101bc6e038b61844368409d/yt_dlp/extractor/youtube/_streaming/sabr/part.py
                when (status.status) {
                    1 -> Log.i(TAG, "processPart: [StreamProtectionStatus] OK")
                    2 -> {
                        Log.i(TAG, "processPart: [StreamProtectionStatus] Attestation pending.")
                        // try to regenerate the poToken for the next request
                        poToken = generatePoToken()
                    }
                    // we assume that we got a attestation pending warning before and already tried to regenerate the token,
                    // but it's not accepted, so we bail
                    3 -> throw Exception("Attestation required")
                    else -> Log.e(TAG, "processPart: Unknown StreamProtectionStatus (${status.status})")
                }
            }

            UMPPartId.SABR_ERROR -> {
                val error = SabrError.parseFrom(part.data)
                Log.e(TAG, "processPart: Received SABR error: ${error.type} (${error.code})")
                fatalError = error
                throw Exception("SABR error: ${error.type}")
            }

            else -> {
                Log.w(TAG, "processPart: Unhandled UMP part ${part.type}")
            }
        }
    }

    /**
     * Generates new poToken using the set generator.
     *
     * NOTE: This should use the same client used for the requests made for the server
     */
    fun generatePoToken() : ByteString? {
        val poTokenResult = poTokenGenerator.getWebClientPoToken(videoId) ?: return null
        return ByteString.copyFrom(poTokenResult.streamingDataPoToken!!.toByteArray())
    }

    companion object {
        private const val TAG = "SabrStream"
        private const val CONTENT_TYPE = "application/x-protobuf"
        private const val ENCODING = "identity"
        private const val ACCEPT = "application/vnd.yt-ump"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    }
}