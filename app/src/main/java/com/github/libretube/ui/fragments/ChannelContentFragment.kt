package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.github.libretube.R
import com.github.libretube.databinding.FragmentChannelContentBinding
import com.github.libretube.extensions.ceilHalf
import com.github.libretube.ui.adapters.SearchResultsAdapter
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.ui.models.ChannelTabViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChannelContentFragment : DynamicLayoutManagerFragment(R.layout.fragment_channel_content) {
    private var _binding: FragmentChannelContentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChannelTabViewModel by viewModels()
    private val adapter = SearchResultsAdapter()

    override fun setLayoutManagers(gridItems: Int) {
        binding.channelRecView.layoutManager = GridLayoutManager(
            requireContext(),
            gridItems.ceilHalf()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentChannelContentBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
        binding.channelRecView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.pagingData.collectLatest {
                    adapter.submitData(it)
                }
            }

            launch {
                adapter.loadStateFlow.collect {
                    binding.progressBar.isVisible = it.refresh is LoadState.Loading
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}