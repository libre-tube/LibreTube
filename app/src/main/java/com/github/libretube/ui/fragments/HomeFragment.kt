package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentHomeBinding
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.adapters.PlaylistBookmarkAdapter
import com.github.libretube.ui.adapters.PlaylistsAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.models.SubscriptionsViewModel
import com.github.libretube.util.deArrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val subscriptionsViewModel: SubscriptionsViewModel by activityViewModels()

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

        fetchHomeFeed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchHomeFeed() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                binding.nothingHere.isGone = true
                val defaultItems = resources.getStringArray(R.array.homeTabItemsValues)
                val visibleItems = PreferenceHelper
                    .getStringSet(PreferenceKeys.HOME_TAB_CONTENT, defaultItems.toSet())
                awaitAll(
                    async { if (visibleItems.contains(TRENDING)) loadTrending() },
                    async { if (visibleItems.contains(WATCHING)) loadVideosToContinueWatching() },
                    async { if (visibleItems.contains(BOOKMARKS)) loadBookmarks() },
                    async { if (visibleItems.contains(FEATURED)) loadFeed() },
                    async { if (visibleItems.contains(PLAYLISTS)) loadPlaylists() }
                )

                val binding = _binding ?: return@repeatOnLifecycle
                // No category is shown because they are either empty or disabled
                if (binding.progress.isVisible) {
                    binding.progress.isGone = true
                    binding.nothingHere.isVisible = true
                }
            }
        }
    }

    private suspend fun loadTrending() {
        val region = LocaleHelper.getTrendingRegion(requireContext())
        val trending = runCatching {
            withContext(Dispatchers.IO) {
                RetrofitInstance.api.getTrending(region).deArrow().take(10)
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return
        val binding = _binding ?: return

        makeVisible(binding.trendingRV, binding.trendingTV)
        binding.trendingRV.layoutManager = GridLayoutManager(context, 2)
        binding.trendingRV.adapter = VideosAdapter(
            trending.toMutableList(),
            forceMode = VideosAdapter.Companion.LayoutMode.TRENDING_ROW
        )
    }

    private suspend fun loadFeed() {
        val savedFeed = subscriptionsViewModel.videoFeed.value
        val feed = if (
            PreferenceHelper.getBoolean(PreferenceKeys.SAVE_FEED, false) &&
            !savedFeed.isNullOrEmpty()
        ) {
            savedFeed
        } else {
            runCatching {
                withContext(Dispatchers.IO) {
                    SubscriptionHelper.getFeed()
                }
            }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return
        }
        var filteredFeed = feed.filter {
            when (PreferenceHelper.getInt(PreferenceKeys.SELECTED_FEED_FILTER, 0)) {
                1 -> !it.isShort
                2 -> it.isShort
                else -> true
            }
        }
        if (PreferenceHelper.getBoolean(PreferenceKeys.HIDE_WATCHED_FROM_FEED, false)) {
            filteredFeed = runBlocking { DatabaseHelper.filterUnwatched(filteredFeed) }
        }
        val binding = _binding ?: return

        makeVisible(binding.featuredRV, binding.featuredTV)
        binding.featuredRV.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.featuredRV.adapter = VideosAdapter(
            filteredFeed.take(20).toMutableList(),
            forceMode = VideosAdapter.Companion.LayoutMode.RELATED_COLUMN
        )
        binding.featuredRV.setHasFixedSize(true)
    }

    private suspend fun loadBookmarks() {
        val bookmarkedPlaylists = withContext(Dispatchers.IO) {
            DatabaseHolder.Database.playlistBookmarkDao().getAll()
        }.takeIf { it.isNotEmpty() } ?: return
        val binding = _binding ?: return

        makeVisible(binding.bookmarksTV, binding.bookmarksRV)
        binding.bookmarksRV.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.bookmarksRV.adapter = PlaylistBookmarkAdapter(
            bookmarkedPlaylists,
            PlaylistBookmarkAdapter.Companion.BookmarkMode.HOME
        )
    }

    private suspend fun loadPlaylists() {
        val playlists = runCatching {
            withContext(Dispatchers.IO) {
                PlaylistsHelper.getPlaylists().take(20)
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return
        val binding = _binding ?: return

        makeVisible(binding.playlistsRV, binding.playlistsTV)
        binding.playlistsRV.layoutManager = LinearLayoutManager(context)
        binding.playlistsRV.adapter = PlaylistsAdapter(
            playlists.toMutableList(),
            PlaylistsHelper.getPrivatePlaylistType()
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

    private suspend fun loadVideosToContinueWatching() {
        if (!PlayerHelper.watchHistoryEnabled) return

        val videos = withContext(Dispatchers.IO) {
            DatabaseHolder.Database.watchHistoryDao().getAll()
        }
        val unwatchedVideos = DatabaseHelper.filterUnwatched(videos.map { it.toStreamItem() })
            .reversed()
            .take(20)
        if (unwatchedVideos.isEmpty()) return
        val binding = _binding ?: return

        makeVisible(binding.watchingRV, binding.watchingTV)
        binding.watchingRV.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.watchingRV.adapter = VideosAdapter(
            unwatchedVideos.toMutableList(),
            forceMode = VideosAdapter.Companion.LayoutMode.RELATED_COLUMN
        )
    }

    private fun makeVisible(vararg views: View) {
        views.forEach {
            it.isVisible = true
        }
        val binding = _binding ?: return
        binding.progress.isGone = true
        binding.scroll.isVisible = true
        binding.refresh.isRefreshing = false
    }

    companion object {
        // The values of the preference entries for the home tab content
        private const val FEATURED = "featured"
        private const val WATCHING = "watching"
        private const val TRENDING = "trending"
        private const val BOOKMARKS = "bookmarks"
        private const val PLAYLISTS = "playlists"
    }
}
