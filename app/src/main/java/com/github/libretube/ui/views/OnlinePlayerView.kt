package com.github.libretube.ui.views

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import androidx.media3.exoplayer.trackselection.TrackSelector
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import com.github.libretube.R
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.WindowHelper
import com.github.libretube.obj.BottomSheetItem
import com.github.libretube.ui.extensions.toggleSystemBars
import com.github.libretube.ui.interfaces.OnlinePlayerOptions
import com.github.libretube.ui.models.PlayerViewModel

class OnlinePlayerView(
    context: Context,
    attributeSet: AttributeSet? = null
) : CustomExoPlayerView(context, attributeSet) {
    private var playerOptions: OnlinePlayerOptions? = null
    private var playerViewModel: PlayerViewModel? = null
    private var trackSelector: TrackSelector? = null
    private var viewLifecycleOwner: LifecycleOwner? = null
    var autoplayEnabled = PlayerHelper.autoPlayEnabled

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun getOptionsMenuItems(): List<BottomSheetItem> {
        return super.getOptionsMenuItems() +
            listOf(
                BottomSheetItem(
                    context.getString(R.string.quality),
                    R.drawable.ic_hd,
                    { "${player?.videoSize?.height}p" }
                ) {
                    playerOptions?.onQualityClicked()
                },
                BottomSheetItem(
                    context.getString(R.string.audio_track),
                    R.drawable.ic_audio,
                    {
                        trackSelector?.parameters?.preferredAudioLanguages?.firstOrNull()
                    }
                ) {
                    playerOptions?.onAudioStreamClicked()
                },
                BottomSheetItem(
                    context.getString(R.string.captions),
                    R.drawable.ic_caption,
                    {
                        if (trackSelector != null && trackSelector!!.parameters.preferredTextLanguages.isNotEmpty()) {
                            trackSelector!!.parameters.preferredTextLanguages[0]
                        } else {
                            context.getString(R.string.none)
                        }
                    }
                ) {
                    playerOptions?.onCaptionsClicked()
                },
                BottomSheetItem(
                    context.getString(R.string.stats_for_nerds),
                    R.drawable.ic_info
                ) {
                    playerOptions?.onStatsClicked()
                }
            )
    }

    fun initPlayerOptions(
        playerViewModel: PlayerViewModel,
        viewLifecycleOwner: LifecycleOwner,
        trackSelector: TrackSelector,
        playerOptions: OnlinePlayerOptions
    ) {
        this.playerViewModel = playerViewModel
        this.viewLifecycleOwner = viewLifecycleOwner
        this.trackSelector = trackSelector
        this.playerOptions = playerOptions

        playerViewModel.isFullscreen.observe(viewLifecycleOwner) { isFullscreen ->
            WindowHelper.toggleFullscreen(activity, isFullscreen)
            updateTopBarMargin()
        }

        setControllerVisibilityListener(
            ControllerVisibilityListener { visibility ->
                playerViewModel.isFullscreen.value?.let { isFullscreen ->
                    if (!isFullscreen) return@let
                    // Show status bar only not navigation bar if the player controls are visible and hide it otherwise
                    activity.toggleSystemBars(
                        types = WindowInsetsCompat.Type.statusBars(),
                        showBars = visibility == View.VISIBLE
                    )
                }
            }
        )

        binding.autoPlay.isChecked = autoplayEnabled

        binding.autoPlay.setOnCheckedChangeListener { _, isChecked ->
            autoplayEnabled = isChecked
        }
    }

    override fun hideController() {
        super.hideController()

        if (playerViewModel?.isFullscreen?.value == true) {
            WindowHelper.toggleFullscreen(activity, true)
        }
        updateTopBarMargin()
    }

    override fun onSwipeCenterScreen(distanceY: Float) {
        super.onSwipeCenterScreen(distanceY)
        playerViewModel?.isFullscreen?.value = false
    }

    override fun getTopBarMarginDp(): Int {
        return when {
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE -> 15
            playerViewModel?.isFullscreen?.value == true -> 20
            else -> super.getTopBarMarginDp()
        }
    }

    override fun isFullscreen(): Boolean {
        return playerViewModel?.isFullscreen?.value ?: super.isFullscreen()
    }
}
