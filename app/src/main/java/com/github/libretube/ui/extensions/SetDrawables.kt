package com.github.libretube.ui.extensions

import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.TextViewCompat

fun TextView.setDrawables(
    start: Int? = null,
    top: Int? = null,
    end: Int? = null,
    bottom: Int? = null
) {
    setDrawables(
        start?.let { AppCompatResources.getDrawable(context, it) },
        top?.let { AppCompatResources.getDrawable(context, it) },
        end?.let { AppCompatResources.getDrawable(context, it) },
        bottom?.let { AppCompatResources.getDrawable(context, it) }
    )
}

fun TextView.setDrawables(
    start: Drawable? = null,
    top: Drawable? = null,
    end: Drawable? = null,
    bottom: Drawable? = null
) {
    TextViewCompat.setCompoundDrawablesRelative(this, start, top, end, bottom)
}
