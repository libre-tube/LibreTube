package com.github.libretube.ui.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
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
import com.github.libretube.databinding.DoubleTapOverlayBinding
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.databinding.PlayerGestureControlsViewBinding
import com.github.libretube.extensions.dpToPx
import com.github.libretube.extensions.navigateVideo
import com.github.libretube.extensions.normalize
import com.github.libretube.extensions.round
import com.github.libretube.extensions.seekBy
import com.github.libretube.extensions.togglePlayPauseState
import com.github.libretube.extensions.updateIfChanged
import com.github.libretube.helpers.AudioHelper
import com.github.libretube.helpers.BrightnessHelper
import com.github.libretube.helpers.ContextHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.WindowHelper
import com.github.libretube.obj.BottomSheetItem
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.extensions.toggleSystemBars
import com.github.libretube.ui.fragments.PlayerFragment
import com.github.libretube.ui.interfaces.PlayerGestureOptions
import com.github.libretube.ui.interfaces.PlayerOptions
import com.github.libretube.ui.listeners.PlayerGestureController
import com.github.libretube.ui.models.ChaptersViewModel
import com.github.libretube.ui.sheets.BaseBottomSheet
import com.github.libretube.ui.sheets.ChaptersBottomSheet
import com.github.libretube.ui.sheets.PlaybackOptionsSheet
import com.github.libretube.ui.sheets.PlayingQueueSheet
import com.github.libretube.ui.sheets.SleepTimerSheet
import com.github.libretube.util.PlayingQueue

