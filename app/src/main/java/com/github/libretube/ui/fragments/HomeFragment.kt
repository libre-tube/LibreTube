package com.github.libretube.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys.HOME_TAB_CONTENT
import com.github.libretube.databinding.FragmentHomeBinding
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.activities.SettingsActivity
import com.github.libretube.ui.adapters.PlaylistBookmarkAdapter
import com.github.libretube.ui.adapters.PlaylistsAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.adapters.VideosAdapter.Companion.LayoutMode
import com.github.libretube.ui.models.HomeViewModel
import com.github.libretube.ui.models.SubscriptionsViewModel
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val subscriptionsViewModel: SubscriptionsViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.featuredTV.setOnClickListener {
            findNavController().navigate(R.id.subscriptionsFragment)
        }

        binding.watchingTV.setOnClickListener {
            findNavController().navigate(R.id.watchHistoryFragment)
        }

        binding.trendingTV.setOnClickListener {
            findNavController().navigate(R.id.trendsFragment)
        }

        binding.playlistsTV.setOnClickListener {
            findNavController().navigate(R.id.libraryFragment)
        }

        binding.bookmarksTV.setOnClickListener {
            findNavController().navigate(R.id.libraryFragment)
        }

        binding.refresh.setOnRefreshListener {
            binding.refresh.isRefreshing = true
            fetchHomeFeed()
        }

        binding.refreshButton.setOnClickListener {
            fetchHomeFeed()
        }

        binding.changeInstance.setOnClickListener {
            redirectToIntentSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        observeChanges()

        // Avoid re-fetching when re-entering the screen if it was loaded successfully
        if (homeViewModel.loadedSuccessfully.value == false) {
            fetchHomeFeed()
        }
    }

    private fun observeChanges() {
        with (homeViewModel) {
            trending.observe(viewLifecycleOwner, ::showTrending)
            feed.observe(viewLifecycleOwner, ::showFeed)
            bookmarks.observe(viewLifecycleOwner, ::showBookmarks)
            playlists.observe(viewLifecycleOwner, ::showPlaylists)
            continueWatching.observe(viewLifecycleOwner, ::showContinueWatching)
            isLoading.observe(viewLifecycleOwner, ::updateLoading)
        }
    }

    override fun onPause() {
        super.onPause()
        stopObservingChanges()
    }

    private fun stopObservingChanges() {
        with (homeViewModel) {
            trending.removeObserver(::showTrending)
            feed.removeObserver(::showFeed)
            bookmarks.removeObserver(::showBookmarks)
            playlists.removeObserver(::showPlaylists)
            continueWatching.removeObserver(::showContinueWatching)
            isLoading.removeObserver(::updateLoading)
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
            savedFeed = subscriptionsViewModel.videoFeed.value,
            visibleItems = visibleItems,
            onUnusualLoadTime = ::showChangeInstanceSnackBar
        )
    }

    private fun showTrending(streamItems: List<StreamItem>?) {
        streamItems ?: return

        makeVisible(binding.trendingRV, binding.trendingTV)
        binding.trendingRV.layoutManager = GridLayoutManager(context, 2)
        binding.trendingRV.adapter = VideosAdapter(
            streamItems.toMutableList(),
            forceMode = LayoutMode.TRENDING_ROW
        )
    }

    private fun showFeed(streamItems: List<StreamItem>?) {
        streamItems ?: return

        makeVisible(binding.featuredRV, binding.featuredTV)
        val feedVideos = streamItems.take(20).toMutableList()
        with (binding.featuredRV) {
            layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
            adapter = VideosAdapter(feedVideos, forceMode = LayoutMode.RELATED_COLUMN)
        }
    }

    private fun showBookmarks(bookmarks: List<PlaylistBookmark>?) {
        bookmarks ?: return

        makeVisible(binding.bookmarksTV, binding.bookmarksRV)
        with (binding.bookmarksRV) {
            layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
            adapter = PlaylistBookmarkAdapter(bookmarks.toMutableList())
        }
    }

    private fun showPlaylists(playlists: List<Playlists>?) {
        playlists ?: return

        makeVisible(binding.playlistsRV, binding.playlistsTV)
        binding.playlistsRV.layoutManager = LinearLayoutManager(context)
        binding.playlistsRV.adapter = PlaylistsAdapter(
            playlists.toMutableList(),
            playlistType = PlaylistsHelper.getPrivatePlaylistType()
        )
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
    }

    private fun showContinueWatching(unwatchedVideos: List<StreamItem>?) {
        unwatchedVideos ?: return

        makeVisible(binding.watchingRV, binding.watchingTV)
        binding.watchingRV.layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        binding.watchingRV.adapter = VideosAdapter(
            unwatchedVideos.toMutableList(),
            forceMode = LayoutMode.RELATED_COLUMN
        )
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