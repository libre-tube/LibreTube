package com.github.libretube.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri

object IntentHelper {
    fun openLinkFromHref(context: Context, link: String) {
        val uri = Uri.parse(link)
        val launchIntent = Intent(Intent.ACTION_VIEW).setData(uri)
        context.startActivity(launchIntent)
    }
}
