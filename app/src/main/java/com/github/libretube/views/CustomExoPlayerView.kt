package com.github.libretube.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.github.libretube.R
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.google.android.exoplayer2.ui.StyledPlayerView

internal class CustomExoPlayerView(
    context: Context,
    attributeSet: AttributeSet? = null
) : StyledPlayerView(context, attributeSet) {
    val TAG = "CustomExoPlayerView"
    val binding: ExoStyledPlayerControlViewBinding = ExoStyledPlayerControlViewBinding.bind(this)

    init {
        setControllerVisibilityListener {
            // hide the advanced options
            binding.toggleOptions.animate().rotation(0F).setDuration(250).start()
            binding.advancedOptions.visibility = View.GONE
        }
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isControllerFullyVisible) {
                    hideController()
                } else {
                    showController()
                }
            }
        }
        return false
    }
}
