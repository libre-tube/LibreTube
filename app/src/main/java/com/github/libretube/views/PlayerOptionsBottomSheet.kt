package com.github.libretube.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.interfaces.PlayerOptionsInterface
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom Sheet including all the player options
 */
class PlayerOptionsBottomSheet : BottomSheetDialogFragment() {
    lateinit var binding: BottomSheetBinding
    private lateinit var playerOptionsInterface: PlayerOptionsInterface

    /**
     * current values
     */
    var currentPlaybackSpeed: String? = null
    var currentAutoplayMode: String? = null
    var currentRepeatMode: String? = null
    var currentQuality: String? = null
    var currentAspectRatio: String? = null
    var currentCaptions: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // expand the bottom sheet on creation
        dialog!!.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheetInternal =
                d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)!!
            BottomSheetBehavior.from(bottomSheetInternal).state =
                BottomSheetBehavior.STATE_EXPANDED
        }

        binding = BottomSheetBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    fun setOnClickListeners(playerOptionsInterface: PlayerOptionsInterface) {
        this.playerOptionsInterface = playerOptionsInterface
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * update the text if a value is selected
         */

        binding.autoplay.updateText(currentAutoplayMode)

        binding.captions.updateText(currentCaptions)

        binding.playbackSpeed.updateText(currentPlaybackSpeed)

        binding.quality.updateText(currentQuality)

        binding.repeatMode.updateText(currentRepeatMode)

        binding.aspectRatio.updateText(currentAspectRatio)

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

    private fun TextView.updateText(currentValue: String?) {
        if (currentValue == null) return
        this.text = "${this.text} ($currentValue)"
    }
}
