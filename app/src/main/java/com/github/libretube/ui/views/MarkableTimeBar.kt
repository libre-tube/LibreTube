package com.github.libretube.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.view.marginLeft
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import com.github.libretube.api.obj.Segment
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.extensions.dpToPx
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ThemeHelper
import com.google.android.material.R

/**
 * TimeBar that can be marked with SponsorBlock Segments
 */
@UnstableApi
class MarkableTimeBar(
    context: Context,
    attributeSet: AttributeSet? = null
) : DefaultTimeBar(context, attributeSet) {

    private var segments: List<Segment> = listOf()
    private var player: Player? = null
    private var length: Int = 0

    private val progressBarHeight = (2).dpToPx().toInt()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawSegments(canvas)
    }

    private fun drawSegments(canvas: Canvas) {
        if (player == null) return

        canvas.save()
        val horizontalOffset = (parent as View).marginLeft
        length = canvas.width - horizontalOffset * 2
        val marginY = canvas.height / 2 - progressBarHeight / 2
        val themeColor = ThemeHelper.getThemeColor(context, R.attr.colorOnSecondary)

        segments.forEach {
            val (start, end) = it.segmentStartAndEnd

            canvas.drawRect(
                Rect(
                    start.toLength() + horizontalOffset,
                    marginY,
                    end.toLength() + horizontalOffset,
                    canvas.height - marginY
                ),
                Paint().apply {
                    color = if (PreferenceHelper.getBoolean("sb_enable_custom_colors", false)) {
                        PreferenceHelper.getInt(it.category + "_color", themeColor)
                    } else {
                        themeColor
                    }
                }
            )
        }
        canvas.restore()
    }

    private fun Double.toLength(): Int {
        return (this * 1000 / player!!.duration * length).toInt()
    }

    fun setSegments(segments: List<Segment>) {
        this.segments = segments
    }

    fun clearSegments() {
        segments = listOf()
    }

    fun setPlayer(player: Player) {
        this.player = player
    }
}
