package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentSearchResultBinding
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.hideKeyboard
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.adapters.SearchAdapter
import com.github.libretube.util.deArrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchResultFragment : Fragment() {
    private var _binding: FragmentSearchResultBinding? = null
    private val binding get() = _binding!!

    private var nextPage: String? = null
    private var query = ""

    private lateinit var searchAdapter: SearchAdapter
    private var apiSearchFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        query = arguments?.getString(IntentData.query).toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchResultBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchRecycler.layoutManager = LinearLayoutManager(requireContext())

        // add the query to the history
        addToHistory(query)

        // filter options
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, _ ->
            apiSearchFilter = when (
                binding.filterChipGroup.checkedChipId
            ) {
                R.id.chip_all -> "all"
                R.id.chip_videos -> "videos"
                R.id.chip_channels -> "channels"
                R.id.chip_playlists -> "playlists"
                R.id.chip_music_songs -> "music_songs"
                R.id.chip_music_videos -> "music_videos"
                R.id.chip_music_albums -> "music_albums"
                R.id.chip_music_playlists -> "music_playlists"
                else -> throw IllegalArgumentException("Filter out of range")
            }
            fetchSearch()
        }

        fetchSearch()

        binding.searchRecycler.viewTreeObserver.addOnScrollChangedListener {
            if (_binding?.searchRecycler?.canScrollVertically(1) == false &&
                nextPage != null
            ) {
                fetchNextSearchItems()
            }
        }
    }

    private fun fetchSearch() {
        _binding?.progress?.isVisible = true
        _binding?.searchResultsLayout?.isGone = true

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                view?.let { context?.hideKeyboard(it) }
                val response = try {
                    withContext(Dispatchers.IO) {
                        RetrofitInstance.api.getSearchResults(query, apiSearchFilter).apply {
                            items = items.deArrow()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG(), e.toString())
                    context?.toastFromMainDispatcher(R.string.unknown_error)
                    return@repeatOnLifecycle
                }

                val binding = _binding ?: return@repeatOnLifecycle
                searchAdapter = SearchAdapter()
                binding.searchRecycler.adapter = searchAdapter
                searchAdapter.submitList(response.items)
                binding.searchResultsLayout.isVisible = true
                binding.progress.isGone = true
                binding.noSearchResult.isVisible = response.items.isEmpty()

                nextPage = response.nextpage
            }
        }
    }

    private fun fetchNextSearchItems() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val response = try {
                    withContext(Dispatchers.IO) {
                        RetrofitInstance.api.getSearchResultsNextPage(
                            query,
                            apiSearchFilter,
                            nextPage!!
                        ).apply {
                            items = items.deArrow()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG(), e.toString())
                    return@repeatOnLifecycle
                }
                nextPage = response.nextpage
                if (response.items.isNotEmpty()) {
                    searchAdapter.submitList(searchAdapter.currentList + response.items)
                }
            }
        }
    }

    private fun addToHistory(query: String) {
        val searchHistoryEnabled =
            PreferenceHelper.getBoolean(PreferenceKeys.SEARCH_HISTORY_TOGGLE, true)
        if (searchHistoryEnabled && query.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                DatabaseHelper.addToSearchHistory(SearchHistoryItem(query))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
