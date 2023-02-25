package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.constants.PIPED_FRONTEND_URL
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.constants.YOUTUBE_FRONTEND_URL
import com.github.libretube.databinding.DialogShareBinding
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.ShareData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class ShareDialog(
    private val id: String,
    private val shareObjectType: ShareObjectType,
    private val shareData: ShareData
) : DialogFragment() {
    private var binding: DialogShareBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var shareOptions = arrayOf(
            getString(R.string.piped),
            getString(R.string.youtube)
        )
        val instanceUrl = getCustomInstanceFrontendUrl()
        val shareableTitle = shareData.currentChannel
            ?: shareData.currentVideo
            ?: shareData.currentPlaylist.orEmpty()
        // add instanceUrl option if custom instance frontend url available
        if (instanceUrl.isNotEmpty()) {
            shareOptions += getString(R.string.instance)
        }

        if (shareObjectType == ShareObjectType.VIDEO) {
            setupTimeStampBinding()
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
                val path = when (shareObjectType) {
                    ShareObjectType.VIDEO -> "/watch?v=$id"
                    ShareObjectType.PLAYLIST -> "/playlist?list=$id"
                    else -> "/channel/$id"
                }
                var url = "$host$path"

                if (shareObjectType == ShareObjectType.VIDEO && binding!!.timeCodeSwitch.isChecked) {
                    url += "&t=${binding!!.timeStamp.text}"
                }

                val intent = Intent()
                intent.apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, url)
                    putExtra(Intent.EXTRA_SUBJECT, shareableTitle)
                    type = "text/plain"
                }
                context?.startActivity(
                    Intent.createChooser(intent, context?.getString(R.string.shareTo))
                )
            }
            .setView(binding?.root)
            .show()
    }

    private fun setupTimeStampBinding() {
        binding = DialogShareBinding.inflate(layoutInflater)
        binding!!.timeCodeSwitch.isChecked = PreferenceHelper.getBoolean(
            PreferenceKeys.SHARE_WITH_TIME_CODE,
            true
        )
        binding!!.timeCodeSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding!!.timeStampLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            PreferenceHelper.putBoolean(PreferenceKeys.SHARE_WITH_TIME_CODE, isChecked)
        }
        binding!!.timeStamp.setText((shareData.currentPosition ?: 0L).toString())
        if (binding!!.timeCodeSwitch.isChecked) binding!!.timeStampLayout.visibility = View.VISIBLE
    }

    // get the frontend url if it's a custom instance
    private fun getCustomInstanceFrontendUrl(): String {
        val instancePref = PreferenceHelper.getString(
            PreferenceKeys.FETCH_INSTANCE,
            PIPED_FRONTEND_URL
        )

        // get the api urls of the other custom instances
        val customInstances = runBlocking(Dispatchers.IO) {
            Database.customInstanceDao().getAll()
        }

        // return the custom instance frontend url if available
        return customInstances.firstOrNull { it.apiUrl == instancePref }?.frontendUrl.orEmpty()
    }
}
