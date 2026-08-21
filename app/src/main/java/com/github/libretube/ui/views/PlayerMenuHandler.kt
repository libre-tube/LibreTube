package com.github.libretube.ui.views

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import androidx.core.os.bundleOf
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.PlayerCommand
import com.github.libretube.extensions.round
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.BottomSheetItem
import com.github.libretube.obj.VideoResolution
import com.github.libretube.services.AbstractPlayerService
import com.github.libretube.ui.interfaces.PlayerOptions
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.ui.sheets.BaseBottomSheet
import com.github.libretube.ui.sheets.PlaybackOptionsSheet
import com.github.libretube.ui.sheets.SleepTimerSheet
import com.github.libretube.ui.sheets.StatsSheet
import com.github.libretube.ui.tools.SleepTimer
import com.github.libretube.util.PlayingQueue
import androidx.fragment.app.FragmentManager
import kotlin.math.ceil

class PlayerMenuHandler(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val playerProvider: () -> Player?,
    private val playerViewModelProvider: () -> PlayerViewModel?,
    private val isVideoLive: () -> Boolean,
    private val getVideoId: () -> String,
    private val isVideoShort: () -> Boolean,
    private val onResizeModeChanged: (Int) -> Unit,
    private val onSubtitleTrackChanged: (String?) -> Unit,
) : PlayerOptions {
    var selectedResolution: Int? = null
    var selectedAudioLanguageAndRoleFlags: Pair<String?, @C.RoleFlags Int>? = null
    var fullscreenResolution: Int? = null
    var noFullscreenResolution: Int? = null

    private val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to R.string.resize_mode_fit,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to R.string.resize_mode_zoom,
        AspectRatioFrameLayout.RESIZE_MODE_FILL to R.string.resize_mode_fill
    )

    val resizeMode: Int
        get() = PreferenceHelper.getInt(
            PreferenceKeys.PLAYER_RESIZE_MODE,
            AspectRatioFrameLayout.RESIZE_MODE_FIT
        )

    fun saveResizeMode(mode: Int) {
        PreferenceHelper.putInt(PreferenceKeys.PLAYER_RESIZE_MODE, mode)
    }

    fun buildOptionsMenuItems(): List<BottomSheetItem> = listOf(
        BottomSheetItem(
            context.getString(R.string.repeat_mode),
            R.drawable.ic_repeat,
            { getRepeatModeSummary() }
        ) { onRepeatModeClicked() },
        BottomSheetItem(
            context.getString(R.string.player_resize_mode),
            R.drawable.ic_aspect_ratio,
            { getResizeModeSummary() }
        ) { onResizeModeClicked() },
        BottomSheetItem(
            context.getString(R.string.playback_speed),
            R.drawable.ic_speed,
            { "${playerProvider()?.playbackParameters?.speed?.round(2)}x" }
        ) { onPlaybackSpeedClicked() },
        BottomSheetItem(
            context.getString(R.string.sleep_timer),
            R.drawable.ic_sleep,
            { getSleepTimerSummary() }
        ) { onSleepTimerClicked() },
        BottomSheetItem(
            context.getString(R.string.quality),
            R.drawable.ic_hd,
            { getCurrentResolutionSummary() }
        ) { onQualityClicked() },
        BottomSheetItem(
            context.getString(R.string.audio_track),
            R.drawable.ic_audio,
            { getCurrentAudioTrackTitle() }
        ) { onAudioStreamClicked() },
        BottomSheetItem(
            context.getString(R.string.captions),
            R.drawable.ic_caption,
            { getCaptionsSummary() }
        ) { onCaptionsClicked() },
        BottomSheetItem(
            context.getString(R.string.stats_for_nerds),
            R.drawable.ic_info
        ) { onStatsClicked() }
    )

    fun buildSbBundleArgs(): Bundle? {
        val player = playerProvider()
        val currentPosition = player?.currentPosition?.takeIf { it != C.TIME_UNSET } ?: 0
        val duration = player?.duration?.takeIf { it != C.TIME_UNSET }
        val videoId = PlayingQueue.getCurrent()?.url?.toID() ?: return null
        return bundleOf(
            IntentData.currentPosition to currentPosition,
            IntentData.duration to duration,
            IntentData.videoId to videoId
        )
    }

    private fun getRepeatModeSummary(): String = when (PlayingQueue.repeatMode) {
        Player.REPEAT_MODE_OFF -> context.getString(R.string.repeat_mode_none)
        Player.REPEAT_MODE_ONE -> context.getString(R.string.repeat_mode_current)
        Player.REPEAT_MODE_ALL -> context.getString(R.string.repeat_mode_all)
        else -> throw IllegalArgumentException()
    }

    private fun getResizeModeSummary(): String =
        resizeModes.find { it.first == resizeMode }?.second?.let { context.getString(it) }.orEmpty()

    private fun getSleepTimerSummary(): String {
        return if (SleepTimer.timeLeftMillis > 0) {
            val minutesLeft = ceil(SleepTimer.timeLeftMillis.toDouble() / DateUtils.MINUTE_IN_MILLIS).toInt()
            context.resources.getQuantityString(R.plurals.minutes_left, minutesLeft, minutesLeft)
        } else {
            context.getString(R.string.disabled)
        }
    }

    private fun getCaptionsSummary(): String =
        playerProvider()?.let { PlayerHelper.getCurrentPlayedCaptionFormat(it)?.language }
            ?: context.getString(R.string.none)

    private fun getCurrentResolutionSummary(): String {
        val currentQuality = playerProvider()?.videoSize?.height ?: 0
        var summary = "${currentQuality}p"
        if (selectedResolution == null) {
            summary += " - ${context.getString(R.string.auto)}"
        } else if ((selectedResolution ?: 0) > currentQuality) {
            summary += " - ${context.getString(R.string.resolution_limited)}"
        }
        return summary
    }

    fun getCurrentAudioTrackTitle(): String {
        val player = playerProvider() ?: return context.getString(R.string.unknown_or_no_audio)

        val selectedAudioLanguagesAndRoleFlags =
            PlayerHelper.getAudioLanguagesAndRoleFlagsFromTrackGroups(
                player.currentTracks.groups, true
            )

        if (selectedAudioLanguagesAndRoleFlags.isEmpty()) {
            return context.getString(R.string.unknown_or_no_audio)
        }

        val firstSelectedAudioFormat = selectedAudioLanguagesAndRoleFlags[0]

        if (selectedAudioLanguagesAndRoleFlags.size == 1 &&
            firstSelectedAudioFormat.first == null &&
            !PlayerHelper.haveAudioTrackRoleFlagSet(firstSelectedAudioFormat.second)
        ) {
            return context.getString(R.string.default_or_unknown_audio_track)
        }

        return PlayerHelper.getAudioTrackNameFromFormat(context, firstSelectedAudioFormat)
    }

    override fun onPlaybackSpeedClicked() {
        (playerProvider() as? MediaController)?.let {
            PlaybackOptionsSheet(it).show(fragmentManager)
        }
    }

    override fun onResizeModeClicked() {
        BaseBottomSheet()
            .setSimpleItems(
                resizeModes.map { context.getString(it.second) },
                preselectedItem = resizeModes.first { it.first == resizeMode }.second.let { context.getString(it) }
            ) { index ->
                saveResizeMode(resizeModes[index].first)
                onResizeModeChanged(resizeModes[index].first)
            }
            .show(fragmentManager)
    }

    override fun onRepeatModeClicked() {
        BaseBottomSheet()
            .setSimpleItems(
                PlayerHelper.repeatModes.map { context.getString(it.second) },
                preselectedItem = PlayerHelper.repeatModes
                    .firstOrNull { it.first == PlayingQueue.repeatMode }
                    ?.second?.let { context.getString(it) }
            ) { index ->
                PlayingQueue.repeatMode = PlayerHelper.repeatModes[index].first
            }
            .show(fragmentManager)
    }

    override fun onSleepTimerClicked() {
        SleepTimerSheet().show(fragmentManager)
    }

    override fun onCaptionsClicked() {
        val player = playerProvider() ?: return

        val captions = PlayerHelper.getCaptionTracks(player)
            .sortedBy { it.roleFlags == PlayerHelper.ROLE_FLAG_AUTO_GEN_SUBTITLE }
            .associateWith {
                val displayName = java.util.Locale.forLanguageTag(it.language.orEmpty())
                    .getDisplayLanguage(java.util.Locale.getDefault())

                if (it.roleFlags == PlayerHelper.ROLE_FLAG_AUTO_GEN_SUBTITLE) {
                    "$displayName (${context.getString(R.string.auto_generated)})"
                } else {
                    displayName
                }
            }

        val currentSubtitle = PlayerHelper.getCurrentPlayedCaptionFormat(player)
        BaseBottomSheet()
            .setSimpleItems(
                listOf(context.getString(R.string.none)) + captions.values.toList(),
                preselectedItem = captions.entries.firstOrNull { (track, _) ->
                    track == currentSubtitle
                }?.value ?: context.getString(R.string.none)
            ) { index ->
                val captionsFormat = captions.keys.toList().getOrNull(index - 1)
                onSubtitleTrackChanged(captionsFormat?.id)
                playerViewModelProvider()?.currentCaptionId = captionsFormat?.id
            }
            .show(fragmentManager)
    }

    override fun onQualityClicked() {
        val resolutions = getAvailableResolutions()

        BaseBottomSheet()
            .setSimpleItems(
                resolutions.map(VideoResolution::name),
                preselectedItem = resolutions.firstOrNull {
                    it.resolution == selectedResolution
                }?.name ?: context.getString(R.string.auto_quality)
            ) { which ->
                val newResolution = resolutions[which].resolution
                setPlayerResolution(newResolution, isSelectedByUser = true)
            }
            .show(fragmentManager)
    }

    override fun onAudioStreamClicked() {
        val player = playerProvider() as? MediaController ?: return

        val audioLanguagesAndRoleFlags = PlayerHelper.getAudioLanguagesAndRoleFlagsFromTrackGroups(
            player.currentTracks.groups, false
        )
        val baseBottomSheet = BaseBottomSheet()

        if (audioLanguagesAndRoleFlags.isEmpty() || (audioLanguagesAndRoleFlags.size == 1 &&
                    audioLanguagesAndRoleFlags[0].first == null &&
                    !PlayerHelper.haveAudioTrackRoleFlagSet(audioLanguagesAndRoleFlags[0].second))
        ) {
            baseBottomSheet.setSimpleItems(
                listOf(context.getString(R.string.default_or_unknown_audio_track)),
                preselectedItem = context.getString(R.string.default_or_unknown_audio_track),
                listener = null
            )
        } else {
            val sortedAudioTracks = audioLanguagesAndRoleFlags.sortedBy { it.second }

            baseBottomSheet.setSimpleItems(
                sortedAudioTracks.map { PlayerHelper.getAudioTrackNameFromFormat(context, it) },
                preselectedItem = getCurrentAudioTrackTitle(),
            ) { index ->
                val selectedAudioFormat = sortedAudioTracks[index]
                player.sendCustomCommand(
                    AbstractPlayerService.runPlayerActionCommand, bundleOf(
                        PlayerCommand.SET_AUDIO_ROLE_FLAGS.name to selectedAudioFormat.second
                    )
                )
                player.sendCustomCommand(
                    AbstractPlayerService.runPlayerActionCommand, bundleOf(
                        PlayerCommand.SET_AUDIO_LANGUAGE.name to selectedAudioFormat.first
                    )
                )
                selectedAudioLanguageAndRoleFlags = selectedAudioFormat
            }
        }

        baseBottomSheet.show(fragmentManager)
    }

    override fun onStatsClicked() {
        val player = playerProvider() ?: return
        val videoStats = PlayerHelper.getVideoStats(player.currentTracks, getVideoId())
        StatsSheet()
            .apply { arguments = bundleOf(IntentData.videoStats to videoStats) }
            .show(fragmentManager)
    }

    private fun getAvailableResolutions(): List<VideoResolution> {
        val player = playerProvider() ?: return emptyList()

        val resolutions = player.currentTracks.groups.asSequence()
            .flatMap { group ->
                (0 until group.length).map { group.getTrackFormat(it).height }
            }
            .filter { it > 0 }
            .map { VideoResolution("${it}p", it) }
            .toSortedSet(compareByDescending { it.resolution })

        resolutions.add(VideoResolution(context.getString(R.string.auto_quality), Int.MAX_VALUE))
        return resolutions.toList()
    }

    fun setPlayerResolution(resolution: Int, isSelectedByUser: Boolean = false) {
        val player = playerProvider() as? MediaController ?: return

        val transformedResolution =
            if (!isSelectedByUser && isVideoShort()) {
                ceil(resolution * 16.0 / 9.0).toInt()
            } else {
                resolution
            }

        player.sendCustomCommand(
            AbstractPlayerService.runPlayerActionCommand, bundleOf(
                PlayerCommand.SET_RESOLUTION.name to transformedResolution
            )
        )

        selectedResolution = resolution
    }

    fun updateResolution(isFullscreen: Boolean) {
        if (!isFullscreen && noFullscreenResolution != null) {
            setPlayerResolution(noFullscreenResolution!!)
        } else if (fullscreenResolution != null) {
            setPlayerResolution(fullscreenResolution!!)
        } else {
            setPlayerResolution(Int.MAX_VALUE)
        }
    }

    fun setToDefaultResolution(context: Context) {
        fullscreenResolution = PlayerHelper.getDefaultResolution(context, true)
        noFullscreenResolution = PlayerHelper.getDefaultResolution(context, false)
    }
}
