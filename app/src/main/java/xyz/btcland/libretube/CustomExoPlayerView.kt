package xyz.btcland.libretube

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.exoplayer2.ui.PlayerView

internal class CustomExoPlayerView(
    context: Context, attributeSet: AttributeSet? = null
) : PlayerView(context, attributeSet) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                showController()
            }
        }
        return false
    }
}