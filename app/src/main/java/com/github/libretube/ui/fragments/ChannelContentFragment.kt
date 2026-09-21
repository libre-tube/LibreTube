package com.github.libretube.ui.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentChannelContentBinding
import com.github.libretube.extensions.ceilHalf
import com.github.libretube.extensions.parcelable
import com.github.libretube.ui.adapters.SearchResultsAdapter
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.ui.models.ChannelViewModel
import com.github.libretube.ui.models.sources.ChannelTabPagingSource
import kotlinx.coroutines.launch

class ChannelContentFragment : DynamicLayoutManagerFragment(R.layout.fragment_channel_content) {
    private var _binding: FragmentChannelContentBinding? = null
    private val binding get() = _binding!!
    private var recyclerViewState: Parcelable? = null

    private val viewModel: ChannelViewModel by viewModels({ requireParentFragment() })

    override fun setLayoutManagers(gridItems: Int) {
        binding.channelRecView.layoutManager = GridLayoutManager(
            requireContext(),
            gridItems.ceilHalf()
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // manually restore the recyclerview state due to https://github.com/material-components/material-components-android/issues/3473
        binding.channelRecView.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentChannelContentBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        val arguments = requireArguments()
        val tabData = arguments.parcelable<ChannelTab>(IntentData.tabData)!!

        val searchChannelAdapter = SearchResultsAdapter()
        binding.channelRecView.adapter = searchChannelAdapter

        val pagingFlow = Pager(
            PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { ChannelTabPagingSource(tabData) }
        ).flow

        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                pagingFlow.collect {
                    searchChannelAdapter.submitData(it)
                }
            }

            launch {
                searchChannelAdapter.loadStateFlow.collect {
                    if (it.refresh is LoadState.NotLoading) {
                        binding.progressBar.isGone = true
                    }
                }
            }
        }

        binding.channelRecView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}