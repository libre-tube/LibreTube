package com.github.libretube.util

import android.text.Editable
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.github.libretube.util.HtmlParser.Companion.getValue
import org.xml.sax.Attributes

class LinkHandler(private val clickCallback: ((String) -> Unit)?) : HtmlParser.TagHandler {
    private var linkTagStartIndex = -1
    private var link: String? = null
    override fun handleTag(
        opening: Boolean,
        tag: String?,
        output: Editable?,
        attributes: Attributes?
    ): Boolean {
        if (output != null) {
            if ("a" == tag) {
                if (opening && attributes != null) {
                    linkTagStartIndex = output.length
                    link = getValue(attributes, "href")
                } else {
                    val refTagEndIndex = output.length
                    setLinkSpans(output, linkTagStartIndex, refTagEndIndex, link)
                }
                return true
            }
        }
        return false
    }

    private fun setLinkSpans(output: Editable, start: Int, end: Int, link: String?) {
        output.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    if (clickCallback != null && link != null) {
                        clickCallback.invoke(link)
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            },
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}
