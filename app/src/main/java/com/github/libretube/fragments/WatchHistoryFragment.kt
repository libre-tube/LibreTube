package com.github.libretube.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.adapters.WatchHistoryAdapter
import com.github.libretube.databinding.FragmentWatchHistoryBinding
import com.github.libretube.util.PreferenceHelper

class WatchHistoryFragment : Fragment() {
    private val TAG = "WatchHistoryFragment"
    private lateinit var binding: FragmentWatchHistoryBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWatchHistoryBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val watchHistory = PreferenceHelper.getWatchHistory(requireContext())
        binding.watchHistoryRecView.adapter = WatchHistoryAdapter(watchHistory)
        binding.watchHistoryRecView.layoutManager = LinearLayoutManager(view.context)
    }
}
