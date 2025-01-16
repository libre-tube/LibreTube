package com.github.libretube.helpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.content.getSystemService
import com.github.libretube.R

object ClipboardHelper {
    fun save(
        context: Context,
        label: String = context.getString(R.string.copied),
        text: String,
        notify: Boolean = false
    ) {
        val clip = ClipData.newPlainText(label, text)
        context.getSystemService<ClipboardManager>()!!.setPrimaryClip(clip)

        if (notify && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
        }
    }
}
