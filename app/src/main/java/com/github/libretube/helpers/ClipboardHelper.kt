package com.github.libretube.helpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService
import com.github.libretube.R

object ClipboardHelper {
    fun save(context: Context, text: String) {
        val clip = ClipData.newPlainText(context.getString(R.string.copied), text)
        context.getSystemService<ClipboardManager>()!!.setPrimaryClip(clip)
    }
}
