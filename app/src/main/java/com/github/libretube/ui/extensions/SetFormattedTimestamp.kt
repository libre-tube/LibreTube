package com.github.libretube.ui.extensions

import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.core.text.set
import androidx.core.text.toSpannable
import androidx.core.text.toSpanned
import com.github.libretube.util.TextUtils

fun TextView.setFormattedTimestamp(
    text: String,
    onClick: (time: Long) -> Unit
) {
    val regex = TextUtils.TIMESTAMP_REGEX.toRegex()

    // If text does not contain any timestamp, set text as it is.
    if (!text.contains(regex)) {
        this.text = text
        return
    }

    val spannableString = text.toSpannable()

    // Set clickable span for each timestamps present in the text.
    regex.findAll(text).forEach { result ->
        // ClickableSpan object must be created individually for each timestamp.
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                (view as TextView).text.toSpanned().let {
                    val time = it.substring(it.getSpanStart(this), it.getSpanEnd(this))
                    onClick(TextUtils.toTimeInSeconds(time))
                }
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }

        val range = result.range
        spannableString[range.first, range.last + 1] = clickableSpan
    }
    this.text = spannableString
    this.movementMethod = LinkMovementMethod.getInstance()
}
