package com.github.libretube.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.view.marginLeft
import com.github.libretube.api.obj.Segment
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.extensions.dpToPx
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ThemeHelper
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.DefaultTimeBar

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

    private val progressBarHeight = (2).dpToPx().toInt()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawSegments(canvas)
    }

    private fun drawSegments(canvas: Canvas) {
        if (player == null) return

        if (!PreferenceHelper.getBoolean(PreferenceKeys.SB_SHOW_MARKERS, true)) return

        val horizontalOffset = (parent as View).marginLeft

        canvas.save()
        length = canvas.width - 2 * horizontalOffset

        val marginY = canvas.height / 2 - progressBarHeight / 2

        segments.forEach {
            canvas.drawRect(
                Rect(
                    (it.segment.first() + horizontalOffset).toLength(),
                    marginY,
                    (it.segment.last() + horizontalOffset).toLength(),
                    canvas.height - marginY
                ),
                Paint().apply {
                    color = ThemeHelper.getThemeColor(
                        context,
                        com.google.android.material.R.attr.colorOnSecondary
                    )
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