@SuppressLint("ClickableViewAccessibility")
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
abstract class CustomExoPlayerView(
    context: Context,
    attributeSet: AttributeSet? = null
) : PlayerView(context, attributeSet), PlayerOptions, PlayerGestureOptions {
    @Suppress("LeakingThis")
    val binding = ExoStyledPlayerControlViewBinding.bind(this)

    /**
     * Objects for player tap and swipe gesture
     */
    private lateinit var gestureViewBinding: PlayerGestureControlsViewBinding
    private lateinit var playerGestureController: PlayerGestureController
    private lateinit var brightnessHelper: BrightnessHelper
    private lateinit var audioHelper: AudioHelper
    private lateinit var chaptersViewModel: ChaptersViewModel
    private var doubleTapOverlayBinding: DoubleTapOverlayBinding? = null
    private var chaptersBottomSheet: ChaptersBottomSheet? = null
    private var scrubbingTimeBar = false

    /**
     * Objects from the parent fragment
     */

    private val runnableHandler = Handler(Looper.getMainLooper())
    var isPlayerLocked: Boolean = false
    var isLive: Boolean = false
        set(value) {
            field = value
            updateDisplayedDurationType()
            updateCurrentPosition()
        }

    /**
     * Preferences
     */
    private var resizeModePref = PlayerHelper.resizeModePref

    val activity get() = context as BaseActivity

    private val supportFragmentManager
        get() = activity.supportFragmentManager

    private fun toggleController() {
        if (isControllerFullyVisible) hideController() else showController()
    }

    fun initialize(
        doubleTapOverlayBinding: DoubleTapOverlayBinding,
        playerGestureControlsViewBinding: PlayerGestureControlsViewBinding,
        chaptersViewModel: ChaptersViewModel
    ) {
        this.doubleTapOverlayBinding = doubleTapOverlayBinding
        this.gestureViewBinding = playerGestureControlsViewBinding
        this.chaptersViewModel = chaptersViewModel
        this.playerGestureController = PlayerGestureController(context as BaseActivity, this)
        this.brightnessHelper = BrightnessHelper(context as Activity)
        this.audioHelper = AudioHelper(context)

        // Set touch listener for tap and swipe gestures.
        setOnTouchListener(playerGestureController)
        initializeGestureProgress()

        initRewindAndForward()
        applyCaptionsStyle()
        initializeAdvancedOptions()

        // don't let the player view hide its controls automatically
        controllerShowTimeoutMs = -1
        // don't let the player view show its controls automatically
        controllerAutoShow = false

        // locking the player
        binding.lockPlayer.setOnClickListener {
            // change the locked/unlocked icon
            val icon = if (!isPlayerLocked) R.drawable.ic_locked else R.drawable.ic_unlocked
            val tooltip = if (!isPlayerLocked) {
                R.string.tooltip_unlocked
            } else {
                R.string.tooltip_locked
            }

            binding.lockPlayer.setImageResource(icon)
            TooltipCompat.setTooltipText(binding.lockPlayer, context.getString(tooltip))

            // show/hide all the controls
            lockPlayer(isPlayerLocked)

            // change locked status
            isPlayerLocked = !isPlayerLocked

            if (isFullscreen()) toggleSystemBars(!isPlayerLocked)
        }

        resizeMode = when (resizeModePref) {
            "fill" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            "zoom" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        binding.playPauseBTN.setOnClickListener {
            player?.togglePlayPauseState()
        }

        player?.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
                this@CustomExoPlayerView.onPlaybackEvents(player, events)
            }
        })

        player?.let { player ->
            binding.playPauseBTN.setImageResource(
                PlayerHelper.getPlayPauseActionIcon(player)
            )
        }

        player?.let { binding.exoProgress.setPlayer(it) }
        // prevent the controls from disappearing while scrubbing the time bar
        binding.exoProgress.addSeekBarListener(object : TimeBar.OnScrubListener {
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
        })

        binding.autoPlay.isChecked = PlayerHelper.autoPlayEnabled

        binding.autoPlay.setOnCheckedChangeListener { _, isChecked ->
            PlayerHelper.autoPlayEnabled = isChecked
        }

        // restore the duration type from the previous session
        updateDisplayedDurationType()

        binding.duration.setOnClickListener {
            updateDisplayedDurationType(true)
        }
        binding.timeLeft.setOnClickListener {
            updateDisplayedDurationType(false)
        }
        binding.position.setOnClickListener {
            if (isLive) player?.let { it.seekTo(it.duration) }
        }

        // forward touch events to the time bar for better accessibility
        binding.progressBar.setOnTouchListener { _, motionEvent ->
            binding.exoProgress.onTouchEvent(motionEvent)
        }

        updateCurrentPosition()

        activity.supportFragmentManager.setFragmentResultListener(
            ChaptersBottomSheet.SEEK_TO_POSITION_REQUEST_KEY,
            findViewTreeLifecycleOwner() ?: activity
        ) { _, bundle ->
            player?.seekTo(bundle.getLong(IntentData.currentPosition))
        }

        // enable the chapters dialog in the player
        binding.chapterName.setOnClickListener {
            val sheet = chaptersBottomSheet ?: ChaptersBottomSheet()
                .apply {
                    arguments = bundleOf(
                        IntentData.duration to player?.duration?.div(1000)
                    )
                }
                .also {
                    chaptersBottomSheet = it
                }

            if (sheet.isVisible) {
                sheet.dismiss()
            } else {
                sheet.show(activity.supportFragmentManager)
            }
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
    }

    /**
     * Set the name of the video chapter in the [CustomExoPlayerView]
     * @param forceUpdate Update the current chapter name no matter if the seek bar is scrubbed
     * @param enqueueNew set a timeout to automatically repeat this function again in 100ms
     */
    fun setCurrentChapterName(forceUpdate: Boolean = false, enqueueNew: Boolean = true) {
        val player = player ?: return
        val chapters = chaptersViewModel.chapters

        binding.chapterName.isInvisible = chapters.isEmpty()

        // the following logic to set the chapter title can be skipped if no chapters are available
        if (chapters.isEmpty()) return

        // call the function again in 100ms
        if (enqueueNew) postDelayed(this::setCurrentChapterName, 100)

        // if the user is scrubbing the time bar, don't update
        if (scrubbingTimeBar && !forceUpdate) return

        val currentIndex = PlayerHelper.getCurrentChapterIndex(player.currentPosition, chapters)
        val newChapterName = currentIndex?.let { chapters[it].title.trim() }.orEmpty()

        chaptersViewModel.currentChapterIndex.updateIfChanged(currentIndex ?: -1)

        // change the chapter name textView text to the chapterName
        if (newChapterName != binding.chapterName.text) {
            binding.chapterName.text = newChapterName
        }
    }

    fun toggleSystemBars(showBars: Boolean) {
        getWindow().toggleSystemBars(
            types = if (showBars) {
                WindowHelper.getGestureControlledBars(context)
            } else {
                WindowInsetsCompat.Type.systemBars()
            },
            showBars = showBars
        )
    }

    open fun onPlayerEvent(player: Player, playerEvents: Player.Events) = Unit

    private fun updateDisplayedDurationType(showTimeLeft: Boolean? = null) {
        var shouldShowTimeLeft = showTimeLeft ?: PreferenceHelper
            .getBoolean(PreferenceKeys.SHOW_TIME_LEFT, false)
        // always show the time left only if it's a livestream
        if (isLive) shouldShowTimeLeft = true
        if (showTimeLeft != null) {
            // save whether to show time left or duration for next session
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
        // remove the callback to hide the controller
        cancelHideControllerTask()
        super.hideController()
    }

    override fun showController() {
        // remove the previous callback from the queue to prevent a flashing behavior
        cancelHideControllerTask()
        // automatically hide the controller after 2 seconds
        enqueueHideControllerTask()
        super.showController()
    }

    fun showControllerPermanently() {
        // remove the previous callback from the queue to prevent a flashing behavior
        cancelHideControllerTask()
        super.showController()
    }

    override fun onTouchEvent(event: MotionEvent) = false

    private fun initRewindAndForward() {
        val seekIncrementText = (PlayerHelper.seekIncrement / 1000).toString()
        listOf(
            doubleTapOverlayBinding?.rewindTV,
            doubleTapOverlayBinding?.forwardTV,
            binding.forwardTV,
            binding.rewindTV
        ).forEach {
            it?.text = seekIncrementText
        }
        binding.forwardBTN.setOnClickListener {
            player?.seekBy(PlayerHelper.seekIncrement)
        }
        binding.rewindBTN.setOnClickListener {
            player?.seekBy(-PlayerHelper.seekIncrement)
        }
        if (PlayerHelper.doubleTapToSeek) return

        listOf(binding.forwardBTN, binding.rewindBTN).forEach {
            it.isVisible = true
        }
    }

    private fun initializeAdvancedOptions() {
        binding.toggleOptions.setOnClickListener {
            val items = getOptionsMenuItems()
            val bottomSheetFragment = BaseBottomSheet().setItems(items, null)
            bottomSheetFragment.show(supportFragmentManager, null)
        }
    }

    open fun getOptionsMenuItems(): List<BottomSheetItem> = listOf(
        BottomSheetItem(
            context.getString(R.string.repeat_mode),
            R.drawable.ic_repeat,
            {
                when (PlayingQueue.repeatMode) {
                    Player.REPEAT_MODE_OFF -> context.getString(R.string.repeat_mode_none)
                    Player.REPEAT_MODE_ONE -> context.getString(R.string.repeat_mode_current)
                    Player.REPEAT_MODE_ALL -> context.getString(R.string.repeat_mode_all)
                    else -> throw IllegalArgumentException()
                }
            }
        ) {
            onRepeatModeClicked()
        },
        BottomSheetItem(
            context.getString(R.string.player_resize_mode),
            R.drawable.ic_aspect_ratio,
            {
                when (resizeMode) {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> context.getString(
                        R.string.resize_mode_fit
                    )

                    AspectRatioFrameLayout.RESIZE_MODE_FILL -> context.getString(
                        R.string.resize_mode_fill
                    )

                    else -> context.getString(R.string.resize_mode_zoom)
                }
            }
        ) {
            onResizeModeClicked()
        },
        BottomSheetItem(
            context.getString(R.string.playback_speed),
            R.drawable.ic_speed,
            {
                "${player?.playbackParameters?.speed?.round(2)}x"
            }
        ) {
            onPlaybackSpeedClicked()
        },
        BottomSheetItem(
            context.getString(R.string.sleep_timer),
            R.drawable.ic_sleep
        ) {
            onSleepTimerClicked()
        }
    )

    // lock the player
    private fun lockPlayer(isLocked: Boolean) {
        // isLocked is the current (old) state of the player lock
        binding.exoTopBarRight.isVisible = isLocked
        binding.exoCenterControls.isVisible = isLocked
        binding.bottomBar.isVisible = isLocked
        binding.closeImageButton.isVisible = isLocked
        binding.exoTitle.isVisible = isLocked
        binding.playPauseBTN.isVisible = isLocked

        if (!PlayerHelper.doubleTapToSeek) {
            binding.rewindBTN.isVisible = isLocked
            binding.forwardBTN.isVisible = isLocked
        }

        // hide the dimming background overlay if locked
        binding.exoControlsBackground.setBackgroundColor(
            if (isLocked) {
                ContextCompat.getColor(
                    context,
                    androidx.media3.ui.R.color.exo_black_opacity_60
                )
            } else {
                Color.TRANSPARENT
            }
        )

        // disable tap and swipe gesture if the player is locked
        playerGestureController.isEnabled = isLocked
    }

    private fun rewind() {
        player?.seekBy(-PlayerHelper.seekIncrement)

        // show the rewind button
        doubleTapOverlayBinding?.apply {
            animateSeeking(rewindBTN, rewindIV, rewindTV, true)

            // start callback to hide the button
            runnableHandler.removeCallbacksAndMessages(HIDE_REWIND_BUTTON_TOKEN)
            runnableHandler.postDelayed(700, HIDE_REWIND_BUTTON_TOKEN) {
                rewindBTN.isGone = true
            }
        }
    }

    private fun forward() {
        player?.seekBy(PlayerHelper.seekIncrement)

        // show the forward button
        doubleTapOverlayBinding?.apply {
            animateSeeking(forwardBTN, forwardIV, forwardTV, false)

            // start callback to hide the button
            runnableHandler.removeCallbacksAndMessages(HIDE_FORWARD_BUTTON_TOKEN)
            runnableHandler.postDelayed(700, HIDE_FORWARD_BUTTON_TOKEN) {
                forwardBTN.isGone = true
            }
        }
    }

    private fun animateSeeking(
        container: FrameLayout,
        imageView: ImageView,
        textView: TextView,
        isRewind: Boolean
    ) {
        container.isVisible = true
        // the direction of the action
        val direction = if (isRewind) -1 else 1

        // clear previous animation
        imageView.animate()
            .rotation(0F)
            .setDuration(0)
            .start()

        textView.animate()
            .translationX(0f)
            .setDuration(0)
            .start()

        // start the rotate animation of the drawable
        imageView.animate()
            .rotation(direction * 30F)
            .setDuration(ANIMATION_DURATION)
            .withEndAction {
                // reset the animation when finished
                imageView.animate()
                    .rotation(0F)
                    .setDuration(ANIMATION_DURATION)
                    .start()
            }
            .start()

        // animate the text view to move outside the image view
        textView.animate()
            .translationX(direction * 100f)
            .setDuration((ANIMATION_DURATION * 1.5).toLong())
            .withEndAction {
                // move the text back into the button
                runnableHandler.postDelayed(100) {
                    textView.animate()
                        .setDuration(ANIMATION_DURATION / 2)
                        .translationX(0f)
                        .start()
                }
            }
    }

    private fun initializeGestureProgress() {
        gestureViewBinding.brightnessProgressBar.let { bar ->
            bar.progress =
                brightnessHelper.getBrightnessWithScale(bar.max.toFloat(), saved = true).toInt()
        }
        gestureViewBinding.volumeProgressBar.let { bar ->
            bar.progress = audioHelper.getVolumeWithScale(bar.max)
        }
    }

    private fun updateBrightness(distance: Float) {
        gestureViewBinding.brightnessControlView.isVisible = true
        val bar = gestureViewBinding.brightnessProgressBar

        if (bar.progress == 0) {
            // If brightness progress goes to below 0, set to system brightness
            if (distance <= 0) {
                brightnessHelper.resetToSystemBrightness()
                gestureViewBinding.brightnessImageView.setImageResource(
                    R.drawable.ic_brightness_auto
                )
                gestureViewBinding.brightnessTextView.text = resources.getString(R.string.auto)
                return
            }
            gestureViewBinding.brightnessImageView.setImageResource(R.drawable.ic_brightness)
        }

        bar.incrementProgressBy(distance.toInt())
        gestureViewBinding.brightnessTextView.text = "${bar.progress.normalize(0, bar.max, 0, 100)}"
        brightnessHelper.setBrightnessWithScale(bar.progress.toFloat(), bar.max.toFloat())
    }

    private fun updateVolume(distance: Float) {
        val bar = gestureViewBinding.volumeProgressBar
        gestureViewBinding.volumeControlView.apply {
            if (visibility == View.GONE) {
                isVisible = true
                // Volume could be changed using other mediums, sync progress
                // bar with new value.
                bar.progress = audioHelper.getVolumeWithScale(bar.max)
            }
        }

        if (bar.progress == 0) {
            gestureViewBinding.volumeImageView.setImageResource(
                when {
                    distance > 0 -> R.drawable.ic_volume_up
                    else -> R.drawable.ic_volume_off
                }
            )
        }
        bar.incrementProgressBy(distance.toInt())
        audioHelper.setVolumeWithScale(bar.progress, bar.max)

        gestureViewBinding.volumeTextView.text = "${bar.progress.normalize(0, bar.max, 0, 100)}"
    }

    override fun onPlaybackSpeedClicked() {
        (player as? MediaController)?.let {
            PlaybackOptionsSheet(it).show(supportFragmentManager)
        }
    }

    override fun onResizeModeClicked() {
        // switching between original aspect ratio (black bars) and zoomed to fill device screen
        val aspectRatioModeNames = context.resources?.getStringArray(R.array.resizeMode)
            ?.toList().orEmpty()

        val aspectRatioModes = listOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            AspectRatioFrameLayout.RESIZE_MODE_FILL
        )

        BaseBottomSheet()
            .setSimpleItems(
                aspectRatioModeNames,
                preselectedItem = aspectRatioModeNames[aspectRatioModes.indexOf(resizeMode)]
            ) { index ->
                resizeMode = aspectRatioModes[index]
            }
            .show(supportFragmentManager)
    }

    override fun onRepeatModeClicked() {
        // repeat mode options dialog
        BaseBottomSheet()
            .setSimpleItems(
                PlayerHelper.repeatModes.map { context.getString(it.second) },
                preselectedItem = PlayerHelper.repeatModes
                    .firstOrNull { it.first == PlayingQueue.repeatMode }
                    ?.second?.let {
                        context.getString(it)
                    }
            ) { index ->
                PlayingQueue.repeatMode = PlayerHelper.repeatModes[index].first
            }
            .show(supportFragmentManager)
    }

    override fun onSleepTimerClicked() {
        SleepTimerSheet().show(supportFragmentManager)
    }

    open fun isFullscreen() =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        updateMarginsByFullscreenMode()
    }

    /**
     * Updates the margins according to the current orientation and fullscreen mode
     */
    fun updateMarginsByFullscreenMode() {
        // add a larger bottom margin to the time bar in landscape mode
        binding.progressBar.updateLayoutParams<MarginLayoutParams> {
            bottomMargin = (if (isFullscreen()) 20f else 10f).dpToPx()
        }

        updateTopBarMargin()

        // don't add extra padding if there's no cutout and no margin set that would need to be undone
        if (!activity.hasCutout && binding.topBar.marginStart == LANDSCAPE_MARGIN_HORIZONTAL_NONE) return

        // add a margin to the top and the bottom bar in landscape mode for notches
        val isForcedLandscape =
            activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val isInLandscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val horizontalMargin =
            if (isFullscreen() && (isInLandscape || isForcedLandscape)) LANDSCAPE_MARGIN_HORIZONTAL else LANDSCAPE_MARGIN_HORIZONTAL_NONE

        listOf(binding.topBar, binding.bottomBar).forEach {
            it.updateLayoutParams<MarginLayoutParams> {
                marginStart = horizontalMargin
                marginEnd = horizontalMargin
            }
        }
    }

    /**
     * Load the captions style according to the users preferences
     */
    private fun applyCaptionsStyle() {
        val captionStyle = PlayerHelper.getCaptionStyle(context)
        subtitleView?.apply {
            setApplyEmbeddedFontSizes(false)
            setFixedTextSize(Cue.TEXT_SIZE_TYPE_ABSOLUTE, PlayerHelper.captionsTextSize)
            if (PlayerHelper.useRichCaptionRendering) setViewType(SubtitleView.VIEW_TYPE_WEB)
            if (!PlayerHelper.useSystemCaptionStyle) return
            setApplyEmbeddedStyles(captionStyle == CaptionStyleCompat.DEFAULT)
            setStyle(captionStyle)
        }
    }

    /**
     * Add extra margin to the top bar to not overlap the status bar.
     */
    fun updateTopBarMargin() {
        binding.topBar.updateLayoutParams<MarginLayoutParams> {
            topMargin = (if (isFullscreen()) 18f else 0f).dpToPx()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCurrentPosition() {
        val position = player?.currentPosition?.div(1000) ?: 0
        val duration = player?.duration?.takeIf { it != C.TIME_UNSET }?.div(1000) ?: 0
        val timeLeft = duration - position

        binding.position.text =
            if (isLive) context.getString(R.string.live) else DateUtils.formatElapsedTime(position)
        binding.timeLeft.text = "-${DateUtils.formatElapsedTime(timeLeft)}"

        runnableHandler.postDelayed(100, UPDATE_POSITION_TOKEN, this::updateCurrentPosition)
    }

    override fun onSingleTap() {
        toggleController()
    }

    override fun onDoubleTapCenterScreen() {
        player?.togglePlayPauseState()
    }

    override fun onDoubleTapLeftScreen() {
        if (!PlayerHelper.doubleTapToSeek) return
        rewind()
    }

    override fun onDoubleTapRightScreen() {
        if (!PlayerHelper.doubleTapToSeek) return
        forward()
    }

    override fun onSwipeLeftScreen(distanceY: Float) {
        if (!PlayerHelper.swipeGestureEnabled) {
            if (PlayerHelper.fullscreenGesturesEnabled) onSwipeCenterScreen(distanceY)
            return
        }

        if (isControllerFullyVisible) hideController()
        updateBrightness(distanceY)
    }

    override fun onSwipeRightScreen(distanceY: Float) {
        if (!PlayerHelper.swipeGestureEnabled) {
            if (PlayerHelper.fullscreenGesturesEnabled) onSwipeCenterScreen(distanceY)
            return
        }

        if (isControllerFullyVisible) hideController()
        updateVolume(distanceY)
    }

    override fun onSwipeCenterScreen(distanceY: Float) {
        if (!PlayerHelper.fullscreenGesturesEnabled) return

        if (isControllerFullyVisible) hideController()
        if (distanceY >= 0) return

        playerGestureController.isMoving = false
        minimizeOrExitPlayer()
    }

    override fun onSwipeEnd() {
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

    override fun onFullscreenChange(isFullscreen: Boolean) {
        if (isFullscreen) {
            if (PlayerHelper.swipeGestureEnabled && this::brightnessHelper.isInitialized) {
                brightnessHelper.restoreSavedBrightness()
            }
            subtitleView?.setFixedTextSize(
                Cue.TEXT_SIZE_TYPE_ABSOLUTE,
                PlayerHelper.captionsTextSize * 1.5f
            )
            if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                subtitleView?.setBottomPaddingFraction(SUBTITLE_BOTTOM_PADDING_FRACTION)
            }
        } else {
            if (PlayerHelper.swipeGestureEnabled && this::brightnessHelper.isInitialized) {
                brightnessHelper.resetToSystemBrightness(false)
            }
            subtitleView?.setFixedTextSize(
                Cue.TEXT_SIZE_TYPE_ABSOLUTE,
                PlayerHelper.captionsTextSize
            )
            subtitleView?.setBottomPaddingFraction(SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION)
        }
    }

    /**
     * Listen for all child touch events
     */
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // when a control is clicked, restart the countdown to hide the controller
        if (isControllerFullyVisible) {
            cancelHideControllerTask()
            enqueueHideControllerTask()
        }
        return super.onInterceptTouchEvent(ev)
    }

    fun onKeyBoardAction(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                player?.togglePlayPauseState()
            }

            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                forward()
            }

            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                rewind()
            }

            KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_NAVIGATE_NEXT -> {
                PlayingQueue.getNext()?.let { (player as? MediaController)?.navigateVideo(it) }
            }

            KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_NAVIGATE_PREVIOUS -> {
                PlayingQueue.getPrev()?.let { (player as? MediaController)?.navigateVideo(it) }
            }

            KeyEvent.KEYCODE_F -> {
                val fragmentManager =
                    ContextHelper.unwrapActivity<MainActivity>(context).supportFragmentManager
                fragmentManager.fragments.filterIsInstance<PlayerFragment>().firstOrNull()
                    ?.toggleFullscreen()
            }

            else -> return false
        }

        return true
    }

    override fun getViewMeasures(): Pair<Int, Int> {
        return width to height
    }

    open fun onPlaybackEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            binding.playPauseBTN.setImageResource(
                PlayerHelper.getPlayPauseActionIcon(player)
            )

            // keep screen on if the video is playing
            keepScreenOn = player.isPlaying == true
            onPlayerEvent(player, events)
        }
    }

    open fun minimizeOrExitPlayer() = Unit

    open fun getWindow(): Window = activity.window

    companion object {
        private const val HIDE_CONTROLLER_TOKEN = "hideController"
        private const val HIDE_FORWARD_BUTTON_TOKEN = "hideForwardButton"
        private const val HIDE_REWIND_BUTTON_TOKEN = "hideRewindButton"
        private const val UPDATE_POSITION_TOKEN = "updatePosition"

        private const val SUBTITLE_BOTTOM_PADDING_FRACTION = 0.158f
        private const val ANIMATION_DURATION = 100L
        private const val AUTO_HIDE_CONTROLLER_DELAY = 2000L
        private val LANDSCAPE_MARGIN_HORIZONTAL = 20f.dpToPx()
        private val LANDSCAPE_MARGIN_HORIZONTAL_NONE = 0f.dpToPx()
    }
}
