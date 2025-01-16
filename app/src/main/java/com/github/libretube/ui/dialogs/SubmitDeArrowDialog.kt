package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.DeArrowBody
import com.github.libretube.api.obj.DeArrowSubmitThumbnail
import com.github.libretube.api.obj.DeArrowSubmitTitle
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogSubmitDearrowBinding
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.util.TextUtils
import com.github.libretube.util.TextUtils.parseDurationString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubmitDeArrowDialog: DialogFragment() {
    private var videoId: String = ""
    private var currentPosition: Float = 0f

    private var _binding: DialogSubmitDearrowBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoId = it.getString(IntentData.videoId)!!
            currentPosition = it.getLong(IntentData.currentPosition).toFloat() / 1000
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSubmitDearrowBinding.inflate(layoutInflater)

        binding.dearrowTitle.typingEnabled = true
        binding.thumbnailTime.setText(currentPosition.toString())

        lifecycleScope.launch { fetchDeArrowData() }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton(R.string.okay, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
            .apply {
                val positiveButton = getButton(DialogInterface.BUTTON_POSITIVE)
                positiveButton.isEnabled = false

                binding.titleCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    binding.dearrowTitle.isEnabled = isChecked
                    positiveButton.isEnabled = isChecked || binding.thumbnailTimeCheckbox.isChecked
                }
                binding.thumbnailTimeCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    binding.thumbnailTimeInputLayout.isEnabled = isChecked
                    positiveButton.isEnabled = binding.titleCheckbox.isChecked || isChecked
                }

                positiveButton.setOnClickListener {
                    lifecycleScope.launch { submitDeArrow() }
                }
            }
    }

    private suspend fun fetchDeArrowData() {
        val data = try {
            withContext(Dispatchers.IO) {
                RetrofitInstance.api.getDeArrowContent(videoId)
            }.getOrElse(videoId) { return }
        } catch (e: Exception) {
            return
        }

        binding.dearrowTitle.items = data.titles.map { it.title }
    }

    private suspend fun submitDeArrow() {
        val context = requireContext().applicationContext
        requireDialog().hide()

        val userID = PreferenceHelper.getSponsorBlockUserID()
        val userAgent = TextUtils.getUserAgent(context)
        val title = binding.dearrowTitle.text
            .takeIf { it.isNotEmpty() && binding.titleCheckbox.isChecked }
            ?.let { DeArrowSubmitTitle(it) }
        val thumbnail = binding.thumbnailTime.text.toString().parseDurationString()
            ?.takeIf { binding.thumbnailTimeCheckbox.isChecked }
            ?.let { DeArrowSubmitThumbnail(it) }
        val requestBody = DeArrowBody(videoId, userID, userAgent, title, thumbnail)

        try {
            // https://wiki.sponsor.ajay.app/w/API_Docs/DeArrow
            withContext(Dispatchers.IO) {
                RetrofitInstance.externalApi.submitDeArrow(requestBody)
            }
            context.toastFromMainDispatcher(R.string.success)
        } catch (e: Exception) {
            context.toastFromMainDispatcher(e.localizedMessage.orEmpty())
        }

        dialog?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}