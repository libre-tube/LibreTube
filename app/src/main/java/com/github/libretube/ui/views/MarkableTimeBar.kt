package com.github.libretube.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import com.github.libretube.R
import com.github.libretube.api.obj.Segment
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.extensions.toPixel
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.util.ThemeHelper
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.DefaultTimeBar
import kotlin.math.roundToInt

/**
 * TimeBar that can be marked with SponsorBlock Segments
 */
class MarkableTimeBar(
    context: Context,
    attributeSet: AttributeSet? = null
) : DefaultTimeBar(context, attributeSet) {

    private var segments: List<Segment> = listOf()
    private var player: Player? = null
    private var length: Int = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawSegments(canvas)
    }

    private fun drawSegments(canvas: Canvas) {
        if (player == null) return

        if (!PreferenceHelper.getBoolean(PreferenceKeys.SB_SHOW_MARKERS, true)) return

        canvas.save()
        length = canvas.width - 2 * HORIZONTAL_OFFSET

        val marginY = canvas.height / 2 - PROGRESS_BAR_HEIGHT / 2

        segments.forEach {
            canvas.drawRect(
                Rect(
                    (it.segment.first() + HORIZONTAL_OFFSET).toLength(),
                    marginY,
                    (it.segment.last() + HORIZONTAL_OFFSET).toLength(),
                    canvas.height - marginY
                ),
                Paint().apply {
                    color = ThemeHelper.getThemeColor(context, R.attr.colorOnSecondary)
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

    companion object {
        const val HORIZONTAL_OFFSET = 10
        val PROGRESS_BAR_HEIGHT = (2).toPixel().roundToInt()
    }
}
