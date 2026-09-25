package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.github.libretube.R
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentChannelContentBinding
import com.github.libretube.extensions.ceilHalf
import com.github.libretube.extensions.parcelable
import com.github.libretube.ui.adapters.SearchResultsAdapter
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.ui.models.ChannelViewModel
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChannelContentFragment : DynamicLayoutManagerFragment(R.layout.fragment_channel_content) {
    private var _binding: FragmentChannelContentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChannelViewModel by viewModels({ requireParentFragment() })
    private val adapter = SearchResultsAdapter()
    private lateinit var tab: ChannelTab

    private val sortChipsNames = mapOf(
        "latest" to R.string.channel_sort_latest,
        "popular" to R.string.channel_sort_popular,
        "oldest" to R.string.channel_sort_oldest,
    )

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

        tab = requireArguments().parcelable<ChannelTab>(IntentData.tabData)!!
        var resetScroll = false

        binding.sortingChips.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = group.findViewById<Chip>(checkedIds.first())
            val nextPage = chip.tag as? String ?: return@setOnCheckedStateChangeListener
            viewModel.selectSort(tab, nextPage)
            resetScroll = true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tabData(tab).collectLatest {
                        adapter.submitData(it)
                    }
                }

                launch {
                    adapter.loadStateFlow.collect {
                        binding.progressBar.isVisible = it.refresh is LoadState.Loading

                        if (it.refresh is LoadState.NotLoading && resetScroll) {
                            binding.channelRecView.scrollToPosition(0)
                            resetScroll = false
                        }
                    }
                }
            }
        }

        viewModel.sortingOptions(tab).observe(viewLifecycleOwner) {
            setSortingChips(it)
        }
    }

    fun setSortingChips(sortingChips: Map<String, String>?) {
        binding.sortingChipsContainer.isVisible = sortingChips?.isNotEmpty() ?: false
        binding.sortingChips.removeAllViews()

        sortingChips?.mapKeys { sortChipsNames[it.key] }
            ?.entries
            ?.filter { it.key != null }
            ?.sortedBy { sortChipsNames.values.indexOf(it.key) }
            ?.forEachIndexed { index, (stringId, nextPage) ->
                val chip = layoutInflater.inflate(
                    R.layout.filter_outline_chip,
                    binding.sortingChips,
                    false
                ) as Chip
                chip.apply {
                    id = View.generateViewId()
                    isCheckable = true
                    text = getString(stringId!!)
                    tag = nextPage
                }

                binding.sortingChips.addView(chip)
                if (index == 0) {
                    binding.sortingChips.check(chip.id)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}