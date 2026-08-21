package com.github.libretube.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import android.widget.FrameLayout
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.media3.ui.TimeBar
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.CustomExoPlayerViewTemplateBinding
import com.github.libretube.databinding.DoubleTapOverlayBinding
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.databinding.PlayerGestureControlsViewBinding
import com.github.libretube.enums.PlayerCommand
import com.github.libretube.extensions.dpToPx
import com.github.libretube.extensions.navigateVideo
import com.github.libretube.extensions.normalize
import com.github.libretube.extensions.seekBy
import com.github.libretube.extensions.togglePlayPauseState
import com.github.libretube.extensions.updateIfChanged
import com.github.libretube.helpers.BrightnessHelper
import com.github.libretube.helpers.CaptionHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.WindowHelper
import com.github.libretube.services.AbstractPlayerService
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.controllers.FullscreenGestureAnimationController
import com.github.libretube.ui.dialogs.SubmitDeArrowDialog
import com.github.libretube.ui.dialogs.SubmitSegmentDialog
import com.github.libretube.ui.extensions.toggleSystemBars
import com.github.libretube.ui.interfaces.CustomPlayerCallback
import com.github.libretube.ui.interfaces.PlayerGestureOptions
import com.github.libretube.ui.interfaces.PlayerOptions
import com.github.libretube.ui.listeners.PlayerGestureController
import com.github.libretube.ui.models.ChaptersViewModel
import com.github.libretube.ui.models.CommonPlayerViewModel
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.ui.sheets.BaseBottomSheet
import com.github.libretube.ui.sheets.ChaptersBottomSheet
import com.github.libretube.ui.sheets.PlayingQueueSheet
import com.github.libretube.util.PlayingQueue

