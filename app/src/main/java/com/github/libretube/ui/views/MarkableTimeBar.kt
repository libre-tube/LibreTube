package com.github.libretube.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import com.github.libretube.api.obj.Segment
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.util.PreferenceHelper
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.DefaultTimeBar

/**
 * TimeBar that can be marked with SponsorBlock Segments
 */
class MarkableTimeBar(
    context: Context,
    attributeSet: AttributeSet? = null
) : DefaultTimeBar(context, attributeSet) {

    private var segments: List<Segment> = listOf(Segment(segment = listOf(1f, 10f)), Segment(segment = listOf(20f, 30f)))
    private var player: Player? = null
    private var length: Int = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawSegments(canvas)
    }

    private fun drawSegments(canvas: Canvas) {
        if (player == null) return

        if (!PreferenceHelper.getBoolean(PreferenceKeys.SB_SHOW_MARKERS, false)) return

        canvas.save()
        length = canvas.width - 2 * HORIZONTAL_OFFSET

        val marginY = canvas.height / 2 - 3

        segments.forEach {
            canvas.drawRect(
                Rect(
                    (it.segment!!.first() + HORIZONTAL_OFFSET).toLength(),
                    marginY,
                    (it.segment.last() + HORIZONTAL_OFFSET).toLength(),
                    canvas.height - marginY
                ),
                Paint().apply {
                    color = Color.RED
                }
            )
        }
        canvas.restore()
    }

    private fun Float.toLength(): Int {
        val position = (this * 1000 / player!!.duration * length).toInt()
        return maxOf(
            minOf(position, length - HORIZONTAL_OFFSET),
            HORIZONTAL_OFFSET
        )
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
        const val HORIZONTAL_OFFSET = 8
    }
}
