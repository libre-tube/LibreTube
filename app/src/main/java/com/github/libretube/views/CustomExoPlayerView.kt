package com.github.libretube.views

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.FragmentManager
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.constants.PreferenceRanges
import com.github.libretube.databinding.DialogSliderBinding
import com.github.libretube.databinding.DoubleTapOverlayBinding
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.extensions.setSliderRangeAndValue
import com.github.libretube.interfaces.DoubleTapInterface
import com.github.libretube.interfaces.PlayerOptionsInterface
import com.github.libretube.obj.BottomSheetItem
import com.github.libretube.util.DoubleTapListener
import com.github.libretube.util.PreferenceHelper
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.RepeatModeUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@SuppressLint("ClickableViewAccessibility")
internal class CustomExoPlayerView(
    context: Context,
    attributeSet: AttributeSet? = null
) : StyledPlayerView(context, attributeSet) {
    val binding: ExoStyledPlayerControlViewBinding = ExoStyledPlayerControlViewBinding.bind(this)
    private var doubleTapOverlayBinding: DoubleTapOverlayBinding? = null

    /**
     * Objects from the parent fragment
     */
    private var doubleTapListener: DoubleTapInterface? = null
    private var playerOptionsInterface: PlayerOptionsInterface? = null
    private lateinit var childFragmentManager: FragmentManager
    private var trackSelector: TrackSelector? = null

    private val runnableHandler = Handler(Looper.getMainLooper())

    // the x-position of where the user clicked
    private var xPos = 0F

    var isPlayerLocked: Boolean = false

    /**
     * Preferences
     */
    var autoplayEnabled = PreferenceHelper.getBoolean(
        PreferenceKeys.AUTO_PLAY,
        false
    )

    private val seekIncrement = PreferenceHelper.getString(
        PreferenceKeys.SEEK_INCREMENT,
        "5"
    ).toLong() * 1000

    private var resizeModePref = PreferenceHelper.getString(
        PreferenceKeys.PLAYER_RESIZE_MODE,
        "fit"
    )

    private fun toggleController() {
        if (isControllerFullyVisible) hideController() else showController()
    }

    private val doubleTouchListener = object : DoubleTapListener() {
        override fun onDoubleClick() {
            doubleTapListener?.onEvent(xPos)
        }

        override fun onSingleClick() {
            toggleController()
        }
    }

    fun initialize(
        childFragmentManager: FragmentManager,
        playerViewInterface: PlayerOptionsInterface?,
        doubleTapOverlayBinding: DoubleTapOverlayBinding,
        trackSelector: TrackSelector?
    ) {
        this.childFragmentManager = childFragmentManager
        this.playerOptionsInterface = playerViewInterface
        this.doubleTapOverlayBinding = doubleTapOverlayBinding
        this.trackSelector = trackSelector

        // set the double click listener for rewind/forward
        setOnClickListener(doubleTouchListener)

        enableDoubleTapToSeek()

        initializeAdvancedOptions(context)

        // locking the player
        binding.lockPlayer.setOnClickListener {
            // change the locked/unlocked icon
            binding.lockPlayer.setImageResource(
                if (!isPlayerLocked) {
                    R.drawable.ic_locked
                } else {
                    R.drawable.ic_unlocked
                }
            )

            // show/hide all the controls
            lockPlayer(isPlayerLocked)

            // change locked status
            isPlayerLocked = !isPlayerLocked
        }

        resizeMode = when (resizeModePref) {
            "fill" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            "zoom" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    override fun hideController() {
        (context as? MainActivity)?.hideSystemBars()
        super.hideController()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // save the x position of the touch event
        xPos = event.x
        // listen for a double touch
        doubleTouchListener.onClick(this)
        return false
    }

    private fun initializeAdvancedOptions(context: Context) {
        binding.toggleOptions.setOnClickListener {
            val bottomSheetFragment = BottomSheet().apply {
                val items = mutableListOf(
                    BottomSheetItem(
                        context.getString(R.string.player_autoplay),
                        R.drawable.ic_play,
                        if (autoplayEnabled) {
                            context.getString(R.string.enabled)
                        } else {
                            context.getString(R.string.disabled)
                        }
                    ),
                    BottomSheetItem(
                        context.getString(R.string.repeat_mode),
                        R.drawable.ic_repeat,
                        if (player?.repeatMode == RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE) {
                            context.getString(R.string.repeat_mode_none)
                        } else {
                            context.getString(R.string.repeat_mode_current)
                        }
                    ),
                    BottomSheetItem(
                        context.getString(R.string.player_resize_mode),
                        R.drawable.ic_aspect_ratio,
                        when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> context.getString(R.string.resize_mode_fit)
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> context.getString(R.string.resize_mode_fill)
                            else -> context.getString(R.string.resize_mode_zoom)
                        }
                    ),
                    BottomSheetItem(
                        context.getString(R.string.playback_speed),
                        R.drawable.ic_speed,
                        "${player?.playbackParameters?.speed
                            .toString()
                            .replace(".0", "")
                        }x"
                    )
                )

                if (playerOptionsInterface != null) {
                    items.add(
                        BottomSheetItem(
                            context.getString(R.string.quality),
                            R.drawable.ic_hd,
                            "${player?.videoSize?.height}p"
                        )
                    )
                    items.add(
                        BottomSheetItem(
                            context.getString(R.string.captions),
                            R.drawable.ic_caption,
                            if (trackSelector != null && trackSelector!!.parameters.preferredTextLanguages.isNotEmpty()) {
                                trackSelector!!.parameters.preferredTextLanguages[0]
                            } else {
                                context.getString(R.string.none)
                            }
                        )
                    )
                }

                setItems(items) { index ->
                    when (index) {
                        0 -> onAutoplayClicked()
                        1 -> onRepeatModeClicked()
                        2 -> onResizeModeClicked()
                        3 -> onPlaybackSpeedClicked()
                        4 -> playerOptionsInterface?.onQualityClicked()
                        5 -> playerOptionsInterface?.onCaptionClicked()
                    }
                }
            }
            bottomSheetFragment.show(childFragmentManager, null)
        }
    }

    // lock the player
    private fun lockPlayer(isLocked: Boolean) {
        // isLocked is the current (old) state of the player lock
        val visibility = if (isLocked) View.VISIBLE else View.GONE

        binding.exoTopBarRight.visibility = visibility
        binding.exoCenterControls.visibility = visibility
        binding.exoBottomBar.visibility = visibility
        binding.closeImageButton.visibility = visibility

        // disable double tap to seek when the player is locked
        if (isLocked) {
            // enable fast forward and rewind by double tapping
            enableDoubleTapToSeek()
        } else {
            // disable fast forward and rewind by double tapping
            doubleTapListener = null
        }
    }

    private fun enableDoubleTapToSeek() {
        // set seek increment text
        val seekIncrementText = (seekIncrement / 1000).toString()
        doubleTapOverlayBinding?.rewindTV?.text = seekIncrementText
        doubleTapOverlayBinding?.forwardTV?.text = seekIncrementText
        doubleTapListener =
            object : DoubleTapInterface {
                override fun onEvent(x: Float) {
                    when {
                        width * 0.5 > x -> rewind()
                        width * 0.5 < x -> forward()
                    }
                }
            }
    }

    private fun rewind() {
        player?.seekTo((player?.currentPosition ?: 0L) - seekIncrement)

        // show the rewind button
        doubleTapOverlayBinding?.rewindBTN.apply {
            this!!.visibility = View.VISIBLE
            // clear previous animation
            this.animate().rotation(0F).setDuration(0).start()
            // start new animation
            this.animate()
                .rotation(-30F)
                .setDuration(100)
                .withEndAction {
                    // reset the animation when finished
                    animate().rotation(0F).setDuration(100).start()
                }
                .start()

            runnableHandler.removeCallbacks(hideRewindButtonRunnable)
            // start callback to hide the button
            runnableHandler.postDelayed(hideRewindButtonRunnable, 700)
        }
    }

    private fun forward() {
        player?.seekTo(player!!.currentPosition + seekIncrement)

        // show the forward button
        doubleTapOverlayBinding?.forwardBTN.apply {
            this!!.visibility = View.VISIBLE
            // clear previous animation
            this.animate().rotation(0F).setDuration(0).start()
            // start new animation
            this.animate()
                .rotation(30F)
                .setDuration(100)
                .withEndAction {
                    // reset the animation when finished
                    animate().rotation(0F).setDuration(100).start()
                }
                .start()

            // start callback to hide the button
            runnableHandler.removeCallbacks(hideForwardButtonRunnable)
            runnableHandler.postDelayed(hideForwardButtonRunnable, 700)
        }
    }

    private val hideForwardButtonRunnable = Runnable {
        doubleTapOverlayBinding?.forwardBTN.apply {
            this!!.visibility = View.GONE
        }
    }
    private val hideRewindButtonRunnable = Runnable {
        doubleTapOverlayBinding?.rewindBTN.apply {
            this!!.visibility = View.GONE
        }
    }

    fun onAutoplayClicked() {
        // autoplay options dialog
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.player_autoplay)
            .setItems(
                arrayOf(
                    context.getString(R.string.enabled),
                    context.getString(R.string.disabled)
                )
            ) { _, index ->
                when (index) {
                    0 -> autoplayEnabled = true
                    1 -> autoplayEnabled = false
                }
            }
            .show()
    }

    fun onPlaybackSpeedClicked() {
        val playbackSpeedBinding = DialogSliderBinding.inflate(
            LayoutInflater.from(context)
        )
        playbackSpeedBinding.slider.setSliderRangeAndValue(
            PreferenceRanges.playbackSpeed
        )
        playbackSpeedBinding.slider.value = player?.playbackParameters?.speed ?: 1f
        // change playback speed dialog
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.change_playback_speed)
            .setView(playbackSpeedBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.okay) { _, _ ->
                player?.setPlaybackSpeed(
                    playbackSpeedBinding.slider.value
                )
            }
            .show()
    }

    fun onResizeModeClicked() {
        // switching between original aspect ratio (black bars) and zoomed to fill device screen
        val aspectRatioModeNames = context.resources?.getStringArray(R.array.resizeMode)

        val aspectRatioModes = arrayOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            AspectRatioFrameLayout.RESIZE_MODE_FILL
        )

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.aspect_ratio)
            .setItems(aspectRatioModeNames) { _, index ->
                resizeMode = aspectRatioModes[index]
            }
            .show()
    }

    fun onRepeatModeClicked() {
        val repeatModeNames = arrayOf(
            context.getString(R.string.repeat_mode_none),
            context.getString(R.string.repeat_mode_current)
        )

        val repeatModes = arrayOf(
            RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE,
            RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL

        )
        // repeat mode options dialog
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.repeat_mode)
            .setItems(repeatModeNames) { _, index ->
                player?.repeatMode = repeatModes[index]
            }
            .show()
    }
}
