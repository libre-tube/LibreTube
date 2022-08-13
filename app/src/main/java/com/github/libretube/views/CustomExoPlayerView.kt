package com.github.libretube.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.interfaces.DoubleTapInterface
import com.github.libretube.util.DoubleTapListener
import com.google.android.exoplayer2.ui.StyledPlayerView

@SuppressLint("ClickableViewAccessibility")
internal class CustomExoPlayerView(
    context: Context,
    attributeSet: AttributeSet? = null
) : StyledPlayerView(context, attributeSet) {
    val TAG = "CustomExoPlayerView"
    val binding: ExoStyledPlayerControlViewBinding = ExoStyledPlayerControlViewBinding.bind(this)

    private var doubleTapListener: DoubleTapInterface? = null

    // the x-position of where the user clicked
    private var xPos = 0F

    fun setOnDoubleTapListener(
        eventListener: DoubleTapInterface?
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
        // set the double click listener for rewind/forward
        setOnClickListener(doubleTouchListener)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // save the x position of the touch event
        xPos = event.x
        // listen for a double touch
        doubleTouchListener.onClick(this)
        return false
    }
}
