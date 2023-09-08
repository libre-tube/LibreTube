package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogSubmitSegmentBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.Exception
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubmitSegmentDialog : DialogFragment() {
    private var videoId = ""
    private var currentPosition = 0L
    private var duration = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = requireArguments()
        videoId = arguments.getString(IntentData.videoId)!!
        currentPosition = arguments.getLong(IntentData.currentPosition)
        duration = arguments.getLong(IntentData.duration)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogSubmitSegmentBinding.inflate(layoutInflater)

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
            .setNeutralButton(R.string.vote_for_segment) { _, _ ->
                VoteForSegmentDialog().apply {
                    arguments = bundleOf(IntentData.videoId to videoId)
                }.show(parentFragmentManager, null)
            }
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    requireDialog().hide()

                    lifecycleScope.launch {
                        submitSegment(binding)
                        dismiss()
                    }
                }
            }
    }

    private suspend fun submitSegment(binding: DialogSubmitSegmentBinding) {
        val context = requireContext().applicationContext

        val startTime = binding.startTime.text.toString().toFloatOrNull()
        var endTime = binding.endTime.text.toString().toFloatOrNull()
        if (endTime == null || startTime == null || startTime > endTime) {
            context.toastFromMainDispatcher(R.string.sb_invalid_segment)
            return
        }

        if (duration != 0L) {
            // the end time can't be greater than the video duration
            endTime = minOf(endTime, duration.toFloat())
        }

        val categories = resources.getStringArray(R.array.sponsorBlockSegments)
        val category = categories[binding.segmentCategory.selectedItemPosition]
        val userAgent = "${context.packageName}/${BuildConfig.VERSION_NAME}"
        val uuid = PreferenceHelper.getSponsorBlockUserID()

        try {
            withContext(Dispatchers.IO) {
                RetrofitInstance.externalApi
                    .submitSegment(
                        videoId, startTime, endTime, category, userAgent, uuid, duration.toFloat()
                    )
            }
            context.toastFromMainDispatcher(R.string.segment_submitted)
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
            context.toastFromMainDispatcher(e.localizedMessage.orEmpty())
        }
    }
}
