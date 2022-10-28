package com.github.libretube.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import com.github.libretube.api.obj.Segment
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.DefaultTimeBar

class MarkableTimeBar(
    context: Context,
    attributeSet: AttributeSet? = null
) : DefaultTimeBar(context, attributeSet) {

    private val HORIZONTAL_OFFSET = 5

    private var segments: List<Segment> = listOf(Segment(segment = listOf(1f, 10f)), Segment(segment = listOf(20f, 30f)))
    private var player: Player? = null
    private var length: Int = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawSegments(canvas)
    }

    private fun drawSegments(canvas: Canvas) {
        if (player == null) return

        canvas.save()
        length = canvas.width

        val marginY = canvas.height / 2 - 3

        segments.forEach {
            canvas.drawRect(
                Rect(
                    it.segment!!.first().toLength(),
                    marginY,
                    it.segment.last().toLength(),
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

    fun setPlayer(player: Player) {
        this.player = player
    }
}