@SuppressLint("ClickableViewAccessibility")
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class CustomExoPlayerView(
    context: Context,
    attributeSet: AttributeSet? = null
) : PlayerView(context, attributeSet), PlayerOptions, PlayerGestureOptions {
    @Suppress("LeakingThis")
    val binding = ExoStyledPlayerControlViewBinding.bind(this)
    val backgroundBinding = CustomExoPlayerViewTemplateBinding.bind(this)

    private val gestureViewBinding: PlayerGestureControlsViewBinding get() = backgroundBinding.playerGestureControlsView.binding
    private val doubleTapOverlayBinding: DoubleTapOverlayBinding get() = backgroundBinding.doubleTapOverlay.binding

    private var playerGestureController: PlayerGestureController
    private var brightnessHelper: BrightnessHelper
    private lateinit var chaptersViewModel: ChaptersViewModel
    private lateinit var seekBarListener: TimeBar.OnScrubListener
    private var fullscreenGestureAnimationController: FullscreenGestureAnimationController
    private var chaptersBottomSheet: ChaptersBottomSheet? = null
    private var scrubbingTimeBar = false

    private val runnableHandler = Handler(Looper.getMainLooper())
    private var isPlayerLocked: Boolean = false

    private val activity get() = context as BaseActivity
    private val supportFragmentManager get() = activity.supportFragmentManager

    private var playerViewModel: PlayerViewModel? = null
    private var commonPlayerViewModel: CommonPlayerViewModel? = null
    private var viewLifecycleOwner: LifecycleOwner? = null

    private val handler = Handler(Looper.getMainLooper())

    var currentWindow: Window? = null
    var sponsorBlockAutoSkip = true
        private set

    private lateinit var playerCallback: CustomPlayerCallback

    private lateinit var menuHandler: PlayerMenuHandler
    private lateinit var seekHelper: PlayerSeekHelper

    init {
        val audioHelper = com.github.libretube.helpers.AudioHelper(context)
        brightnessHelper = BrightnessHelper(activity)
        playerGestureController = PlayerGestureController(activity, this)
        fullscreenGestureAnimationController = FullscreenGestureAnimationController(
            playerView = this,
            videoFrameView = backgroundBinding.exoContentFrame,
            onSwipeUpCompleted = { if (!isFullscreen()) playerCallback.toggleFullscreen() },
            onSwipeDownCompleted = { if (isFullscreen()) playerCallback.toggleFullscreen() }
        )

        seekHelper = PlayerSeekHelper(
            playerProvider = { player },
            gestureBinding = { doubleTapOverlayBinding },
            controlBinding = { binding },
            fastForwardViewProvider = { backgroundBinding.fastForwardView },
            isPlayerLocked = { isPlayerLocked },
            handler = runnableHandler,
            hideForwardToken = HIDE_FORWARD_BUTTON_TOKEN,
            hideRewindToken = HIDE_REWIND_BUTTON_TOKEN
        )
    }

    fun initialize(
        chaptersViewModel: ChaptersViewModel,
        commonPlayerViewModel: CommonPlayerViewModel,
        playerViewModel: PlayerViewModel,
        viewLifecycleOwner: LifecycleOwner,
        playerCallback: CustomPlayerCallback,
        player: Player,
    ) {
        this.chaptersViewModel = chaptersViewModel
        this.playerViewModel = playerViewModel
        this.commonPlayerViewModel = commonPlayerViewModel
        this.viewLifecycleOwner = viewLifecycleOwner
        this.playerCallback = playerCallback
        super.player = player

        menuHandler = PlayerMenuHandler(
            context = context,
            fragmentManager = supportFragmentManager,
            playerProvider = { player },
            playerViewModelProvider = { playerViewModel },
            isVideoLive = { playerCallback.isVideoLive() },
            getVideoId = { playerCallback.getVideoId() },
            isVideoShort = { playerCallback.isVideoShort() },
            onResizeModeChanged = { resizeMode = it },
            onSubtitleTrackChanged = ::updateCurrentSubtitle
        )

        initializeGestureProgress()

        seekHelper.initRewindAndForward()
        applyCaptionsStyle()
        initializeAdvancedOptions()

        setupKeyboardFocus()

        controllerShowTimeoutMs = -1
        controllerAutoShow = false

        binding.fullscreen.setOnClickListener { playerCallback.toggleFullscreen() }

        resizeMode = menuHandler.resizeMode

        if (!::seekBarListener.isInitialized) {
            seekBarListener = object : TimeBar.OnScrubListener {
                override fun onScrubStart(timeBar: TimeBar, position: Long) {
                    cancelHideControllerTask()
                }

                override fun onScrubMove(timeBar: TimeBar, position: Long) {
                    cancelHideControllerTask()
                    setCurrentChapterName(forceUpdate = true, enqueueNew = false)
                    scrubbingTimeBar = true
                }

                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                    enqueueHideControllerTask()
                    setCurrentChapterName(forceUpdate = true, enqueueNew = false)
                    scrubbingTimeBar = false
                }
            }
            binding.exoProgress.addSeekBarListener(seekBarListener)
        }

        binding.autoPlay.isChecked = PlayerHelper.autoPlayEnabled
        binding.autoPlay.setOnCheckedChangeListener { _, isChecked ->
            PlayerHelper.autoPlayEnabled = isChecked
        }

        updateDisplayedDurationType()

        binding.duration.setOnClickListener { updateDisplayedDurationType(true) }
        binding.timeLeft.setOnClickListener { updateDisplayedDurationType(false) }
        binding.position.setOnClickListener {
            if (playerCallback.isVideoLive()) player.let { it.seekTo(it.duration) }
        }

        activity.supportFragmentManager.setFragmentResultListener(
            ChaptersBottomSheet.SEEK_TO_POSITION_REQUEST_KEY,
            findViewTreeLifecycleOwner() ?: activity
        ) { _, bundle ->
            player.seekTo(bundle.getLong(IntentData.currentPosition))
        }

        binding.chapterName.setOnClickListener {
            val sheet = chaptersBottomSheet ?: ChaptersBottomSheet()
                .apply {
                    arguments = bundleOf(IntentData.duration to player.duration.div(1000))
                }
                .also { chaptersBottomSheet = it }

            if (sheet.isVisible) sheet.dismiss()
            else sheet.show(activity.supportFragmentManager)
        }

        supportFragmentManager.setFragmentResultListener(
            PlayingQueueSheet.PLAYING_QUEUE_REQUEST_KEY,
            findViewTreeLifecycleOwner() ?: activity
        ) { _, args ->
            (player as? MediaController)?.navigateVideo(
                args.getString(IntentData.videoId) ?: return@setFragmentResultListener
            )
        }
        binding.queueToggle.setOnClickListener {
            PlayingQueueSheet().show(supportFragmentManager, null)
        }

        updateMarginsByFullscreenMode()

        commonPlayerViewModel.isFullscreen.observe(viewLifecycleOwner) { isFullscreen ->
            updateTopBarMargin()

            binding.fullscreen.isInvisible = PlayerHelper.autoFullscreenEnabled
            val fullscreenDrawable =
                if (isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
            binding.fullscreen.setImageResource(fullscreenDrawable)

            binding.exoTitle.isInvisible = !isFullscreen

            menuHandler.updateResolution(isFullscreen)
        }

        val updateSbImageResource = {
            binding.sbToggle.setImageResource(
                if (sponsorBlockAutoSkip) R.drawable.ic_sb_enabled else R.drawable.ic_sb_disabled
            )
        }
        updateSbImageResource()
        binding.sbToggle.setOnClickListener {
            sponsorBlockAutoSkip = !sponsorBlockAutoSkip
            (player as? MediaController)?.sendCustomCommand(
                AbstractPlayerService.runPlayerActionCommand, bundleOf(
                    PlayerCommand.SET_SB_AUTO_SKIP_ENABLED.name to sponsorBlockAutoSkip
                )
            )
            updateSbImageResource()
        }

        syncQueueButtons()

        binding.sbSubmit.isVisible =
            PreferenceHelper.getBoolean(PreferenceKeys.CONTRIBUTE_TO_SB, false)
        binding.sbSubmit.setOnClickListener {
            val submitSegmentDialog = SubmitSegmentDialog()
            submitSegmentDialog.arguments = menuHandler.buildSbBundleArgs() ?: return@setOnClickListener
            submitSegmentDialog.show((context as BaseActivity).supportFragmentManager, null)
        }

        binding.dearrowSubmit.isVisible =
            PreferenceHelper.getBoolean(PreferenceKeys.CONTRIBUTE_TO_DEARROW, false)
        binding.dearrowSubmit.setOnClickListener {
            val submitDialog = SubmitDeArrowDialog()
            submitDialog.arguments = menuHandler.buildSbBundleArgs() ?: return@setOnClickListener
            submitDialog.show((context as BaseActivity).supportFragmentManager, null)
        }

        binding.playPauseBTN.setOnClickListener { player.togglePlayPauseState() }

        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
                this@CustomExoPlayerView.onPlaybackEvents(player, events)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                keepScreenOn = isPlaying
            }
        })

        binding.playPauseBTN.setImageResource(PlayerHelper.getPlayPauseActionIcon(player))
        binding.exoProgress.setPlayer(player)

        if (player.isPlaying) keepScreenOn = true

        binding.lockPlayer.setOnClickListener {
            val icon = if (!isPlayerLocked) R.drawable.ic_locked else R.drawable.ic_unlocked
            val tooltip = if (!isPlayerLocked) R.string.tooltip_unlocked else R.string.tooltip_locked

            binding.lockPlayer.setImageResource(icon)
            TooltipCompat.setTooltipText(binding.lockPlayer, context.getString(tooltip))

            lockPlayer(isPlayerLocked)
            isPlayerLocked = !isPlayerLocked

            if (isFullscreen()) toggleSystemBars(!isPlayerLocked)
        }

        updateCurrentPosition()
    }

    @Deprecated("Use `initialize()` instead to attach `Player` and use `detachPlayer()` to detach it")
    override fun setPlayer(player: Player?) {
        super.setPlayer(player)
    }

    fun detachPlayer() {
        super.setPlayer(null)
    }

    private fun syncQueueButtons() {
        if (!PlayerHelper.skipButtonsEnabled) return

        binding.skipPrev.isInvisible = !PlayingQueue.hasPrev() || isPlayerLocked
        binding.skipNext.isInvisible = !PlayingQueue.hasNext() || isPlayerLocked

        handler.postDelayed(this::syncQueueButtons, 100)
    }

    private fun updateDisplayedDuration() {
        if (playerCallback.isVideoLive()) return

        val duration = player?.duration?.div(1000) ?: return
        if (duration < 0) return

        val durationWithoutSegments = duration - playerViewModel?.segments?.value.orEmpty().sumOf {
            val (start, end) = it.segmentStartAndEnd
            end.toDouble() - start.toDouble()
        }.toLong()
        val durationString = DateUtils.formatElapsedTime(duration)

        binding.duration.text = if (durationWithoutSegments < duration) {
            "$durationString (${DateUtils.formatElapsedTime(durationWithoutSegments)})"
        } else {
            durationString
        }
    }

    fun setCurrentChapterName(forceUpdate: Boolean = false, enqueueNew: Boolean = true) {
        val player = player ?: return
        val chapters = chaptersViewModel.chapters

        binding.chapterName.isInvisible = chapters.isEmpty()

        if (chapters.isEmpty()) return

        if (enqueueNew) postDelayed(this::setCurrentChapterName, 100)

        if (scrubbingTimeBar && !forceUpdate) return

        val currentIndex = PlayerHelper.getCurrentChapterIndex(player.currentPosition, chapters)
        val newChapterName = currentIndex?.let { chapters[it].title.trim() }.orEmpty()

        chaptersViewModel.currentChapterIndex.updateIfChanged(currentIndex ?: -1)

        if (newChapterName != binding.chapterName.text) {
            binding.chapterName.text = newChapterName
        }
    }

    private fun setupKeyboardFocus() {
        isFocusable = true
        isFocusableInTouchMode = true
        activity.window.decorView.requestFocus()
    }

    fun toggleSystemBars(showBars: Boolean) {
        getWindow().toggleSystemBars(
            types = if (showBars) WindowHelper.getGestureControlledBars(context)
            else WindowInsetsCompat.Type.systemBars(),
            showBars = showBars
        )
    }

    private fun updateDisplayedDurationType(showTimeLeft: Boolean? = null) {
        var shouldShowTimeLeft = showTimeLeft
            ?: PreferenceHelper.getBoolean(PreferenceKeys.SHOW_TIME_LEFT, false)
        if (playerCallback.isVideoLive()) shouldShowTimeLeft = true
        if (showTimeLeft != null) {
            PreferenceHelper.putBoolean(PreferenceKeys.SHOW_TIME_LEFT, shouldShowTimeLeft)
        }
        binding.timeLeft.isVisible = shouldShowTimeLeft
        binding.duration.isGone = shouldShowTimeLeft
    }

    private fun enqueueHideControllerTask() {
        runnableHandler.postDelayed(AUTO_HIDE_CONTROLLER_DELAY, HIDE_CONTROLLER_TOKEN) {
            hideController()
        }
    }

    private fun cancelHideControllerTask() {
        runnableHandler.removeCallbacksAndMessages(HIDE_CONTROLLER_TOKEN)
    }

    override fun hideController() {
        cancelHideControllerTask()
        super.hideController()
        backgroundBinding.exoControlsBackground.animate().alpha(0f).setDuration(500).start()

        if (isFullscreen()) toggleSystemBars(false)
    }

    override fun showController() {
        cancelHideControllerTask()
        enqueueHideControllerTask()
        super.showController()
        backgroundBinding.exoControlsBackground.animate().alpha(1f).setDuration(200).start()

        if (isFullscreen() && !isPlayerLocked) toggleSystemBars(true)
    }

    fun showControllerPermanently() {
        cancelHideControllerTask()
        super.showController()
    }

    private fun initializeAdvancedOptions() {
        binding.toggleOptions.setOnClickListener {
            val items = menuHandler.buildOptionsMenuItems()
            val bottomSheetFragment = BaseBottomSheet().setItems(items, null)
            bottomSheetFragment.show(supportFragmentManager, null)
        }
    }

    fun getOptionsMenuItems() = menuHandler.buildOptionsMenuItems()

    fun setToDefaultResolution() {
        menuHandler.setToDefaultResolution(context)
        menuHandler.updateResolution(isFullscreen())
    }

    fun setPlayerResolution(resolution: Int, isSelectedByUser: Boolean = false) {
        menuHandler.setPlayerResolution(resolution, isSelectedByUser)
    }

    fun updateCurrentSubtitle(trackId: String?) {
        val player = player as? MediaController ?: return

        player.sendCustomCommand(
            AbstractPlayerService.runPlayerActionCommand, bundleOf(
                PlayerCommand.SET_CAPTION_TRACK.name to trackId
            )
        )
    }

    private fun lockPlayer(isLocked: Boolean) {
        binding.exoTopBarRight.isVisible = isLocked
        binding.exoCenterControls.isVisible = isLocked
        binding.bottomBar.isVisible = isLocked
        binding.closeImageButton.isVisible = isLocked
        binding.exoTitle.isVisible = isLocked
        binding.playPauseBTN.isVisible = isLocked

        if (!PlayerHelper.doubleTapToSeek) {
            binding.seekButtonRewind.rewindBTN.isVisible = isLocked
            binding.seekButtonForward.forwardBTN.isVisible = isLocked
        }

        backgroundBinding.exoControlsBackground.setBackgroundColor(
            if (isLocked) ContextCompat.getColor(context, androidx.media3.ui.R.color.exo_black_opacity_60)
            else Color.TRANSPARENT
        )

        playerGestureController.areControlsLocked = !isLocked
    }

    private fun initializeGestureProgress() {
        gestureViewBinding.brightnessProgressBar.let { bar ->
            bar.progress = (brightnessHelper.savedWindowBrightness * bar.max).toInt().coerceIn(0, bar.max)
        }
        gestureViewBinding.volumeProgressBar.let { bar ->
            val audioHelper = com.github.libretube.helpers.AudioHelper(context)
            bar.progress = (audioHelper.deviceVolume * bar.max).toInt().coerceIn(0, bar.max)
        }
    }

    private fun updateBrightness(distance: Float) {
        gestureViewBinding.brightnessControlView.isVisible = true
        val bar = gestureViewBinding.brightnessProgressBar

        if (bar.progress == 0) {
            if (distance <= 0) {
                brightnessHelper.resetToSystemBrightness()
                gestureViewBinding.brightnessImageView.setImageResource(R.drawable.ic_brightness_auto)
                gestureViewBinding.brightnessTextView.text = resources.getString(R.string.auto)
                return
            }
            gestureViewBinding.brightnessImageView.setImageResource(R.drawable.ic_brightness)
        }

        bar.incrementProgressBy(distance.toInt())
        gestureViewBinding.brightnessTextView.text = "${bar.progress.normalize(0, bar.max, 0, 100)}"
        brightnessHelper.windowBrightness = bar.progress.toFloat() / bar.max
    }

    private fun updateVolume(distance: Float) {
        val audioHelper = com.github.libretube.helpers.AudioHelper(context)
        val bar = gestureViewBinding.volumeProgressBar
        gestureViewBinding.volumeControlView.apply {
            if (isGone) {
                isVisible = true
                bar.progress = (audioHelper.deviceVolume * bar.max).toInt().coerceIn(0, bar.max)
            }
        }

        if (bar.progress == 0) {
            gestureViewBinding.volumeImageView.setImageResource(
                if (distance > 0) R.drawable.ic_volume_up else R.drawable.ic_volume_off
            )
        }
        bar.incrementProgressBy(distance.toInt())
        audioHelper.deviceVolume = bar.progress.toFloat() / bar.max

        gestureViewBinding.volumeTextView.text = "${bar.progress.normalize(0, bar.max, 0, 100)}"
    }

    fun isFullscreen() = commonPlayerViewModel?.isFullscreen?.value ?: false

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        updateMarginsByFullscreenMode()
    }

    fun updateMarginsByFullscreenMode() {
        binding.exoProgress.updateLayoutParams<MarginLayoutParams> {
            bottomMargin = (if (isFullscreen()) 20f else 0f).dpToPx()
        }

        updateTopBarMargin()

        if (!activity.hasCutout && binding.topBar.marginStart == LANDSCAPE_MARGIN_HORIZONTAL_NONE) return

        val isForcedLandscape =
            activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val isInLandscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val horizontalMargin =
            if (isFullscreen() && (isInLandscape || isForcedLandscape)) LANDSCAPE_MARGIN_HORIZONTAL
            else LANDSCAPE_MARGIN_HORIZONTAL_NONE

        listOf(binding.topBar, binding.bottomBar).forEach {
            it.updateLayoutParams<MarginLayoutParams> {
                marginStart = horizontalMargin
                marginEnd = horizontalMargin
            }
        }

        binding.fullscreen.layoutParams =
            (binding.fullscreen.layoutParams as MarginLayoutParams).apply {
                if (isFullscreen()) {
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.fullscreen_button_margin_bottom)
                    marginEnd = resources.getDimensionPixelSize(R.dimen.fullscreen_button_margin_end)
                } else {
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.normal_button_margin_bottom)
                    marginEnd = resources.getDimensionPixelSize(R.dimen.normal_button_margin_end)
                }
            }
    }

    private fun applyCaptionsStyle() {
        val captionStyle = CaptionHelper.getCaptionStyle(context)
        subtitleView?.apply {
            setApplyEmbeddedFontSizes(false)
            setFixedTextSize(Cue.TEXT_SIZE_TYPE_ABSOLUTE, PlayerHelper.captionsTextSize)
            if (PlayerHelper.useRichCaptionRendering) setViewType(SubtitleView.VIEW_TYPE_WEB)
            if (!PlayerHelper.useSystemCaptionStyle) return
            setApplyEmbeddedStyles(captionStyle == CaptionStyleCompat.DEFAULT)
            setStyle(captionStyle)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCurrentPosition() {
        val position = player?.currentPosition?.div(1000) ?: 0
        val duration = player?.duration?.takeIf { it != C.TIME_UNSET }?.div(1000) ?: 0
        val timeLeft = duration - position

        binding.position.text =
            if (playerCallback.isVideoLive()) context.getString(R.string.live)
            else DateUtils.formatElapsedTime(position)
        binding.timeLeft.text = "-${DateUtils.formatElapsedTime(timeLeft)}"

        runnableHandler.postDelayed(100, UPDATE_POSITION_TOKEN, this::updateCurrentPosition)
    }

    fun updateTopBarMargin() {
        binding.topBar.updateLayoutParams<MarginLayoutParams> {
            topMargin = (if (isFullscreen()) 18f else 0f).dpToPx()
        }
    }

    // PlayerOptions delegation
    override fun onPlaybackSpeedClicked() = menuHandler.onPlaybackSpeedClicked()
    override fun onResizeModeClicked() = menuHandler.onResizeModeClicked()
    override fun onRepeatModeClicked() = menuHandler.onRepeatModeClicked()
    override fun onSleepTimerClicked() = menuHandler.onSleepTimerClicked()
    override fun onCaptionsClicked() = menuHandler.onCaptionsClicked()
    override fun onQualityClicked() = menuHandler.onQualityClicked()
    override fun onAudioStreamClicked() = menuHandler.onAudioStreamClicked()
    override fun onStatsClicked() = menuHandler.onStatsClicked()

    override fun setResizeMode(resizeMode: Int) {
        super.setResizeMode(resizeMode)
        menuHandler.saveResizeMode(resizeMode)
    }

    // PlayerGestureOptions delegation
    override fun onSingleTap(areControlsLocked: Boolean) {
        if (areControlsLocked) {
            toggleController(true)
            return
        }
        toggleController()
    }

    override fun onDoubleTapCenterScreen() = player?.togglePlayPauseState() ?: Unit
    override fun onDoubleTapLeftScreen() { if (PlayerHelper.doubleTapToSeek) seekHelper.rewind() }
    override fun onDoubleTapRightScreen() { if (PlayerHelper.doubleTapToSeek) seekHelper.forward() }

    override fun onSwipeLeftScreen(distanceY: Float, positionY: Float) {
        if (!PlayerHelper.swipeGestureEnabled) {
            if (PlayerHelper.fullscreenGesturesEnabled) onSwipeCenterScreen(distanceY, positionY)
            return
        }
        if (isControllerFullyVisible) hideController()
        updateBrightness(distanceY)
    }

    override fun onSwipeRightScreen(distanceY: Float, positionY: Float) {
        if (!PlayerHelper.swipeGestureEnabled) {
            if (PlayerHelper.fullscreenGesturesEnabled) onSwipeCenterScreen(distanceY, positionY)
            return
        }
        if (isControllerFullyVisible) hideController()
        updateVolume(distanceY)
    }

    override fun onSwipeCenterScreen(distanceY: Float, positionY: Float) {
        if (!PlayerHelper.fullscreenGesturesEnabled) return
        fullscreenGestureAnimationController.onSwipe(distanceY, positionY)
    }

    override fun onSwipeEnd() {
        fullscreenGestureAnimationController.onSwipeEnd()
        gestureViewBinding.brightnessControlView.isGone = true
        gestureViewBinding.volumeControlView.isGone = true
    }

    override fun onZoom() {
        if (!PlayerHelper.pinchGestureEnabled) return
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            subtitleView?.setBottomPaddingFraction(SUBTITLE_BOTTOM_PADDING_FRACTION)
        }
    }

    override fun onMinimize() {
        if (!PlayerHelper.pinchGestureEnabled) return
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        subtitleView?.setBottomPaddingFraction(SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION)
    }

    override fun onLongPress() = seekHelper.onLongPress()
    override fun onLongPressEnd() = seekHelper.onLongPressEnd()

    override fun onFullscreenChange(isFullscreen: Boolean) {
        if (isFullscreen) {
            if (PlayerHelper.swipeGestureEnabled) brightnessHelper.restoreSavedBrightness()
            subtitleView?.setFixedTextSize(Cue.TEXT_SIZE_TYPE_ABSOLUTE, PlayerHelper.captionsTextSize * 1.5f)
            if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                subtitleView?.setBottomPaddingFraction(SUBTITLE_BOTTOM_PADDING_FRACTION)
            }
        } else {
            if (PlayerHelper.swipeGestureEnabled) brightnessHelper.resetToSystemBrightness()
            subtitleView?.setFixedTextSize(Cue.TEXT_SIZE_TYPE_ABSOLUTE, PlayerHelper.captionsTextSize)
            subtitleView?.setBottomPaddingFraction(SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION)
        }

        updateMarginsByFullscreenMode()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (isControllerFullyVisible) {
            cancelHideControllerTask()
            enqueueHideControllerTask()
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false
        if (!useController) return false
        return playerGestureController.onTouchEvent(event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_F) {
            playerCallback.toggleFullscreen()
            return true
        }
        return seekHelper.onKeyUp(keyCode, event)
    }

    override fun getViewMeasures(): Pair<Int, Int> = width to height

    var alreadySetDefaultSubtitle: Boolean = false
    fun onPlaybackEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            binding.playPauseBTN.setImageResource(PlayerHelper.getPlayPauseActionIcon(player))
            keepScreenOn = player.isPlaying == true
        }

        if (events.contains(Player.EVENT_RENDERED_FIRST_FRAME)) {
            if (!PlayerHelper.playAutomatically) showControllerPermanently()
        }

        if (events.contains(Player.EVENT_RENDERED_FIRST_FRAME) && !alreadySetDefaultSubtitle) {
            alreadySetDefaultSubtitle = true

            val captions = PlayerHelper.getCaptionTracks(player)
            val defaultLangCaption =
                captions.firstOrNull { it.language == PlayerHelper.defaultSubtitleCode }

            updateCurrentSubtitle(defaultLangCaption?.id)
            updateDisplayedDurationType()
        }
        if (events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)) {
            alreadySetDefaultSubtitle = false
        }

        updateDisplayedDuration()
    }

    fun getWindow(): Window = currentWindow ?: activity.window

    private fun toggleController(show: Boolean = !isControllerFullyVisible) {
        if (show) showController() else hideController()
    }

    companion object {
        private const val HIDE_CONTROLLER_TOKEN = "hideController"
        private const val HIDE_FORWARD_BUTTON_TOKEN = "hideForwardButton"
        private const val HIDE_REWIND_BUTTON_TOKEN = "hideRewindButton"
        private const val UPDATE_POSITION_TOKEN = "updatePosition"

        private const val SUBTITLE_BOTTOM_PADDING_FRACTION = 0.158f
        const val ANIMATION_DURATION = 100L
        private const val AUTO_HIDE_CONTROLLER_DELAY = 2000L
        private val LANDSCAPE_MARGIN_HORIZONTAL = 20f.dpToPx()
        private val LANDSCAPE_MARGIN_HORIZONTAL_NONE = 0f.dpToPx()
    }
}
