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
        val watchHistoryAdapter = WatchHistoryAdapter(watchHistory, childFragmentManager)
        binding.watchHistoryRecView.adapter = watchHistoryAdapter

        binding.clearHistory.setOnClickListener {
            PreferenceHelper.removePreference(requireContext(), "watch_history")
            watchHistoryAdapter.clear()
        }

        // reverse order
        val linearLayoutManager = LinearLayoutManager(view.context)
        linearLayoutManager.reverseLayout = true
        linearLayoutManager.stackFromEnd = true

        binding.watchHistoryRecView.layoutManager = linearLayoutManager
    }
}
