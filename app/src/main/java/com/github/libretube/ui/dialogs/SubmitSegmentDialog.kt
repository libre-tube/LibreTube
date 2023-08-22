package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.databinding.DialogSubmitSegmentBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.Exception
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubmitSegmentDialog(
    private val videoId: String,
    private val currentPosition: Long,
    private val duration: Long?
) : DialogFragment() {
    private var _binding: DialogSubmitSegmentBinding? = null
    private val binding = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSubmitSegmentBinding.inflate(layoutInflater)

        binding.startTime.setText((currentPosition.toFloat() / 1000).toString())

        val categoryNames = resources.getStringArray(R.array.sponsorBlockSegmentNames)
        binding.segmentCategory.adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            categoryNames
        )

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.sb_create_segment))
            .setView(binding.root)
            .setPositiveButton(R.string.okay, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    requireDialog().hide()

                    lifecycleScope.launch {
                        submitSegment()
                        dismiss()
                    }
                }
            }
    }

    private suspend fun submitSegment() {
        val context = requireContext().applicationContext

        val startTime = binding.startTime.text.toString().toFloatOrNull()
        var endTime = binding.endTime.text.toString().toFloatOrNull()
        if (endTime == null || startTime == null || startTime > endTime) {
            context.toastFromMainDispatcher(R.string.sb_invalid_segment)
            return
        }

        if (duration != null) {
            // the end time can't be greater than the video duration
            endTime = minOf(endTime, duration.toFloat())
        }

        val categories = resources.getStringArray(R.array.sponsorBlockSegments)
        val category = categories[binding.segmentCategory.selectedItemPosition]
        val userAgent = "${context.packageName}/${BuildConfig.VERSION_NAME}"
        val uuid = PreferenceHelper.getSponsorBlockUserID()
        val duration = duration?.let { it.toFloat() / 1000 }

        try {
            withContext(Dispatchers.IO) {
                RetrofitInstance.externalApi
                    .submitSegment(videoId, startTime, endTime, category, userAgent, uuid, duration)
            }
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
            context.toastFromMainDispatcher(e.localizedMessage.orEmpty())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
