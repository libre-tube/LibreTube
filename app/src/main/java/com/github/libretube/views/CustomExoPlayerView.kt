package com.github.libretube.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.google.android.exoplayer2.ui.StyledPlayerView

internal class CustomExoPlayerView(
    context: Context,
    attributeSet: AttributeSet? = null
) : StyledPlayerView(context, attributeSet) {
    val binding: ExoStyledPlayerControlViewBinding = ExoStyledPlayerControlViewBinding.bind(this)

    override fun hideController() {
        super.hideController()
        binding.toggleOptions.animate().rotationX(0F).start()
        binding.advancedOptions.visibility = View.GONE
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
