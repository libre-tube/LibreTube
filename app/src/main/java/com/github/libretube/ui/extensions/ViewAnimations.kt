package com.github.libretube.ui.extensions

import android.view.View

/**
 * This function animates a view in a downward direction by a specified amount.
 *
 * @param duration The duration of the animation in milliseconds.
 * @param dy The distance to move the view along the Y-axis.
 * @param onEnd An optional lambda function that is invoked when the animation ends.
 */
fun View.animateDown(duration: Long, dy: Float, onEnd: () -> Unit = { }) {
    this
        .animate()
        .withEndAction { onEnd.invoke() }
        .y(dy)
        .setDuration(duration)
        .start()
}
