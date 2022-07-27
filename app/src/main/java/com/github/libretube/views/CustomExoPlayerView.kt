package com.github.libretube.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.github.libretube.R
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.util.DoubleTapListener
import com.github.libretube.util.OnDoubleTapEventListener
import com.google.android.exoplayer2.ui.StyledPlayerView

@SuppressLint("ClickableViewAccessibility")
internal class CustomExoPlayerView(
    context: Context,
    attributeSet: AttributeSet? = null
) : StyledPlayerView(context, attributeSet) {
    val TAG = "CustomExoPlayerView"
    val binding: ExoStyledPlayerControlViewBinding = ExoStyledPlayerControlViewBinding.bind(this)

    private var doubleTapListener: OnDoubleTapEventListener? = null

    // the x-position of where the user clicked
    private var xPos = 0F

    fun setOnDoubleTapListener(
        eventListener: OnDoubleTapEventListener?
    ) {
        doubleTapListener = eventListener
    }

    private fun toggleController() {
        if (isControllerFullyVisible) hideController() else showController()
    }

    val doubleTouchListener = object : DoubleTapListener() {
        override fun onDoubleClick() {
            doubleTapListener?.onEvent(xPos)
        }

        override fun onSingleClick() {
            toggleController()
        }
    }

    init {
        setControllerVisibilityListener {
            // hide the advanced options
            binding.toggleOptions.animate().rotation(0F).setDuration(250).start()
            binding.advancedOptions.visibility = View.GONE
        }
        // set the double click listener for rewind/forward
        setOnClickListener(doubleTouchListener)
    }

    override fun hideController() {
        super.hideController()
        setDoubleTapOverlayLayoutParams(0)
    }

    override fun showController() {
        setDoubleTapOverlayLayoutParams(90)
        super.showController()
    }

    // set the top and bottom margin of the double tap overlay
    fun setDoubleTapOverlayLayoutParams(margin: Int) {
        val dpMargin = resources?.displayMetrics?.density!!.toInt() * margin
        val doubleTapOverlay = binding.root.findViewById<DoubleTapOverlay>(R.id.doubleTapOverlay)
        val params = doubleTapOverlay.layoutParams as MarginLayoutParams
        params.topMargin = dpMargin
        params.bottomMargin = dpMargin
        doubleTapOverlay.layoutParams = params
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // save the x position of the touch event
        xPos = event.x
        // listen for a double touch
        doubleTouchListener.onClick(this)
        return false
    }
}
