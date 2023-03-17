package com.github.libretube.extensions

import com.google.android.exoplayer2.trackselection.DefaultTrackSelector

inline fun DefaultTrackSelector.updateParameters(
    actions: DefaultTrackSelector.Parameters.Builder.() -> Unit
) = setParameters(buildUponParameters().apply(actions))
