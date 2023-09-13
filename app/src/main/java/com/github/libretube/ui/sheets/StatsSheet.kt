package com.github.libretube.ui.sheets

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogStatsBinding
import com.github.libretube.extensions.parcelable
import com.github.libretube.obj.VideoStats

class StatsSheet : ExpandedBottomSheet() {
    private var _binding: DialogStatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var stats: VideoStats

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogStatsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stats = arguments?.parcelable(IntentData.videoStats)!!
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding

        binding.videoId.setText(stats.videoId)
        binding.videoInfo.setText(stats.videoInfo)
        binding.audioInfo.setText(stats.audioInfo)
        binding.videoQuality.setText(stats.videoQuality)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
