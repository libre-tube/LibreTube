package com.github.libretube.ui.fragments

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.libretube.R
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.TrendingCategory
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentTrendsBinding
import com.github.libretube.databinding.FragmentTrendsContentBinding
import com.github.libretube.extensions.serializable
import com.github.libretube.helpers.NavBarHelper
import com.github.libretube.ui.activities.SettingsActivity
import com.github.libretube.ui.adapters.VideoCardsAdapter
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.ui.extensions.setupFragmentAnimation
import com.github.libretube.ui.models.TrendsViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class TrendsFragment : Fragment(R.layout.fragment_trends) {
    private var _binding: FragmentTrendsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTrendsBinding.bind(view)

        val categories = MediaServiceRepository.instance.getTrendingCategories()
        binding.pager.adapter = TrendsAdapter(this, categories)

        if (categories.size <= 1) binding.tabLayout.isGone = true
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            val category = categories[position]
            tab.text = getString(categoryNamesToStringRes[category]!!)
        }.attach()
    }

    companion object {
        val categoryNamesToStringRes = mapOf(
            TrendingCategory.TRENDING to R.string.trends,
            TrendingCategory.GAMING to R.string.gaming,
            TrendingCategory.TRAILERS to R.string.trailers,
            TrendingCategory.PODCASTS to R.string.podcasts,
            TrendingCategory.MUSIC to R.string.music,
            TrendingCategory.LIVE to R.string.live
        )
    }
}

class TrendsAdapter(fragment: Fragment, private val categories: List<TrendingCategory>) :
    FragmentStateAdapter(fragment) {
    override fun createFragment(position: Int): Fragment {
        return TrendsContentFragment().apply {
            arguments = bundleOf(
                IntentData.category to categories[position]
            )
        }
    }

    override fun getItemCount(): Int {
        return categories.size
    }

}

class TrendsContentFragment : DynamicLayoutManagerFragment(R.layout.fragment_trends_content) {
    private var _binding: FragmentTrendsContentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TrendsViewModel by activityViewModels()

    override fun setLayoutManagers(gridItems: Int) {
        _binding?.recview?.layoutManager = GridLayoutManager(context, gridItems)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTrendsContentBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        val category = requireArguments()
            .serializable<TrendingCategory>(IntentData.category)!!

        val adapter = VideoCardsAdapter()
        binding.recview.adapter = adapter
        binding.recview.layoutManager?.onRestoreInstanceState(viewModel.recyclerViewState)

        viewModel.trendingVideos.observe(viewLifecycleOwner) { categoryMap ->
            val videos = categoryMap[category]
            if (videos == null) return@observe

            binding.homeRefresh.isRefreshing = false
            binding.progressBar.isGone = true

            adapter.submitList(videos)

            if (videos.isEmpty()) {
                Snackbar.make(binding.root, R.string.change_region, Snackbar.LENGTH_LONG)
                    .setAction(R.string.settings) {
                        val settingsIntent = Intent(context, SettingsActivity::class.java)
                        startActivity(settingsIntent)
                    }
                    .show()
            }
        }

        binding.homeRefresh.isEnabled = true
        binding.homeRefresh.setOnRefreshListener {
            viewModel.fetchTrending(requireContext(), category)
        }

        binding.recview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                viewModel.recyclerViewState =
                    _binding?.recview?.layoutManager?.onSaveInstanceState()
            }
        })

        lifecycleScope.launch {
            // every time the user navigates to the fragment for the selected category,
            // fetch the trends for the selected category if they're not yet cached
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (viewModel.trendingVideos.value.orEmpty()[category].isNullOrEmpty()) {
                    viewModel.fetchTrending(requireContext(), category)
                }
            }
        }

        if (NavBarHelper.getStartFragmentId(requireContext()) != R.id.trendsFragment) {
            setupFragmentAnimation(binding.root)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // manually restore the recyclerview state due to https://github.com/material-components/material-components-android/issues/3473
        binding.recview.layoutManager?.onRestoreInstanceState(viewModel.recyclerViewState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
