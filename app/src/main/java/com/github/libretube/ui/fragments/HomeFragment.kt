package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.FragmentHomeBinding
import com.github.libretube.ui.adapters.PlaylistsAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.base.BaseFragment
import com.github.libretube.ui.models.HomeModel
import com.github.libretube.util.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment() {
    private lateinit var binding: FragmentHomeBinding
    private val viewModel: HomeModel by activityViewModels()

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
            viewModel.fetchHome(requireContext(), LocaleHelper.getTrendingRegion(requireContext()))
        }

        viewModel.feed.observe(viewLifecycleOwner) {
            binding.featuredTV.visibility = View.VISIBLE
            binding.featuredRV.visibility = View.VISIBLE
            binding.progress.visibility = View.GONE
            binding.featuredRV.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            binding.featuredRV.adapter = VideosAdapter(
                it.toMutableList(),
                childFragmentManager,
                forceMode = VideosAdapter.Companion.ForceMode.RELATED
            )
        }

        viewModel.trending.observe(viewLifecycleOwner) {
            if (it.isEmpty()) return@observe
            binding.trendingTV.visibility = View.VISIBLE
            binding.trendingRV.visibility = View.VISIBLE
            binding.progress.visibility = View.GONE
            binding.trendingRV.layoutManager = GridLayoutManager(context, 2)
            binding.trendingRV.adapter = VideosAdapter(
                it.toMutableList(),
                childFragmentManager,
                forceMode = VideosAdapter.Companion.ForceMode.TRENDING
            )
        }

        viewModel.playlists.observe(viewLifecycleOwner) {
            if (it.isEmpty()) return@observe
            binding.playlistsRV.visibility = View.VISIBLE
            binding.playlistsTV.visibility = View.VISIBLE
            binding.progress.visibility = View.GONE
            binding.playlistsRV.layoutManager = LinearLayoutManager(context)
            binding.playlistsRV.adapter = PlaylistsAdapter(it.toMutableList(), childFragmentManager)
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
