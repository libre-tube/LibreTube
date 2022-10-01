package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.github.libretube.databinding.PlaybackBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlaybackSpeedSheet(
    private val onChange: (speed: Float, pitch: Float) -> Unit
) : BottomSheetDialogFragment() {
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

        binding.speed.addOnChangeListener { _, value, _ ->
            onChange.invoke(value, binding.pitch.value)
        }

        binding.pitch.addOnChangeListener { _, value, _ ->
            onChange.invoke(binding.speed.value, value)
        }
    }

    fun show(fragmentManager: FragmentManager) = show(
        fragmentManager,
        null
    )
}
