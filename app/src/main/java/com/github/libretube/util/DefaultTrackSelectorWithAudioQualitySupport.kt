package com.github.libretube.util

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.NetworkHelper
import com.github.libretube.helpers.PreferenceHelper

/**
 * [DefaultTrackSelector] that automatically chooses the audio quality based on
 * the current preference set in [PreferenceHelper]
 */
@androidx.annotation.OptIn(UnstableApi::class)
class DefaultTrackSelectorWithAudioQualitySupport
    (context: Context) :
    DefaultTrackSelector(context) {
    override fun selectAudioTrack(
        mappedTrackInfo: MappedTrackInfo,
        rendererFormatSupports: Array<out Array<out IntArray>>,
        rendererMixedMimeTypeAdaptationSupports: IntArray,
        params: Parameters
    ): android.util.Pair<ExoTrackSelection.Definition, Int>? {
        val prefKey = if (NetworkHelper.isNetworkMetered(context!!)) {
            PreferenceKeys.PLAYER_AUDIO_QUALITY_MOBILE
        } else {
            PreferenceKeys.PLAYER_AUDIO_QUALITY
        }

        val qualityPref = PreferenceHelper.getString(prefKey, "auto")

        val tracks = super.selectAudioTrack(
            mappedTrackInfo,
            rendererFormatSupports,
            rendererMixedMimeTypeAdaptationSupports,
            params
        ) ?: return null

        val trackGroup = tracks.first.group
        val trackFormats = (0 until trackGroup.length)
            .map { trackGroup.getFormat(it) }
        val selectionIndex = when (qualityPref) {
            "worst" -> {
                trackFormats.indexOf(
                    trackFormats.minBy { it.bitrate }
                )
            }
            "best" -> {
                trackFormats.indexOf(
                    trackFormats.maxBy { it.bitrate }
                )
            }
            else -> {
                return tracks
            }
        }
        return android.util.Pair(
            ExoTrackSelection.Definition(
                tracks.first.group,
                intArrayOf(selectionIndex),
                tracks.first.type
            ), tracks.second
        )
    }
}
