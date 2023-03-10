package com.github.libretube.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.libretube.R
import com.github.libretube.extensions.toastFromMainThread

object IntentHelper {
    fun openLinkFromHref(context: Context, link: String) {
        val uri = Uri.parse(link)
        val launchIntent = Intent(Intent.ACTION_VIEW).setData(uri)
        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            context.toastFromMainThread(R.string.unknown_error)
        }
    }
}
