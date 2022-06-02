package com.github.libretube.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.exoplayer2.ui.StyledPlayerView

internal class CustomExoPlayerView(
    context: Context,
    attributeSet: AttributeSet? = null
) : StyledPlayerView(context, attributeSet) {

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
