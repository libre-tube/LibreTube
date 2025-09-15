package com.github.libretube.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.TrendingCategory
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.constants.PreferenceKeys.HOME_TAB_CONTENT
import com.github.libretube.databinding.FragmentHomeBinding
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.helpers.NavBarHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.activities.SettingsActivity
import com.github.libretube.ui.adapters.CarouselPlaylist
import com.github.libretube.ui.adapters.CarouselPlaylistAdapter
import com.github.libretube.ui.adapters.VideoCardsAdapter
import com.github.libretube.ui.extensions.setupFragmentAnimation
import com.github.libretube.ui.models.HomeViewModel
import com.github.libretube.ui.models.SubscriptionsViewModel
import com.github.libretube.ui.models.TrendsViewModel
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.carousel.UncontainedCarouselStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.runBlocking


class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val subscriptionsViewModel: SubscriptionsViewModel by activityViewModels()
    private val trendsViewModel: TrendsViewModel by activityViewModels()

    private val trendingAdapter = VideoCardsAdapter()
    private val feedAdapter = VideoCardsAdapter(columnWidthDp = 250f)
    private val watchingAdapter = VideoCardsAdapter(columnWidthDp = 250f)
    private val bookmarkAdapter = CarouselPlaylistAdapter()
    private val playlistAdapter = CarouselPlaylistAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.bookmarksRV.layoutManager = CarouselLayoutManager(UncontainedCarouselStrategy())
        binding.playlistsRV.layoutManager = CarouselLayoutManager(UncontainedCarouselStrategy())

        val bookmarksSnapHelper = CarouselSnapHelper()
        bookmarksSnapHelper.attachToRecyclerView(binding.bookmarksRV)

        val playlistsSnapHelper = CarouselSnapHelper()
        playlistsSnapHelper.attachToRecyclerView(binding.playlistsRV)

        binding.trendingRV.adapter = trendingAdapter
        binding.featuredRV.adapter = feedAdapter
        binding.bookmarksRV.adapter = bookmarkAdapter
        binding.playlistsRV.adapter = playlistAdapter
        binding.playlistsRV.adapter?.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                if (itemCount == 0) {
                    binding.playlistsRV.isGone = true
                    binding.playlistsTV.isGone = true
                }
            }
        })
        binding.watchingRV.adapter = watchingAdapter

        with(homeViewModel) {
            trending.observe(viewLifecycleOwner, ::showTrending)
            feed.observe(viewLifecycleOwner, ::showFeed)
            bookmarks.observe(viewLifecycleOwner, ::showBookmarks)
            playlists.observe(viewLifecycleOwner, ::showPlaylists)
            continueWatching.observe(viewLifecycleOwner, ::showContinueWatching)
            isLoading.observe(viewLifecycleOwner, ::updateLoading)
        }

        binding.featuredTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_subscriptionsFragment)
        }

        binding.watchingTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_watchHistoryFragment)
        }

        binding.trendingTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_trendsFragment)
        }

        binding.playlistsTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_libraryFragment)
        }

        binding.bookmarksTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_libraryFragment)
        }

        binding.refresh.setOnRefreshListener {
            binding.refresh.isRefreshing = true
            fetchHomeFeed()
        }

        binding.trendingRegion.setOnClickListener {
            val currentRegionPref = PreferenceHelper.getTrendingRegion(requireContext())

            val countries = LocaleHelper.getAvailableCountries()
            var selected = countries.indexOfFirst { it.code == currentRegionPref }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.region)
                .setSingleChoiceItems(
                    countries.map { it.name }.toTypedArray(),
                    selected
                ) { _, checked ->
                    selected = checked
                }
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.okay) { _, _ ->
                    PreferenceHelper.putString(PreferenceKeys.REGION, countries[selected].code)
                    fetchHomeFeed()
                }
                .show()
        }

        val trendingCategories = MediaServiceRepository.instance.getTrendingCategories()
        binding.trendingCategory.isVisible = trendingCategories.size > 1
        binding.trendingCategory.setOnClickListener {
            val currentTrendingCategoryPref = PreferenceHelper.getString(
                PreferenceKeys.TRENDING_CATEGORY,
                TrendingCategory.TRENDING.name
            ).let { TrendingCategory.valueOf(it) }

            val categories = trendingCategories.map { category ->
                category to getString(TrendsFragment.categoryNamesToStringRes[category]!!)
            }
            var selected = TrendingCategory.entries.indexOf(currentTrendingCategoryPref)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.category)
                .setSingleChoiceItems(categories.map { it.second }.toTypedArray(), selected) { _, checked ->
                    selected = checked
                }
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.okay) { _, _ ->
                    PreferenceHelper.putString(PreferenceKeys.TRENDING_CATEGORY, TrendingCategory.entries[selected].name)
                    fetchHomeFeed()
                }
                .show()
        }

        binding.refreshButton.setOnClickListener {
            fetchHomeFeed()
        }

        binding.changeInstance.setOnClickListener {
            redirectToIntentSettings()
        }

        if (NavBarHelper.getStartFragmentId(requireContext()) != R.id.homeFragment) {
            setupFragmentAnimation(binding.root)
        }
    }

    override fun onResume() {
        super.onResume()

        // Avoid re-fetching when re-entering the screen if it was loaded successfully
        if (homeViewModel.loadedSuccessfully.value == false) {
            fetchHomeFeed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchHomeFeed() {
        binding.nothingHere.isGone = true
        val defaultItems = resources.getStringArray(R.array.homeTabItemsValues)
        val visibleItems = PreferenceHelper.getStringSet(HOME_TAB_CONTENT, defaultItems.toSet())

        homeViewModel.loadHomeFeed(
            context = requireContext(),
            subscriptionsViewModel = subscriptionsViewModel,
            visibleItems = visibleItems,
            onUnusualLoadTime = ::showChangeInstanceSnackBar
        )
    }

    private fun showTrending(streamItems: List<StreamItem>?) {
        if (streamItems == null) return

        // cache the loaded trends in the [TrendsViewModel] so that the trends don't need to be
        // reloaded there
        trendsViewModel.setStreamsForCategory(TrendingCategory.TRENDING, streamItems)

        makeVisible(binding.trendingRV, binding.trendingTV)
        trendingAdapter.submitList(streamItems.take(10))
    }

    private fun showFeed(streamItems: List<StreamItem>?) {
        if (streamItems == null) return

        makeVisible(binding.featuredRV, binding.featuredTV)
        val hideWatched = PreferenceHelper.getBoolean(PreferenceKeys.HIDE_WATCHED_FROM_FEED, false)
        val feedVideos = streamItems
            .let { runBlocking { DatabaseHelper.filterByStatusAndWatchPosition(it, hideWatched) } }
            .take(20)

        feedAdapter.submitList(feedVideos)
    }

    private fun showBookmarks(bookmarks: List<PlaylistBookmark>?) {
        if (bookmarks == null) return

        makeVisible(binding.bookmarksTV, binding.bookmarksRV)
        bookmarkAdapter.submitList(bookmarks.map { bookmark ->
            CarouselPlaylist(
                id = bookmark.playlistId,
                title = bookmark.playlistName,
                thumbnail = bookmark.thumbnailUrl
            )
        })
    }

    private fun showPlaylists(playlists: List<Playlists>?) {
        if (playlists == null) return

        makeVisible(binding.playlistsRV, binding.playlistsTV)
        playlistAdapter.submitList(playlists.map { playlist ->
            CarouselPlaylist(
                id = playlist.id!!,
                thumbnail = playlist.thumbnail,
                title = playlist.name
            )
        })
    }

    private fun showContinueWatching(unwatchedVideos: List<StreamItem>?) {
        if (unwatchedVideos == null) return

        makeVisible(binding.watchingRV, binding.watchingTV)
        watchingAdapter.submitList(unwatchedVideos)
    }

    private fun updateLoading(isLoading: Boolean) {
        if (isLoading) {
            showLoading()
        } else {
            hideLoading()
        }
    }

    private fun showLoading() {
        binding.progress.isVisible = !binding.refresh.isRefreshing
        binding.nothingHere.isVisible = false
    }

    private fun hideLoading() {
        binding.progress.isVisible = false
        binding.refresh.isRefreshing = false

        val hasContent = homeViewModel.loadedSuccessfully.value == true
        if (hasContent) {
            showContent()
        } else {
            showNothingHere()
        }
    }

    private fun showNothingHere() {
        binding.nothingHere.isVisible = true
        binding.scroll.isVisible = false
    }

    private fun showContent() {
        binding.nothingHere.isVisible = false
        binding.scroll.isVisible = true
    }

    private fun showChangeInstanceSnackBar() {
        val root = _binding?.root ?: return
        Snackbar
            .make(root, R.string.suggest_change_instance, Snackbar.LENGTH_LONG)
            .apply {
                setAction(R.string.change) {
                    redirectToIntentSettings()
                }
                show()
            }
    }

    private fun redirectToIntentSettings() {
        val settingsIntent = Intent(context, SettingsActivity::class.java).apply {
            putExtra(SettingsActivity.REDIRECT_KEY, SettingsActivity.REDIRECT_TO_INTENT_SETTINGS)
        }
        startActivity(settingsIntent)
    }

    private fun makeVisible(vararg views: View) {
        views.forEach { it.isVisible = true }
    }
}
