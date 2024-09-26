package com.github.libretube.ui.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.StreamsExtractor
import com.github.libretube.api.obj.Message
import com.github.libretube.api.obj.Segment
import com.github.libretube.api.obj.Streams
import com.github.libretube.api.obj.Subtitle
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.util.NowPlayingNotification
import com.github.libretube.util.deArrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

@UnstableApi
class PlayerViewModel(
    val player: ExoPlayer,
    val trackSelector: DefaultTrackSelector,
) : ViewModel() {

    // data to remember for recovery on orientation change
    private var streamsInfo: Streams? = null
    var nowPlayingNotification: NowPlayingNotification? = null
    var segments = listOf<Segment>()
    var currentSubtitle = Subtitle(code = PlayerHelper.defaultSubtitleCode)
    var sponsorBlockConfig = PlayerHelper.getSponsorBlockCategories()

    /**
     * Whether an orientation change is in progress, so that the current player should be continued to use
     *
     * Set to true if the activity will be recreated due to an orientation change
     */
    var isOrientationChangeInProgress = false
    var sponsorBlockEnabled = PlayerHelper.sponsorBlockEnabled

    /**
     * @return pair of the stream info and the error message if the request was not successful
     */
    suspend fun fetchVideoInfo(context: Context, videoId: String): Pair<Streams?, String?> =
        withContext(Dispatchers.IO) {
            if (isOrientationChangeInProgress && streamsInfo != null) return@withContext streamsInfo to null

            return@withContext try {
                StreamsExtractor.extractStreams(videoId).deArrow(videoId) to null
            } catch (e: Exception) {
                return@withContext null to StreamsExtractor.getExtractorErrorMessageString(context, e)
            }
        }

    suspend fun fetchSponsorBlockSegments(videoId: String) = withContext(Dispatchers.IO) {
        if (sponsorBlockConfig.isEmpty() || isOrientationChangeInProgress) return@withContext

        runCatching {
            segments =
                RetrofitInstance.api.getSegments(
                    videoId,
                    JsonHelper.json.encodeToString(sponsorBlockConfig.keys)
                ).segments
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val context = this[APPLICATION_KEY]!!
                val trackSelector = DefaultTrackSelector(context)
                PlayerViewModel(
                    player = PlayerHelper.createPlayer(context, trackSelector, false),
                    trackSelector = trackSelector,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}