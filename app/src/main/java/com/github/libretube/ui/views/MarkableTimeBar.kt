package com.github.libretube.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
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
    attributeSet: AttributeSet? = null,
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
        val markersEnabled = PreferenceHelper.getBoolean(PreferenceKeys.SB_SHOW_MARKERS, true)
        //TODO Add ability to change these colors via settings
        val segmentColors = mapOf(
            "intro" to "#00ffff",
            "selfpromo" to "#ffff00",
            "interaction" to "#cc00ff",
            "sponsor" to "#00d400",
            "outro" to "#0202ED",
            "filler" to "#7300ff",
            "music_offtopic" to "#ff9900",
            "preview" to "#008fd6"
        )
        if (player == null || !markersEnabled) return

        canvas.save()
        val horizontalOffset = (parent as View).marginLeft
        length = canvas.width - horizontalOffset * 2
        val marginY = canvas.height / 2 - progressBarHeight / 2

        segments.forEach {
            canvas.drawRect(
                Rect(
                    it.segment.first().toLength() + horizontalOffset,
                    marginY,
                    it.segment.last().toLength() + horizontalOffset,
                    canvas.height - marginY,
                ),
                Paint().apply {
                    color =  Color.parseColor(segmentColors[it.category])
                },
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
