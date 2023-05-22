package com.github.libretube.ui.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.media3.exoplayer.ExoPlayer
import com.github.libretube.R
import com.github.libretube.databinding.DialogStatsBinding
import com.github.libretube.util.TextUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class StatsDialog(
    private val player: ExoPlayer,
    private val videoId: String,
) : DialogFragment() {

    @SuppressLint("SetTextI18n")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogStatsBinding.inflate(layoutInflater)
        binding.videoId.setText(videoId)
        binding.videoInfo.setText(
            "${player.videoFormat?.codecs.orEmpty()} ${TextUtils.formatBitrate(
                player.videoFormat?.bitrate,
            )}",
        )
        binding.audioInfo.setText(
            "${player.audioFormat?.codecs.orEmpty()} ${TextUtils.formatBitrate(
                player.audioFormat?.bitrate,
            )}",
        )
        binding.videoQuality.setText(
            "${player.videoFormat?.width}x${player.videoFormat?.height} ${player.videoFormat?.frameRate?.toInt()}fps",
        )

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.stats_for_nerds)
            .setView(binding.root)
            .setPositiveButton(R.string.okay, null)
            .create()
    }
}
