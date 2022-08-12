package com.github.libretube.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.interfaces.PlayerOptionsInterface
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetBinding
    private lateinit var playerOptionsInterface: PlayerOptionsInterface

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    fun setOnClickListeners(playerOptionsInterface: PlayerOptionsInterface) {
        this.playerOptionsInterface = playerOptionsInterface
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.aspectRatio.setOnClickListener {
            playerOptionsInterface.onAspectRatioClicked()
            this.dismiss()
        }

        binding.quality.setOnClickListener {
            playerOptionsInterface.onQualityClicked()
            this.dismiss()
        }

        binding.playbackSpeed.setOnClickListener {
            playerOptionsInterface.onPlaybackSpeedClicked()
            this.dismiss()
        }

        binding.captions.setOnClickListener {
            playerOptionsInterface.onCaptionClicked()
            this.dismiss()
        }

        binding.autoplay.setOnClickListener {
            playerOptionsInterface.onAutoplayClicked()
            this.dismiss()
        }

        binding.repeatMode.setOnClickListener {
            playerOptionsInterface.onRepeatModeClicked()
            this.dismiss()
        }
    }
}
