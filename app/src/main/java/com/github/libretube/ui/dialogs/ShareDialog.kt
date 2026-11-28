package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.DialogShareBinding
import com.github.libretube.extensions.parcelable
import com.github.libretube.helpers.ClipboardHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ShareHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.repo.UserDataRepositoryHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareDialog : DialogFragment() {
    private lateinit var id: String
    private lateinit var shareData: ShareData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            id = it.getString(IntentData.id)!!
            shareData = it.parcelable(IntentData.shareData)!!
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogShareBinding.inflate(layoutInflater)

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

        binding.copyLink.setOnClickListener {
            ClipboardHelper.save(requireContext(), text = binding.linkPreview.text.toString())
        }

        binding.linkPreview.text = generateLinkText(binding)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.share))
            .setView(binding.root)
            .setPositiveButton(R.string.share) { _, _ ->
                ShareHelper.shareLink(
                    requireContext(),
                    generateLinkText(binding),
                    shareData.title,
                    shareData.previewImageUrl
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }


    private fun generateLinkText(
        binding: DialogShareBinding,
    ): String = "$YOUTUBE_SHORT_URL/$id${if (binding.timeCodeSwitch.isChecked) "?t=${binding.timeStamp.text}" else ""}"

    companion object {
        const val YOUTUBE_FRONTEND_URL = "https://www.youtube.com"
        const val YOUTUBE_MUSIC_URL = "https://music.youtube.com"
        const val YOUTUBE_SHORT_URL = "https://youtu.be"
    }
}
