package com.github.libretube.util

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist.Rendition
import androidx.media3.exoplayer.hls.playlist.HlsPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParserFactory
import androidx.media3.exoplayer.upstream.ParsingLoadable
import java.io.InputStream

/**
 * A YouTube HLS playlist parser which adds role flags to audio formats with track types.
 *
 * YouTube does not provide descriptive audio track types in a standard way and there is no standard
 * way to tell whether an audio track is a dubbed track.
 *
 * However, this information is still provided in the track name, a non-standard property
 * (`YT-EXT-XTAGS` which has its value encoded as a protocol buffer) and the stream manifest URL.
 *
 * This playlist parser adds track types to audio formats which have this information, by parsing
 * the manifest URL of these formats.
 *
 * It relies internally on a default [HlsPlaylistParser] and processes audio tracks when the
 * [HlsPlaylistParser] instance used parsed the manifest.
 */
@OptIn(UnstableApi::class)
class YoutubeHlsPlaylistParser : ParsingLoadable.Parser<HlsPlaylist> {

    /**
     * Factory to create [YoutubeHlsPlaylistParser] instances.
     */
    class Factory : HlsPlaylistParserFactory {
        override fun createPlaylistParser() = YoutubeHlsPlaylistParser()

        override fun createPlaylistParser(
            multivariantPlaylist: HlsMultivariantPlaylist,
            previousMediaPlaylist: HlsMediaPlaylist?
        ) = YoutubeHlsPlaylistParser(multivariantPlaylist, previousMediaPlaylist)
    }

    /**
     * The [HlsPlaylistParser] instance which is used to delegate parsing of HLS manifests.
     */
    private val hlsPlaylistParser: HlsPlaylistParser

    /**
     * @see [HlsPlaylistParser] no-parameters constructor
     */
    private constructor() {
        this.hlsPlaylistParser = HlsPlaylistParser()
    }

    /**
     * @see [HlsPlaylistParser] constructor with [HlsMultivariantPlaylist] and [HlsMediaPlaylist]
     * parameters
     */
    private constructor(
        multivariantPlaylist: HlsMultivariantPlaylist,
        previousMediaPlaylist: HlsMediaPlaylist?
    ) {
        this.hlsPlaylistParser = HlsPlaylistParser(multivariantPlaylist, previousMediaPlaylist)
    }

    /**
     * Parse a YouTube HLS playlist.
     *
     * If the given HLS playlist type is not a [HlsMultivariantPlaylist], it is returned as it is.
     *
     * If that's the case, audios extracted from the playlist are parsed and the good audio track
     * type is set to each audio, if applicable and if this information is available.
     *
     * @param uri         the source [Uri] of the response, after any redirection.
     * @param inputStream an [InputStream] from which the response data can be read.
     *
     * @return a [HlsPlaylist] which is either the original one parsed by the delegated
     * [HlsPlaylistParser] instance or a [HlsMultivariantPlaylist] on which audio formats have been
     * edited to add the role track type flags to the existing ones on them if needed
     */
    override fun parse(uri: Uri, inputStream: InputStream): HlsPlaylist {
        val hlsPlaylist = hlsPlaylistParser.parse(uri, inputStream)
        if (hlsPlaylist !is HlsMultivariantPlaylist) {
            return hlsPlaylist
        }

        val hlsMultivariantPlaylist: HlsMultivariantPlaylist = hlsPlaylist

        return HlsMultivariantPlaylist(
            hlsMultivariantPlaylist.baseUri,
            hlsMultivariantPlaylist.tags,
            hlsMultivariantPlaylist.variants,
            hlsMultivariantPlaylist.videos,
            getAudioRenditionsWithTrackTypeSet(hlsMultivariantPlaylist.audios),
            hlsMultivariantPlaylist.subtitles,
            hlsMultivariantPlaylist.closedCaptions,
            // YouTube HLS playlists have only demuxed formats, so it should be not needed to parse
            // the muxed format, as it would be always null in this case
            hlsMultivariantPlaylist.muxedAudioFormat,
            hlsMultivariantPlaylist.muxedCaptionFormats,
            hlsMultivariantPlaylist.hasIndependentSegments,
            hlsMultivariantPlaylist.variableDefinitions,
            hlsMultivariantPlaylist.sessionKeyDrmInitData
        )
    }

