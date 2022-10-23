package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.ui.adapters.PlayingQueueAdapter
import com.github.libretube.util.PlayingQueue
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlayingQueueSheet : BottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionsRecycler.layoutManager = LinearLayoutManager(context)
        binding.optionsRecycler.adapter = PlayingQueueAdapter(
            PlayingQueue.streams
        )
    }
}
