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

        binding.speed.addOnChangeListener { _, value, _ ->
            onChange(value, binding.pitch.value.round(2))
        }

        binding.pitch.addOnChangeListener { _, value, _ ->
            onChange(binding.speed.value.round(2), value)
        }
    }

    private fun onChange(speed: Float, pitch: Float) {
        player.playbackParameters = PlaybackParameters(
            speed,
            pitch
        )
    }

    fun show(fragmentManager: FragmentManager) = show(
        fragmentManager,
        null
    )
}
