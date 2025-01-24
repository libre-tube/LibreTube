package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.DialogShareBinding
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.serializable
import com.github.libretube.helpers.ClipboardHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.ShareData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class ShareDialog : DialogFragment() {
    private lateinit var id: String
    private lateinit var shareObjectType: ShareObjectType
    private lateinit var shareData: ShareData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            id = it.getString(IntentData.id)!!
            shareObjectType = it.serializable(IntentData.shareObjectType)!!
            shareData = it.parcelable(IntentData.shareData)!!
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val customInstanceUrl = getCustomInstanceFrontendUrl().toHttpUrlOrNull()
        val shareableTitle = shareData.currentChannel
            ?: shareData.currentVideo
            ?: shareData.currentPlaylist.orEmpty()

        val binding = DialogShareBinding.inflate(layoutInflater)

        binding.shareHostGroup.check(
            when (PreferenceHelper.getInt(PreferenceKeys.SELECTED_SHARE_HOST, 0)) {
                0 -> binding.youtube.id
                1 -> binding.piped.id
                else -> if (customInstanceUrl != null) binding.customInstance.id else 0
            }
        )

        binding.shareHostGroup.setOnCheckedChangeListener { _, _ ->
            binding.linkPreview.text = generateLinkText(binding, customInstanceUrl)
            PreferenceHelper.putInt(
                PreferenceKeys.SELECTED_SHARE_HOST, when {
                    binding.youtube.isChecked -> 0
                    binding.piped.isChecked -> 1
                    else -> 2
                }
            )
        }

        if (customInstanceUrl != null) {
            binding.customInstance.isVisible = true
            binding.customInstance.text = customInstanceUrl.host
        }
        if (shareObjectType == ShareObjectType.VIDEO) {
            binding.timeStampSwitchLayout.isVisible = true
            binding.timeCodeSwitch.isChecked = PreferenceHelper.getBoolean(
                PreferenceKeys.SHARE_WITH_TIME_CODE,
                false
            )
            binding.timeCodeSwitch.setOnCheckedChangeListener { _, isChecked ->
                binding.timeStampInputLayout.isVisible = isChecked
                PreferenceHelper.putBoolean(PreferenceKeys.SHARE_WITH_TIME_CODE, isChecked)
                binding.linkPreview.text = generateLinkText(binding, customInstanceUrl)
            }
            binding.timeStamp.addTextChangedListener {
                binding.linkPreview.text = generateLinkText(binding, customInstanceUrl)
            }
            val timeStamp = shareData.currentPosition ?: DatabaseHelper.getWatchPositionBlocking(id)?.div(1000)
            binding.timeStamp.setText((timeStamp ?: 0L).toString())
            if (binding.timeCodeSwitch.isChecked) {
                binding.timeStampInputLayout.isVisible = true
            }
        }

        binding.copyLink.setOnClickListener {
            ClipboardHelper.save(requireContext(), text = binding.linkPreview.text.toString())
        }

        binding.linkPreview.text = generateLinkText(binding, customInstanceUrl)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.share))
            .setView(binding.root)
            .setPositiveButton(R.string.share) { _, _ ->
                val intent = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_TEXT, binding.linkPreview.text)
                    .putExtra(Intent.EXTRA_SUBJECT, shareableTitle)
                    .setType("text/plain")
                val shareIntent = Intent.createChooser(intent, getString(R.string.shareTo))
                requireContext().startActivity(shareIntent)
            }
            .show()
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

    private fun generateLinkText(binding: DialogShareBinding, customInstanceUrl: HttpUrl?): String {
        val host = when {
            binding.piped.isChecked -> PIPED_FRONTEND_URL
            binding.youtube.isChecked -> YOUTUBE_FRONTEND_URL
            // only available for custom instances
            else -> customInstanceUrl!!.toString().trimEnd('/')
        }
        var url = when {
            shareObjectType == ShareObjectType.VIDEO && host == YOUTUBE_FRONTEND_URL -> "$YOUTUBE_SHORT_URL/$id"
            shareObjectType == ShareObjectType.VIDEO -> "$host/watch?v=$id"
            shareObjectType == ShareObjectType.PLAYLIST -> "$host/playlist?list=$id"
            else -> "$host/channel/$id"
        }

        if (shareObjectType == ShareObjectType.VIDEO && binding.timeCodeSwitch.isChecked) {
            url += "&t=${binding.timeStamp.text}"
        }

        return url
    }

    companion object {
        const val YOUTUBE_FRONTEND_URL = "https://www.youtube.com"
        const val YOUTUBE_SHORT_URL = "https://youtu.be"
        const val PIPED_FRONTEND_URL = "https://piped.video"
    }
}
