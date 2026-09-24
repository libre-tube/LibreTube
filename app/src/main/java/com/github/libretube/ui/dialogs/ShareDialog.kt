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
import com.github.libretube.databinding.DialogShareBinding
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.serializable
import com.github.libretube.helpers.ClipboardHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.repo.UserDataRepositoryHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

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
                binding.linkPreview.text = generateLinkText(binding)
            }
            binding.timeStamp.addTextChangedListener {
                binding.linkPreview.text = generateLinkText(binding)
            }
            val timeStamp = shareData.currentPosition
            if (timeStamp == null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        val timeStamp = UserDataRepositoryHelper.userDataRepository.getFromWatchHistory(id)
                            ?.metadata?.positionMillis?.div(10000) ?: return@runCatching
                        withContext(Dispatchers.Main) {
                            binding.timeStamp.setText(timeStamp.toString())
                        }
                    }
                }
            }

            binding.timeStamp.setText((timeStamp ?: 0L).toString())
            if (binding.timeCodeSwitch.isChecked) {
                binding.timeStampInputLayout.isVisible = true
            }
        }

        binding.copyLink.setOnClickListener {
            ClipboardHelper.save(requireContext(), text = binding.linkPreview.text.toString())
        }

        binding.linkPreview.text = generateLinkText(binding)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.share))
            .setView(binding.root)
            .setPositiveButton(R.string.share) { _, _ ->
                val intent = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_TEXT, binding.linkPreview.text.toString())
                    .putExtra(Intent.EXTRA_TITLE, shareData.title)
                    .setType("text/plain")
                val shareIntent = Intent.createChooser(intent, getString(R.string.shareTo))
                requireContext().startActivity(shareIntent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun generateLinkText(
        binding: DialogShareBinding
    ): String {
        val url = when (shareObjectType) {
            ShareObjectType.VIDEO -> {
                val baseUrl = "$YOUTUBE_SHORT_URL/$id"

                val queryParams = mutableListOf<String>()
                if (binding.timeCodeSwitch.isChecked) {
                    queryParams += "t=${binding.timeStamp.text}"
                }

                if (queryParams.isEmpty()) baseUrl
                else baseUrl + "?" + queryParams.joinToString("&")
            }

            ShareObjectType.PLAYLIST -> "$YOUTUBE_FRONTEND_URL/playlist?list=$id"
            else -> "$YOUTUBE_FRONTEND_URL/channel/$id"
        }

        return url
    }

    companion object {
        const val YOUTUBE_FRONTEND_URL = "https://www.youtube.com"
        const val YOUTUBE_MUSIC_URL = "https://music.youtube.com"
        const val YOUTUBE_SHORT_URL = "https://youtu.be"
    }
}
