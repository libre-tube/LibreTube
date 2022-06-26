package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.github.libretube.R
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ShareDialog(
    private val id: String,
    private val isPlaylist: Boolean
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            var shareOptions = arrayOf(
                getString(R.string.piped),
                getString(R.string.youtube)
            )
            val instanceUrl = getCustomInstanceFrontendUrl()

            // add instanceUrl option if custom instance frontend url available
            if (instanceUrl != "") shareOptions += getString(R.string.instance)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(context?.getString(R.string.share))
                .setItems(
                    shareOptions
                ) { _, which ->
                    val host = when (which) {
                        0 -> "https://piped.kavin.rocks"
                        1 -> "https://youtube.com"
                        // only available for custom instances
                        else -> instanceUrl
                    }
                    val path = if (!isPlaylist) "/watch?v=$id" else "/playlist?list=$id"
                    val url = "$host$path"

                    val intent = Intent()
                    intent.apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, url)
                        type = "text/plain"
                    }
                    context?.startActivity(
                        Intent.createChooser(intent, context?.getString(R.string.shareTo))
                    )
                }
                .show()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    // get the frontend url if it's a custom instance
    private fun getCustomInstanceFrontendUrl(): String {
        val instancePref = PreferenceHelper.getString(
            requireContext(),
            "selectInstance",
            "https://pipedapi.kavin.rocks"
        )

        // get the api urls of the other custom instances
        val customInstances = PreferenceHelper.getCustomInstances(requireContext())

        // return the custom instance frontend url if available
        customInstances.forEach { instance ->
            if (instance.apiUrl == instancePref) return instance.apiUrl
        }
        return ""
    }
}
