package com.github.libretube.ui.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.GridLayoutManager
import com.github.libretube.R
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentChannelContentBinding
import com.github.libretube.extensions.ceilHalf
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.parcelableArrayList
import com.github.libretube.ui.adapters.SearchResultsAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.ui.extensions.addOnBottomReachedListener
import com.github.libretube.ui.models.sources.ChannelTabPagingSource
import com.github.libretube.util.deArrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelContentFragment : DynamicLayoutManagerFragment(R.layout.fragment_channel_content) {
    private var _binding: FragmentChannelContentBinding? = null
    private val binding get() = _binding!!
    private var recyclerViewState: Parcelable? = null

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
        val channelId = arguments.getString(IntentData.channelId)!!

        val tabData = arguments.parcelable<ChannelTab>(IntentData.tabData)

        if (tabData?.data.isNullOrEmpty()) {
            var nextPage = arguments.getString(IntentData.nextPage)
            var isLoading = false

            val channelAdapter = VideosAdapter(showChannelInfo = false).also {
                it.submitList(arguments.parcelableArrayList<StreamItem>(IntentData.videoList)!!)
            }
            binding.channelRecView.adapter = channelAdapter
            binding.progressBar.isGone = true

            binding.channelRecView.addOnBottomReachedListener {
                if (isLoading || nextPage == null) return@addOnBottomReachedListener

                isLoading = true

                lifecycleScope.launch(Dispatchers.IO) {
                    val resp = try {
                       MediaServiceRepository.instance.getChannelNextPage(channelId, nextPage!!).apply {
                           relatedStreams = relatedStreams.deArrow()
                       }
                    } catch (e: Exception) {
                        return@launch
                    } finally {
                        isLoading = false
                    }

                    nextPage = resp.nextpage
                    withContext(Dispatchers.Main) {
                        channelAdapter.insertItems(resp.relatedStreams)
                    }
                }
            }
        } else {
            val searchChannelAdapter = SearchResultsAdapter()
            binding.channelRecView.adapter = searchChannelAdapter

            val pagingFlow = Pager(
                PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = { ChannelTabPagingSource(tabData!!) }
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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}