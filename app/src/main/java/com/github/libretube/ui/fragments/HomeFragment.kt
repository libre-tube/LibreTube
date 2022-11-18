package com.github.libretube.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.databinding.FragmentHomeBinding
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.ui.adapters.PlaylistsAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.base.BaseFragment
import com.github.libretube.ui.extensions.withMaxSize
import com.github.libretube.util.LocaleHelper
import com.github.libretube.util.PreferenceHelper
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

        lifecycleScope.launch(Dispatchers.IO) {
            fetchHome(requireContext(), LocaleHelper.getTrendingRegion(requireContext()))
        }
    }

    private suspend fun fetchHome(context: Context, trendingRegion: String) {
        val token = PreferenceHelper.getToken()
        val appContext = context.applicationContext
        runOrError(appContext) {
            val feed = SubscriptionHelper.getFeed().withMaxSize(20)
            runOnUiThread {
                makeVisible(binding.featuredRV, binding.featuredTV)
                binding.featuredRV.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                binding.featuredRV.adapter = VideosAdapter(
                    feed.toMutableList(),
                    childFragmentManager,
                    forceMode = VideosAdapter.Companion.ForceMode.RELATED
                )
            }
        }

        runOrError(appContext) {
            val trending = RetrofitInstance.api.getTrending(trendingRegion).withMaxSize(10)
            runOnUiThread {
                makeVisible(binding.trendingRV, binding.trendingTV)
                binding.trendingRV.layoutManager = GridLayoutManager(context, 2)
                binding.trendingRV.adapter = VideosAdapter(
                    trending.toMutableList(),
                    childFragmentManager,
                    forceMode = VideosAdapter.Companion.ForceMode.TRENDING
                )
            }
        }

        runOrError(appContext) {
            val playlists = RetrofitInstance.authApi.getUserPlaylists(token).withMaxSize(20)
            runOnUiThread {
                makeVisible(binding.playlistsRV, binding.playlistsTV)
                binding.playlistsRV.layoutManager = LinearLayoutManager(context)
                binding.playlistsRV.adapter = PlaylistsAdapter(playlists.toMutableList(), childFragmentManager)
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
    }

    private fun runOrError(context: Context, action: suspend () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                action.invoke()
            } catch (e: Exception) {
                e.localizedMessage?.let { context.toastFromMainThread(it) }
            }
        }
    }

    private fun makeVisible(vararg views: View) {
        views.forEach {
            it.visibility = View.VISIBLE
        }
        binding.progress.visibility = View.GONE
    }
}
