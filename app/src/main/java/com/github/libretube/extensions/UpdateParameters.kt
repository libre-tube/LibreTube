package com.github.libretube.extensions

import com.google.android.exoplayer2.trackselection.DefaultTrackSelector

fun DefaultTrackSelector.updateParameters(
    actions: DefaultTrackSelector.Parameters.Builder.() -> Unit
) = apply {
    val params = buildUponParameters().apply(actions)
    setParameters(params)
}
