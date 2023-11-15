package com.github.libretube.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.Window
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.C
import androidx.media3.exoplayer.trackselection.TrackSelector
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.WindowHelper
import com.github.libretube.obj.BottomSheetItem
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.dialogs.SubmitSegmentDialog
import com.github.libretube.ui.interfaces.OnlinePlayerOptions
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.util.PlayingQueue

class OnlinePlayerView(
    context: Context,
    attributeSet: AttributeSet? = null
) : CustomExoPlayerView(context, attributeSet) {
    private var playerOptions: OnlinePlayerOptions? = null
    private var playerViewModel: PlayerViewModel? = null
    private var trackSelector: TrackSelector? = null
    private var viewLifecycleOwner: LifecycleOwner? = null

    /**
     * The window that needs to be addressed for showing and hiding the system bars
     * If null, the activity's default/main window will be used
     */
    var currentWindow: Window? = null

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun getOptionsMenuItems(): List<BottomSheetItem> {
        return super.getOptionsMenuItems() +
            listOf(
                BottomSheetItem(
                    context.getString(R.string.quality),
                    R.drawable.ic_hd,
                    this::getCurrentResolutionSummary
                ) {
                    playerOptions?.onQualityClicked()
                },
                BottomSheetItem(
                    context.getString(R.string.audio_track),
                    R.drawable.ic_audio,
                    this::getCurrentAudioTrackTitle
                ) {
                    playerOptions?.onAudioStreamClicked()
                },
                BottomSheetItem(
                    context.getString(R.string.captions),
                    R.drawable.ic_caption,
                    this::getCurrentCaptionLanguage
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

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun getCurrentResolutionSummary(): String {
        val currentQuality = player?.videoSize?.height ?: 0
        var summary = "${currentQuality}p"
        val trackSelector = trackSelector ?: return summary
        val selectedQuality = trackSelector.parameters.maxVideoHeight
        if (selectedQuality == Int.MAX_VALUE) {
            summary += " - ${context.getString(R.string.auto)}"
        } else if (selectedQuality > currentQuality) {
            summary += " - ${context.getString(R.string.resolution_limited)}"
        }
        return summary
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun getCurrentCaptionLanguage(): String {
        return if (trackSelector != null && trackSelector!!.parameters.preferredTextLanguages.isNotEmpty()) {
            trackSelector!!.parameters.preferredTextLanguages[0]
        } else {
            context.getString(R.string.none)
        }
    }

    private fun getCurrentAudioTrackTitle(): String {
        if (player == null) {
            return context.getString(R.string.unknown_or_no_audio)
        }

        // The player reference should be not changed between the null check
        // and its access, so a non null assertion should be safe here
        val selectedAudioLanguagesAndRoleFlags =
            PlayerHelper.getAudioLanguagesAndRoleFlagsFromTrackGroups(
                player!!.currentTracks.groups,
                true
            )

        if (selectedAudioLanguagesAndRoleFlags.isEmpty()) {
            return context.getString(R.string.unknown_or_no_audio)
        }

        // At most one audio track should be selected regardless of audio
        // format or quality
        val firstSelectedAudioFormat = selectedAudioLanguagesAndRoleFlags[0]

        if (selectedAudioLanguagesAndRoleFlags.size == 1 &&
            firstSelectedAudioFormat.first == null &&
            !PlayerHelper.haveAudioTrackRoleFlagSet(
                firstSelectedAudioFormat.second
            )
        ) {
            // Regardless of audio format or quality, if there is only one
            // audio stream which has no language and no role flags, it
            // should mean that there is only a single audio track which
            // has no language or track type set in the video played
            // Consider it as the default audio track (or unknown)
            return context.getString(R.string.default_or_unknown_audio_track)
        }

        return PlayerHelper.getAudioTrackNameFromFormat(
            context,
            firstSelectedAudioFormat
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
            WindowHelper.toggleFullscreen(activity.window, isFullscreen)
            updateTopBarMargin()
        }

        binding.autoPlay.isChecked = PlayerHelper.autoPlayEnabled

        binding.autoPlay.setOnCheckedChangeListener { _, isChecked ->
            PlayerHelper.autoPlayEnabled = isChecked
        }

        binding.sbSubmit.isVisible = PreferenceHelper.getBoolean(PreferenceKeys.CONTRIBUTE_TO_SB, false)
        binding.sbSubmit.setOnClickListener {
            val currentPosition = player?.currentPosition?.takeIf { it != C.TIME_UNSET } ?: 0
            val duration = player?.duration?.takeIf { it != C.TIME_UNSET }
            val videoId = PlayingQueue.getCurrent()?.url?.toID() ?: return@setOnClickListener

            val bundle = bundleOf(
                IntentData.currentPosition to currentPosition,
                IntentData.duration to duration,
                IntentData.videoId to videoId
            )
            val newSubmitSegmentDialog = SubmitSegmentDialog()
            newSubmitSegmentDialog.arguments = bundle
            newSubmitSegmentDialog.show((context as BaseActivity).supportFragmentManager, null)
        }
    }

    override fun getWindow(): Window = currentWindow ?: activity.window

    override fun hideController() {
        super.hideController()

        if (playerViewModel?.isFullscreen?.value == true) {
            toggleSystemBars(false)
        }
        updateTopBarMargin()
    }

    override fun showController() {
        super.showController()

        if (playerViewModel?.isFullscreen?.value == true && !isPlayerLocked) {
            toggleSystemBars(true)
        }
    }

    override fun isFullscreen(): Boolean {
        return playerViewModel?.isFullscreen?.value ?: super.isFullscreen()
    }

    override fun minimizeOrExitPlayer() {
        playerOptions?.exitFullscreen()
    }
}
