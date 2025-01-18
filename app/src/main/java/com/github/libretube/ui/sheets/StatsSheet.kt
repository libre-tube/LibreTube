package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.View
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogStatsBinding
import com.github.libretube.extensions.parcelable
import com.github.libretube.helpers.ClipboardHelper
import com.github.libretube.obj.VideoStats

class StatsSheet : ExpandedBottomSheet(R.layout.dialog_stats) {
    private lateinit var stats: VideoStats

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stats = arguments?.parcelable(IntentData.videoStats)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = DialogStatsBinding.bind(view)
        binding.videoId.setText(stats.videoId)
        binding.videoIdCopy.setEndIconOnClickListener {
            ClipboardHelper.save(requireContext(), "text", stats.videoId)
        }
        binding.videoInfo.setText(stats.videoInfo)
        binding.audioInfo.setText(stats.audioInfo)
        binding.videoQuality.setText(stats.videoQuality)
    }
}
