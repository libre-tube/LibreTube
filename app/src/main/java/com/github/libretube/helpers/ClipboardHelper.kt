package com.github.libretube.helpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.github.libretube.R

class ClipboardHelper(
    private val context: Context
) {
    fun save(text: String) {
        val clipboard: ClipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(context.getString(R.string.copied), text)
        clipboard.setPrimaryClip(clip)
    }
}
