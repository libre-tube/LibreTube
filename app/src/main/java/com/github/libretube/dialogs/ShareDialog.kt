package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.github.libretube.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ShareDialog(private val videoId: String) : DialogFragment() {

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
                ) { _, id ->
                    val url = when (id) {
                        0 -> "https://piped.kavin.rocks/watch?v=$videoId"
                        1 -> "https://youtu.be/$videoId"
                        2 -> "$instanceUrl/watch?v=$videoId" // only available for custom instances
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

    // get the frontend url if it's a custom instance
    private fun getCustomInstanceFrontendUrl(): String {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        val instancePref = sharedPreferences.getString(
            "selectInstance",
            "https://pipedapi.kavin.rocks"
        )

        // get the api urls of the other custom instances
        var customInstancesUrls = try {
            sharedPreferences
                .getStringSet("custom_instances_url", HashSet())!!.toList()
        } catch (e: Exception) {
            emptyList()
        }

        // get the frontend urls of the other custom instances
        var customInstancesFrontendUrls = try {
            sharedPreferences
                .getStringSet("custom_instances_url", HashSet())!!.toList()
        } catch (e: Exception) {
            emptyList()
        }

        // return the custom instance frontend url if available
        return if (customInstancesUrls.contains(instancePref)) {
            val index = customInstancesUrls.indexOf(instancePref)
            return customInstancesFrontendUrls[index]
        } else {
            ""
        }
    }
}
