package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Segment
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogVoteSegmentBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

class VoteForSegmentDialog : DialogFragment() {
    private lateinit var videoId: String
    private var _binding: DialogVoteSegmentBinding? = null
    private var segments: List<Segment> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoId = arguments?.getString(IntentData.videoId, "")!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogVoteSegmentBinding.inflate(layoutInflater)

        lifecycleScope.launch(Dispatchers.IO) {
            fetchSegments()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.vote_for_segment)
            .setView(_binding?.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.okay, null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val binding = _binding ?: return@setOnClickListener

                    val segmentID = segments.getOrNull(binding.segmentsDropdown.selectedItemPosition)
                        ?.uuid ?: return@setOnClickListener

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
                dismiss()
                Toast.makeText(context, R.string.no_segments_found, Toast.LENGTH_SHORT).show()
                return@withContext
            }

            binding.segmentsDropdown.items = segments.map {
                "${it.category} (${
                    DateUtils.formatElapsedTime(it.segmentStartAndEnd.first.toLong())
                } - ${
                    DateUtils.formatElapsedTime(it.segmentStartAndEnd.second.toLong())
                })"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
