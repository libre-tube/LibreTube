package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.constants.YouTubeConstants
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareDialog : DialogFragment() {
    private lateinit var id: String
    private lateinit var shareObjectType: ShareObjectType
    private lateinit var shareData: ShareData
    private var customInstances: List<CustomInstance> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            id = it.getString(IntentData.id)!!
            shareObjectType = it.serializable(IntentData.shareObjectType)!!
            shareData = it.parcelable(IntentData.shareData)!!
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val shareableTitle = shareData.currentChannel
            ?: shareData.currentVideo
            ?: shareData.currentPlaylist.orEmpty()

        val binding = DialogShareBinding.inflate(layoutInflater)

        lifecycleScope.launch(Dispatchers.IO) {
            // get the api urls of the other custom instances
            customInstances = Database.customInstanceDao().getAll().filter { it.frontendUrl.isNotEmpty() }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

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

                binding.linkPreview.text = generateLinkText(binding, customInstances)
            }
        }

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
            lifecycleScope.launch(Dispatchers.IO) {
                val timeStamp = shareData.currentPosition
                    ?: DatabaseHelper.getWatchPosition(id)?.div(1000)
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    binding.timeStamp.setText((timeStamp ?: 0L).toString())
                }
            }
            if (binding.timeCodeSwitch.isChecked) {
                binding.timeStampInputLayout.isVisible = true
            }
        }

        binding.copyLink.setOnClickListener {
            ClipboardHelper.save(requireContext(), text = binding.linkPreview.text.toString())
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.share))
            .setView(binding.root)
            .setPositiveButton(R.string.share) { _, _ ->
                val intent = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_TEXT, binding.linkPreview.text.toString())
                    .putExtra(Intent.EXTRA_SUBJECT, shareableTitle)
                    .setType("text/plain")
                val shareIntent = Intent.createChooser(intent, getString(R.string.shareTo))
                requireContext().startActivity(shareIntent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun generateLinkText(
        binding: DialogShareBinding,
        customInstances: List<CustomInstance>
    ): String {
        val host = when {
            binding.piped.isChecked -> YouTubeConstants.PIPED_FRONTEND_URL
            binding.youtube.isChecked -> YouTubeConstants.FRONTEND_URL
            else -> {
                val selectedCustomInstance = customInstances
                    .firstOrNull { it.name.hashCode() == binding.shareHostGroup.checkedRadioButtonId }
                selectedCustomInstance?.frontendUrl?.trimEnd('/') ?: YouTubeConstants.FRONTEND_URL
            }
        }
        val url = when (shareObjectType) {
            ShareObjectType.VIDEO -> {
                val queryParams = mutableListOf<String>()
                if (host != YouTubeConstants.FRONTEND_URL) {
                    queryParams.add("v=${id}")
                }
                if (binding.timeCodeSwitch.isChecked) {
                    queryParams += "t=${binding.timeStamp.text}"
                }
                val baseUrl =
                    if (host == YouTubeConstants.FRONTEND_URL) "${YouTubeConstants.SHORT_URL}/$id" else "$host${YouTubeConstants.WATCH_PATH}"

                if (queryParams.isEmpty()) baseUrl
                else baseUrl + "?" + queryParams.joinToString("&")
            }

            ShareObjectType.PLAYLIST -> "$host${YouTubeConstants.PLAYLIST_PATH}$id"
            else -> "$host${YouTubeConstants.CHANNEL_PATH}$id"
        }

        return url
    }

    companion object {
        const val YOUTUBE_FRONTEND_URL = YouTubeConstants.FRONTEND_URL
        const val YOUTUBE_MUSIC_URL = YouTubeConstants.MUSIC_URL
        const val YOUTUBE_SHORT_URL = YouTubeConstants.SHORT_URL
        const val PIPED_FRONTEND_URL = YouTubeConstants.PIPED_FRONTEND_URL
    }
}
