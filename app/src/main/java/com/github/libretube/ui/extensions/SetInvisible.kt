package com.github.libretube.ui.extensions

import android.view.View

fun View.setInvisible(value: Boolean) {
    this.visibility = if (value) View.INVISIBLE else View.VISIBLE
}
