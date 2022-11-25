package com.github.libretube.ui.extensions

import android.text.util.Linkify
import android.widget.TextView
import androidx.core.text.HtmlCompat

fun TextView.setFormattedHtml(text: String) {
    Linkify.addLinks(this, Linkify.WEB_URLS)
    this.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
}
