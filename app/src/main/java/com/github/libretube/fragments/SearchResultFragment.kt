package com.github.libretube.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.adapters.SearchAdapter
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.databinding.FragmentSearchResultBinding
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.extensions.BaseFragment
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.hideKeyboard
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import retrofit2.HttpException
import java.io.IOException

class SearchResultFragment : BaseFragment() {
    private lateinit var binding: FragmentSearchResultBinding

    private var nextPage: String? = null
    private var query: String = ""

    private lateinit var searchAdapter: SearchAdapter
    private var apiSearchFilter: String = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        query = arguments?.getString("query").toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchResultBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        binding.searchRecycler.viewTreeObserver
            .addOnScrollChangedListener {
                if (!binding.searchRecycler.canScrollVertically(1)) {
                    if (nextPage != null) fetchNextSearchItems()
                }
            }
    }

    private fun fetchSearch() {
        lifecycleScope.launchWhenCreated {
            view?.let { context?.hideKeyboard(it) }
            val response = try {
                RetrofitInstance.api.getSearchResults(query, apiSearchFilter)
            } catch (e: IOException) {
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection $e")
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response")
                return@launchWhenCreated
            }
            runOnUiThread {
                if (response.items?.isNotEmpty() == true) {
                    binding.searchRecycler.layoutManager = LinearLayoutManager(requireContext())
                    searchAdapter = SearchAdapter(response.items, childFragmentManager)
                    binding.searchRecycler.adapter = searchAdapter
                } else {
                    binding.searchContainer.visibility = View.GONE
                    binding.noSearchResult.visibility = View.VISIBLE
                }
            }
            nextPage = response.nextpage
        }
    }

    private fun fetchNextSearchItems() {
        lifecycleScope.launchWhenCreated {
            val response = try {
                RetrofitInstance.api.getSearchResultsNextPage(
                    query,
                    apiSearchFilter,
                    nextPage!!
                )
            } catch (e: IOException) {
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection")
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response," + e.response())
                return@launchWhenCreated
            }
            nextPage = response.nextpage!!
            kotlin.runCatching {
                if (response.items?.isNotEmpty() == true) {
                    searchAdapter.updateItems(response.items.toMutableList())
                }
            }
        }
    }

    private fun addToHistory(query: String) {
        val searchHistoryEnabled =
            PreferenceHelper.getBoolean(PreferenceKeys.SEARCH_HISTORY_TOGGLE, true)
        if (searchHistoryEnabled && query != "") {
            DatabaseHelper.addToSearchHistory(
                SearchHistoryItem(
                    query = query
                )
            )
        }
    }

    override fun onStop() {
        if (findNavController().currentDestination?.id != R.id.searchFragment) {
            // remove the search focus
            (activity as MainActivity)
                .binding.toolbar.menu
                .findItem(R.id.action_search).collapseActionView()
        }
        super.onStop()
    }
}
