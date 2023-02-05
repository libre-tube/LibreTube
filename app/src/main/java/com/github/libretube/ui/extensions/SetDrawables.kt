package com.github.libretube.ui.extensions

import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources

fun TextView.setDrawables(
    start: Int? = null,
    top: Int? = null,
    end: Int? = null,
    bottom: Int? = null
) {
    setCompoundDrawablesRelativeWithIntrinsicBounds(
        start?.let { AppCompatResources.getDrawable(context, it) },
        top?.let { AppCompatResources.getDrawable(context, it) },
        end?.let { AppCompatResources.getDrawable(context, it) },
        bottom?.let { AppCompatResources.getDrawable(context, it) }
    )
}
