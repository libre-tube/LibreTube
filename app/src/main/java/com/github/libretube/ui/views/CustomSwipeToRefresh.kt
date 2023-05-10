package com.github.libretube.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_MOVE
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.libretube.helpers.ThemeHelper
import com.google.android.material.elevation.SurfaceColors
import kotlin.math.abs

class CustomSwipeToRefresh(context: Context?, attrs: AttributeSet?) :
    SwipeRefreshLayout(context!!, attrs) {
    private val mTouchSlop: Int = ViewConfiguration.get(this.context).scaledTouchSlop
    private var mPrevX = 0f

    init {
        setColorSchemeColors(
            ThemeHelper.getThemeColor(this.context, androidx.appcompat.R.attr.colorPrimary),
        )
        setProgressBackgroundColorSchemeColor(
            SurfaceColors.getColorForElevation(this.context, 20f),
        )
    }

    @SuppressLint("Recycle")
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> mPrevX = MotionEvent.obtain(event).x
            ACTION_MOVE -> {
                val eventX = event.x
                val xDiff = abs(eventX - mPrevX)
                if (xDiff > mTouchSlop) {
                    return false
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }
}
