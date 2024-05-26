package com.github.libretube.ui.models

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.RetrofitInstance
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
import retrofit2.HttpException
import java.io.IOException

@UnstableApi
class PlayerViewModel : ViewModel() {
    var player: ExoPlayer? = null
    var trackSelector: DefaultTrackSelector? = null

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

    val isMiniPlayerVisible = MutableLiveData(false)
    val isFullscreen = MutableLiveData(false)

    var maxSheetHeightPx = 0

    var sponsorBlockEnabled = PlayerHelper.sponsorBlockEnabled

    /**
     * @return pair of the stream info and the error message if the request was not successful
     */
    suspend fun fetchVideoInfo(context: Context, videoId: String): Pair<Streams?, String?> =
        withContext(Dispatchers.IO) {
            if (isOrientationChangeInProgress && streamsInfo != null) return@withContext streamsInfo to null

            streamsInfo = try {
                RetrofitInstance.api.getStreams(videoId).deArrow(videoId)
            } catch (e: IOException) {
                return@withContext null to context.getString(R.string.unknown_error)
            } catch (e: HttpException) {
                val errorMessage = e.response()?.errorBody()?.string()?.runCatching {
                    JsonHelper.json.decodeFromString<Message>(this).message
                }?.getOrNull() ?: context.getString(R.string.server_error)
                return@withContext null to errorMessage
            }

            return@withContext streamsInfo to null
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

    @OptIn(UnstableApi::class)
    fun keepOrCreatePlayer(context: Context): Pair<ExoPlayer, DefaultTrackSelector> {
        if (!isOrientationChangeInProgress || player == null || trackSelector == null) {
            this.trackSelector = DefaultTrackSelector(context)
            this.player = PlayerHelper.createPlayer(context, trackSelector!!, false)
        }

        return this.player!! to this.trackSelector!!
    }
}
