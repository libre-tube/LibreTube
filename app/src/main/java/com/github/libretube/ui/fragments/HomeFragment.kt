package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.databinding.FragmentHomeBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.extensions.awaitQuery
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.ui.adapters.PlaylistBookmarkAdapter
import com.github.libretube.ui.adapters.PlaylistsAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.base.BaseFragment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment() {
    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.featuredTV.setOnClickListener {
            findNavController().navigate(R.id.subscriptionsFragment)
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
            lifecycleScope.launch(Dispatchers.IO) {
                fetchHome()
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            fetchHome()
        }
    }

    private suspend fun fetchHome() {
        runOrError {
            val feed = SubscriptionHelper.getFeed().take(20)
            if (feed.isEmpty()) return@runOrError
            runOnUiThread {
                makeVisible(binding.featuredRV, binding.featuredTV)
                binding.featuredRV.layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                binding.featuredRV.adapter = VideosAdapter(
                    feed.toMutableList(),
                    forceMode = VideosAdapter.Companion.ForceMode.HOME
                )
            }
        }

        runOrError {
            val region = LocaleHelper.getTrendingRegion(requireContext())
            val trending = RetrofitInstance.api.getTrending(region).take(10)
            if (trending.isEmpty()) return@runOrError
            runOnUiThread {
                makeVisible(binding.trendingRV, binding.trendingTV)
                binding.trendingRV.layoutManager = GridLayoutManager(context, 2)
                binding.trendingRV.adapter = VideosAdapter(
                    trending.toMutableList(),
                    forceMode = VideosAdapter.Companion.ForceMode.TRENDING
                )
            }
        }

        runOrError {
            val playlists = PlaylistsHelper.getPlaylists().take(20)
            if (playlists.isEmpty()) return@runOrError
            runOnUiThread {
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
                            binding.playlistsRV.visibility = View.GONE
                            binding.playlistsTV.visibility = View.GONE
                        }
                    }
                })
            }
        }

        runOrError {
            val bookmarkedPlaylists = awaitQuery {
                DatabaseHolder.Database.playlistBookmarkDao().getAll()
            }
            if (bookmarkedPlaylists.isEmpty()) return@runOrError
            runOnUiThread {
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
        }
    }

    private fun runOrError(action: suspend () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                action.invoke()
                // Can be caused due to activity launch in front view from PiP mode.
            } catch (e: CancellationException) {
                Log.e("fetching home tab", e.toString())
            } catch (e: Exception) {
                e.localizedMessage?.let { context?.toastFromMainThread(it) }
                Log.e("fetching home tab", e.toString())
            }
        }
    }

    private fun makeVisible(vararg views: View) {
        views.forEach {
            it.visibility = View.VISIBLE
        }
        binding.progress.visibility = View.GONE
        binding.scroll.visibility = View.VISIBLE
        binding.refresh.isRefreshing = false
    }
}
