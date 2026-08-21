package com.github.libretube.ui.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.TrendingCategory
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentTrendsContentBinding
import com.github.libretube.extensions.serializable
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.adapters.VideoCardsAdapter
import com.github.libretube.ui.base.BaseBindingFragment
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.ui.models.TrendsViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TrendsContentFragment : DynamicLayoutManagerFragment(R.layout.fragment_trends_content) {
    private var _binding: FragmentTrendsContentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TrendsViewModel by activityViewModels()

    private var _category: TrendingCategory? = null
    private val category get() = _category!!

    override fun setLayoutManagers(gridItems: Int) {
        _binding?.recview?.layoutManager = GridLayoutManager(context, gridItems)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTrendsContentBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        _category = requireArguments()
            .serializable<TrendingCategory>(IntentData.category)!!

        val adapter = VideoCardsAdapter()
        binding.recview.adapter = adapter
        binding.recview.layoutManager?.onRestoreInstanceState(viewModel.recyclerViewState)

        viewModel.trendingVideos.observe(viewLifecycleOwner) { categoryMap ->
            val videos = categoryMap[category]
            if (videos == null) return@observe

            toggleLoadingIndicator(false)
            adapter.submitList(videos.streams)

            val trendingRegion = PreferenceHelper.getTrendingRegion(requireContext())
            if (videos.streams.isEmpty() && (videos.region == trendingRegion)) {
                Snackbar.make(
                    requireParentFragment().requireView(),
                    R.string.change_region,
                    Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.change) {
                        TrendsFragment.showChangeRegionDialog(requireContext()) {
                            refreshTrending()
                        }
                    }
                    .show()
            }
        }

        binding.homeRefresh.isEnabled = true
        binding.homeRefresh.setOnRefreshListener {
            refreshTrending()
        }

        binding.recview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                viewModel.recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState()
            }
        })

        viewModel.fetchTrending(requireContext(), category)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val trendingRegion = PreferenceHelper.getTrendingRegion(requireContext())
                val trendingVideos = viewModel.trendingVideos.value.orEmpty()[category]
                if (trendingVideos == null || (trendingVideos.region != trendingRegion)) {
                    refreshTrending()
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.recview.layoutManager?.onRestoreInstanceState(viewModel.recyclerViewState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun toggleLoadingIndicator(show: Boolean) {
        binding.recview.alpha = if (show) 0.3f else 1.0f
        binding.progressBar.isGone = !show
        if (!show) binding.homeRefresh.isRefreshing = false
    }

    fun refreshTrending() {
        toggleLoadingIndicator(true)
        viewModel.fetchTrending(requireContext(), category)
    }
}
