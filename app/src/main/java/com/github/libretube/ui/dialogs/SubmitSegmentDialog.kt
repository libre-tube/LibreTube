package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Segment
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogSubmitSegmentBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.util.TextUtils
import com.github.libretube.util.TextUtils.parseDurationString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

class SubmitSegmentDialog : DialogFragment() {
    private var videoId: String = ""
    private var currentPosition: Long = 0
    private var duration: Long? = null
    private var segments: List<Segment> = emptyList()

    private var _binding: DialogSubmitSegmentBinding? = null
    private val binding: DialogSubmitSegmentBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoId = it.getString(IntentData.videoId)!!
            currentPosition = it.getLong(IntentData.currentPosition)
            duration = it.getLong(IntentData.duration)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSubmitSegmentBinding.inflate(layoutInflater)

        binding.createSegment.setOnClickListener {
            lifecycleScope.launch { createSegment() }
        }
        binding.voteSegment.setOnClickListener {
            lifecycleScope.launch { voteForSegment() }
        }

        binding.startTime.setText(DateUtils.formatElapsedTime(((currentPosition.toFloat() / 1000).toLong())))

        binding.segmentCategory.items = resources.getStringArray(R.array.sponsorBlockSegmentNames).toList()

        lifecycleScope.launch(Dispatchers.IO) {
            fetchSegments()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private suspend fun createSegment() {
        val context = requireContext().applicationContext
        val binding = _binding ?: return

        requireDialog().hide()

        val startTime = binding.startTime.text.toString().parseDurationString()
        var endTime = binding.endTime.text.toString().parseDurationString()

        if (endTime == null || startTime == null || startTime > endTime) {
            context.toastFromMainDispatcher(R.string.sb_invalid_segment)
            return
        }

        if (duration != null) {
            // the end time can't be greater than the video duration
            endTime = minOf(endTime, duration!!.toFloat())
        }

        val categories = resources.getStringArray(R.array.sponsorBlockSegments)
        val category = categories[binding.segmentCategory.selectedItemPosition]
        val userAgent = TextUtils.getUserAgent(context)
        val uuid = PreferenceHelper.getSponsorBlockUserID()
        val duration = duration?.let { it.toFloat() / 1000 }

        try {
            withContext(Dispatchers.IO) {
                RetrofitInstance.externalApi
                    .submitSegment(videoId, uuid, userAgent, startTime, endTime, category, duration)
            }
            context.toastFromMainDispatcher(R.string.segment_submitted)
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
            context.toastFromMainDispatcher(e.localizedMessage.orEmpty())
        }

        requireDialog().dismiss()
    }

    private suspend fun voteForSegment() {
        val binding = _binding ?: return
        val context = requireContext().applicationContext

        val segmentID = segments.getOrNull(binding.segmentsDropdown.selectedItemPosition)
            ?.uuid ?: return

        // see https://wiki.sponsor.ajay.app/w/API_Docs#POST_/api/voteOnSponsorTime
        val score = when {
            binding.upvote.isChecked -> 1
            binding.downvote.isChecked -> 0
            else -> 20
        }

        dialog?.hide()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                RetrofitInstance.externalApi.voteOnSponsorTime(
                    uuid = segmentID,
                    userID = PreferenceHelper.getSponsorBlockUserID(),
                    score = score
                )
                context.toastFromMainDispatcher(R.string.success)
            } catch (e: Exception) {
                context.toastFromMainDispatcher(e.localizedMessage.orEmpty())
            }
            withContext(Dispatchers.Main) { dialog?.dismiss() }
        }
    }

    private suspend fun fetchSegments() {
        val categories = resources.getStringArray(R.array.sponsorBlockSegments).toList()
        segments = try {
            RetrofitInstance.api.getSegments(videoId, JsonHelper.json.encodeToString(categories)).segments
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
            return
        }

        withContext(Dispatchers.Main) {
            val binding = _binding ?: return@withContext

            if (segments.isEmpty()) {
                binding.voteSegmentContainer.isGone = true
                Toast.makeText(context, R.string.no_segments_found, Toast.LENGTH_SHORT).show()
                return@withContext
            }

            binding.segmentsDropdown.items = segments.map {
                val (start, end) = it.segmentStartAndEnd
                val (startStr, endStr) = DateUtils.formatElapsedTime(start.toLong()) to
                        DateUtils.formatElapsedTime(end.toLong())
                "${it.category} ($startStr - $endStr)"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
