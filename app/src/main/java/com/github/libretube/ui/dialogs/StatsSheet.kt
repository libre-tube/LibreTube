package com.github.libretube.ui.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.exoplayer.ExoPlayer
import com.github.libretube.databinding.DialogStatsBinding
import com.github.libretube.ui.sheets.ExpandedBottomSheet
import com.github.libretube.util.TextUtils

class StatsSheet(
    private val player: ExoPlayer,
    private val videoId: String
) : ExpandedBottomSheet() {

    lateinit var binding: DialogStatsBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogStatsBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.videoId.setText(videoId)
        binding.videoInfo.setText(
            "${player.videoFormat?.codecs.orEmpty()} ${
                TextUtils.formatBitrate(
                    player.videoFormat?.bitrate
                )
            }"
        )
        binding.audioInfo.setText(
            "${player.audioFormat?.codecs.orEmpty()} ${
                TextUtils.formatBitrate(
                    player.audioFormat?.bitrate
                )
            }"
        )
        binding.videoQuality.setText(
            "${player.videoFormat?.width}x${player.videoFormat?.height} ${player.videoFormat?.frameRate?.toInt()}fps"
        )
    }
}
