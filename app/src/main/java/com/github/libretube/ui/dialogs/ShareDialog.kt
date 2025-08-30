package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.DialogShareBinding
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.CustomInstance
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.serializable
import com.github.libretube.helpers.ClipboardHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.ShareData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

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
        // get the api urls of the other custom instances
        val customInstances = runBlocking(Dispatchers.IO) {
            Database.customInstanceDao().getAll().filter { it.frontendUrl.isNotEmpty() }
        }

        val shareableTitle = shareData.currentChannel
            ?: shareData.currentVideo
            ?: shareData.currentPlaylist.orEmpty()

        val binding = DialogShareBinding.inflate(layoutInflater)

        // add one radio button per custom instance
        for (customInstance in customInstances) {
            val radioButton = RadioButton(context).apply {
                text = customInstance.name
                // the view ids are the hash code of the name
                // this guarantees that the right instance is selected
                // even if the order of the custom instances changed
                id = customInstance.name.hashCode()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            binding.shareHostGroup.addView(radioButton)
        }

        binding.shareHostGroup.check(
            when (val previousSelection =
                PreferenceHelper.getInt(PreferenceKeys.SELECTED_SHARE_HOST, 0)) {
                0 -> binding.youtube.id
                1 -> binding.piped.id
                else -> customInstances.firstOrNull {
                    it.name.hashCode() == previousSelection
                }?.name?.hashCode() ?: 0
            }
        )

        binding.shareHostGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.linkPreview.text = generateLinkText(binding, customInstances)
            PreferenceHelper.putInt(
                PreferenceKeys.SELECTED_SHARE_HOST, when {
                    binding.youtube.isChecked -> 0
                    binding.piped.isChecked -> 1
                    else -> checkedId
                }
            )
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
                binding.linkPreview.text = generateLinkText(binding, customInstances)
            }
            binding.timeStamp.addTextChangedListener {
                binding.linkPreview.text = generateLinkText(binding, customInstances)
            }
            val timeStamp =
                shareData.currentPosition ?: DatabaseHelper.getWatchPositionBlocking(id)?.div(1000)
            binding.timeStamp.setText((timeStamp ?: 0L).toString())
            if (binding.timeCodeSwitch.isChecked) {
                binding.timeStampInputLayout.isVisible = true
            }
        }

        binding.copyLink.setOnClickListener {
            ClipboardHelper.save(requireContext(), text = binding.linkPreview.text.toString())
        }

        binding.linkPreview.text = generateLinkText(binding, customInstances)

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

    private fun generateLinkText(
        binding: DialogShareBinding,
        customInstances: List<CustomInstance>
    ): String {
        val host = when {
            binding.piped.isChecked -> PIPED_FRONTEND_URL
            binding.youtube.isChecked -> YOUTUBE_FRONTEND_URL
            // only available for custom instances
            else -> {
                val selectedCustomInstance = customInstances
                    .firstOrNull { it.name.hashCode() == binding.shareHostGroup.checkedRadioButtonId }
                selectedCustomInstance?.frontendUrl?.trimEnd('/') ?: YOUTUBE_FRONTEND_URL
            }
        }
        val url = when (shareObjectType) {
            ShareObjectType.VIDEO -> {
                val queryParams = mutableListOf<String>()
                if (host != YOUTUBE_FRONTEND_URL) {
                    queryParams.add("v=${id}")
                }
                if (binding.timeCodeSwitch.isChecked) {
                    queryParams += "t=${binding.timeStamp.text}"
                }
                val baseUrl =
                    if (host == YOUTUBE_FRONTEND_URL) "$YOUTUBE_SHORT_URL/$id" else "$host/watch"

                if (queryParams.isEmpty()) baseUrl
                else baseUrl + "?" + queryParams.joinToString("&")
            }

            ShareObjectType.PLAYLIST -> "$host/playlist?list=$id"
            else -> "$host/channel/$id"
        }

        return url
    }

    companion object {
        const val YOUTUBE_FRONTEND_URL = "https://www.youtube.com"
        const val YOUTUBE_MUSIC_URL = "https://music.youtube.com"
        const val YOUTUBE_SHORT_URL = "https://youtu.be"
        const val PIPED_FRONTEND_URL = "https://piped.video"
    }
}