    /**
     * Get audio renditions with track types set on them, if they are not already set.
     *
     * This function parses audio track types from the stream manifest URL, by parsing the `acont`
     * value of the `xtags` property of the value of the `sgoap` "path parameter".
     * It adds then the corresponding ExoPlayer role flag in the audio format, if it has been not
     * already set (this should never be the case).
     *
     * Any failure when the audio track type property could not parsed when it should (audio track
     * types are only available on videos with multiple audio tracks) is ignored and the stream is
     * kept as it is in this case.
     *
     * @param hlsMultivariantPlaylistAudios the list of audio [Rendition]s of a
     * [HlsMultivariantPlaylist]
     * @return a new list of audio [Rendition]s with audio track types set in the role flags of the
     * audio formats
     */
    private fun getAudioRenditionsWithTrackTypeSet(
        hlsMultivariantPlaylistAudios: List<Rendition>
    ): List<Rendition> {
        return hlsMultivariantPlaylistAudios.map {
            // Add the audio stream as it is if no path segments has been found
            // This should never happen, as YouTube always uses path segments for their HLS URLs
            val pathSegments = it.url?.pathSegments ?: return@map it

            // Path segments after the videoplayback one can be also converted to query parameters
            // (the contrary is also possible), so these segments work like keys and values in a map
            val sgoapPathParameterNameIndex = pathSegments.indexOf(SGOAP_PATH_PARAMETER)

            // Return the audio stream as it is if no audio track type parameter has been found
            if (sgoapPathParameterNameIndex == -1) {
                return@map it
            }

            val sgoapPathParameterValueIndex = sgoapPathParameterNameIndex + 1

            if (sgoapPathParameterValueIndex == pathSegments.size) {
                return@map it
            }

            Rendition(
                it.url,
                createAudioFormatFromAcountValue(
                    pathSegments[sgoapPathParameterValueIndex],
                    it.format
                ),
                it.groupId,
                it.name
            )
        }
    }

    /**
     * Create an audio [Format] based on an existing one and the `acont` property value of the
     * `xtags` one, from a `sgoap` path parameter value.
     *
     * If the `acont` property has been found in the `sgoap` path parameter value provided, an
     * audio track type role flag is added to the existing ones, if it isn't already added, using
     * [getFullAudioRoleFlags]; otherwise, the format is kept as it is.
     *
     * @param sgoapPathParameterValue a `sgoap` path parameter value
     * @param audioFormat             the audio format linked to the URL from which the
     *                                `sgoapPathParameterValue` parameter comes from
     * @return an [Format] based of the original one provided or the original one if the `acont`
     * property has been not found
     */
    private fun createAudioFormatFromAcountValue(
        sgoapPathParameterValue: String,
        audioFormat: Format
    ): Format {
        XTAGS_ACONT_VALUE_REGEX.find(sgoapPathParameterValue)?.groupValues?.get(1)
            ?.let { acontValue ->
                return audioFormat.buildUpon()
                    .setRoleFlags(
                        getFullAudioRoleFlags(
                            audioFormat.roleFlags,
                            acontValue
                        )
                    )
                    .build()
            }

        // If no info about format being original, dubbed or descriptive, return the format as it is
        return audioFormat
    }

    /**
     * Get the full audio role flags of an audio track.
     *
     * Full role flags are the existing flags parsed by ExoPlayer and the flags coming from the
     * audio track type parsed from the `acont` property value of the stream manifest URL.
     *
     * The following table describes what value is parsed
     *
     * | `acont` value  | Role flag added from [ExoPlayer track role flags][C.RoleFlags] |
     * | ------------- | ------------- |
     * | `dubbed`  | [C.ROLE_FLAG_DUB]  |
     * | `descriptive`  | [C.ROLE_FLAG_DESCRIBES_VIDEO]  |
     * | `original`  | [C.ROLE_FLAG_MAIN]  |
     * | everything else  | [C.ROLE_FLAG_ALTERNATE]  |
     *
     * @param roleFlags the current role flags of the audio track
     * @param acontValue the value of the `acont` property
     * @return the full audio role flags of the audio track like described above
     */
    private fun getFullAudioRoleFlags(
        roleFlags: Int,
        acontValue: String
    ): Int {
        val acontRoleFlags = when (acontValue.lowercase()) {
            "dubbed" -> C.ROLE_FLAG_DUB
            "descriptive" -> C.ROLE_FLAG_DESCRIBES_VIDEO
            "original" -> C.ROLE_FLAG_MAIN
            // Original audio tracks without other audio track should not have the `acont` property
            // nor the `xtags` one, so the the track should be not set as the main one
            // The alternate role flag should be the most relevant flag in this case
            else -> C.ROLE_FLAG_ALTERNATE
        }

        // Add this flag to the existing ones (if it has been not already added) and return the
        // result of this operation
        return roleFlags or acontRoleFlags
    }

    companion object {

        /**
         * Constant for the `sgoap` "path parameter" name.
         *
         * YouTube HLS streams are for most of them, the same streams delivered as the DASH ones.
         * The service provide information on the original stream of an HLS stream URL in "path
         * parameters", `sgovp` for video streams and `sgoap` for audio streams.
         *
         * This information should include, for audio streams, the track type when there is multiple
         * audio tracks in a video, which is what we need to get.
         */
        private const val SGOAP_PATH_PARAMETER = "sgoap"

        /**
         * Regular expression to find the `acont` property value of the `xtags` property value from
         * a `sgoap` "path parameter" value of a YouTube HLS streaming URL.
         *
         * The `acont` property provides the track type of an audio stream, when a video of the
         * service has multiple audio tracks.
         */
        private val XTAGS_ACONT_VALUE_REGEX = Regex("xtags=.*acont=(.[^:]+)")
    }
}
