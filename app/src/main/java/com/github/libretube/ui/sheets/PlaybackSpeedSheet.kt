package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.github.libretube.databinding.PlaybackBottomSheetBinding
import com.github.libretube.extensions.round
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player

class PlaybackSpeedSheet(
    private val player: Player
) : ExpandedBottomSheet() {
    private lateinit var binding: PlaybackBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PlaybackBottomSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.speed.value = player.playbackParameters.speed
        binding.pitch.value = player.playbackParameters.pitch

        binding.speed.addOnChangeListener { _, _, _ ->
            onChange()
        }

        binding.pitch.addOnChangeListener { _, _, _ ->
            onChange()
        }

        binding.resetSpeed.setOnClickListener {
            binding.speed.value = 1f
            onChange()
        }

        binding.resetPitch.setOnClickListener {
            binding.pitch.value = 1f
            onChange()
        }
    }

    private fun onChange() {
        player.playbackParameters = PlaybackParameters(
            binding.speed.value.round(2),
            binding.pitch.value.round(2)
        )
    }

    fun show(fragmentManager: FragmentManager) = show(
        fragmentManager,
        null
    )
}
