package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.GridLayoutManager
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.PlaybackBottomSheetBinding
import com.github.libretube.extensions.round
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.adapters.SliderLabelsAdapter

class PlaybackOptionsSheet(
    private val player: ExoPlayer
) : ExpandedBottomSheet() {
    private var _binding: PlaybackBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlaybackBottomSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding

        binding.speedShortcuts.layoutManager = GridLayoutManager(context, SUGGESTED_SPEEDS.size)
        binding.pitchShortcuts.layoutManager = GridLayoutManager(context, SUGGESTED_PITCHES.size)

        binding.speedShortcuts.adapter = SliderLabelsAdapter(SUGGESTED_SPEEDS) {
            binding.speed.value = it
        }
        binding.pitchShortcuts.adapter = SliderLabelsAdapter(SUGGESTED_PITCHES) {
            binding.pitch.value = it
        }

        binding.speed.value = player.playbackParameters.speed
        binding.pitch.value = player.playbackParameters.pitch
        PreferenceHelper.getBoolean(PreferenceKeys.SKIP_SILENCE, false).let {
            binding.skipSilence.isChecked = it
        }

        binding.speed.addOnChangeListener { _, _, _ ->
            onChange()
        }

        binding.pitch.addOnChangeListener { _, _, _ ->
            onChange()
        }

        binding.skipSilence.setOnCheckedChangeListener { _, isChecked ->
            player.skipSilenceEnabled = isChecked
            PreferenceHelper.putBoolean(PreferenceKeys.SKIP_SILENCE, isChecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onChange() {
        player.playbackParameters = PlaybackParameters(
            binding.speed.value.round(2),
            binding.pitch.value.round(2)
        )

        if (PreferenceHelper.getBoolean(PreferenceKeys.REMEMBER_PLAYBACK_SPEED, true)) {
            val currentSpeed = player.playbackParameters.speed.toString()
            PreferenceHelper.putString(PreferenceKeys.PLAYBACK_SPEED, currentSpeed)
        }
    }

    companion object {
        private val SUGGESTED_SPEEDS = listOf(0.5f, 1f, 1.25f, 1.5f, 2f)
        private val SUGGESTED_PITCHES = listOf(0.5f, 1f, 1.5f, 2f)
    }
}
