package com.github.libretube.dialogs

import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.github.libretube.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URLEncoder

fun showShareDialog(context: Context, videoId: String) {
    val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
    val instancePref = sharedPreferences.getString(
        "instance",
        "https://pipedapi.kavin.rocks"
    )!!
    val instance = "&instance=${URLEncoder.encode(instancePref, "UTF-8")}"
    val shareOptions = arrayOf(
        context.getString(R.string.piped),
        context.getString(R.string.instance),
        context.getString(R.string.youtube)
    )
    MaterialAlertDialogBuilder(context)
        .setTitle(context.getString(R.string.share))
        .setItems(
            shareOptions
        ) { _, id ->
            val url = when (id) {
                0 -> "https://piped.kavin.rocks/watch?v=$videoId"
                1 -> "https://piped.kavin.rocks/watch?v=$videoId$instance"
                2 -> "https://youtu.be/$videoId"
                else -> "https://piped.kavin.rocks/watch?v=$videoId"
            }
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, url)
            intent.type = "text/plain"
            context.startActivity(Intent.createChooser(intent, "Share Url To:"))
        }
        .show()
}
