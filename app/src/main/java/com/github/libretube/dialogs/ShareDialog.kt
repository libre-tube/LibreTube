package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.github.libretube.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URLEncoder

class ShareDialog(private val videoId: String) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
            val instancePref = sharedPreferences.getString(
                "selectInstance",
                "https://pipedapi.kavin.rocks"
            )!!
            val instance = "&instance=${URLEncoder.encode(instancePref, "UTF-8")}"
            val shareOptions = arrayOf(
                context?.getString(R.string.piped),
                context?.getString(R.string.instance),
                context?.getString(R.string.youtube)
            )
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(context?.getString(R.string.share))
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
                    context?.startActivity(
                        Intent.createChooser(intent, context?.getString(R.string.shareTo))
                    )
                }
                .show()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
