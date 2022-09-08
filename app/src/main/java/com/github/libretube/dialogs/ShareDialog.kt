package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.constants.PIPED_FRONTEND_URL
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.constants.YOUTUBE_FRONTEND_URL
import com.github.libretube.databinding.DialogShareBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.CustomInstance
import com.github.libretube.extensions.await
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ShareDialog(
    private val id: String,
    private val isPlaylist: Boolean,
    private val position: Long? = null
) : DialogFragment() {
    private var binding: DialogShareBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var shareOptions = arrayOf(
            getString(R.string.piped),
            getString(R.string.youtube)
        )
        val instanceUrl = getCustomInstanceFrontendUrl()

        // add instanceUrl option if custom instance frontend url available
        if (instanceUrl != "") shareOptions += getString(R.string.instance)

        if (position != null) {
            binding = DialogShareBinding.inflate(layoutInflater)
            binding!!.timeCodeSwitch.isChecked = PreferenceHelper.getBoolean(
                PreferenceKeys.SHARE_WITH_TIME_CODE,
                true
            )
        }

        return MaterialAlertDialogBuilder(requireContext())
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

                if (binding != null && binding!!.timeCodeSwitch.isChecked) {
                    url += "&t=$position"
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
            .setView(binding?.root)
            .show()
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
            customInstances = DatabaseHolder.db.customInstanceDao().getAll()
        }.await()

        // return the custom instance frontend url if available
        customInstances.forEach { instance ->
            if (instance.apiUrl == instancePref) return instance.apiUrl
        }
        return ""
    }
}
