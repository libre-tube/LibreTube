package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.github.libretube.PIPED_FRONTEND_URL
import com.github.libretube.R
import com.github.libretube.YOUTUBE_FRONTEND_URL
import com.github.libretube.database.DatabaseHolder
import com.github.libretube.obj.CustomInstance
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ShareDialog(
    private val id: String,
    private val isPlaylist: Boolean,
    private val position: Long = 0L
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
                        0 -> PIPED_FRONTEND_URL
                        1 -> YOUTUBE_FRONTEND_URL
                        // only available for custom instances
                        else -> instanceUrl
                    }
                    val path = if (!isPlaylist) "/watch?v=$id" else "/playlist?list=$id"
                    var url = "$host$path"
                    if (PreferenceHelper.getBoolean(
                            PreferenceKeys.SHARE_WITH_TIME_CODE,
                            true
                        )
                    ) {
                        url += "?t=$position"
                    }

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
            PreferenceKeys.FETCH_INSTANCE,
            PIPED_FRONTEND_URL
        )

        // get the api urls of the other custom instances
        var customInstances = listOf<CustomInstance>()
        Thread {
            customInstances = DatabaseHolder.database.customInstanceDao().getAll()
        }.apply {
            start()
            join()
        }

        // return the custom instance frontend url if available
        customInstances.forEach { instance ->
            if (instance.apiUrl == instancePref) return instance.apiUrl
        }
        return ""
    }
}
