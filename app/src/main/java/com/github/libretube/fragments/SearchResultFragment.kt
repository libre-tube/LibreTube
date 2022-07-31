package com.github.libretube.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.adapters.SearchAdapter
import com.github.libretube.databinding.FragmentSearchResultBinding
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.hideKeyboard
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.HttpException
import java.io.IOException

class SearchResultFragment : Fragment() {
    private val TAG = "SearchResultFragment"
    private lateinit var binding: FragmentSearchResultBinding

    private lateinit var nextPage: String
    private var query: String = ""

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

        binding.filterMenuImageView.setOnClickListener {
            val filterOptions = arrayOf(
                getString(R.string.all),
                getString(R.string.videos),
                getString(R.string.channels),
                getString(R.string.playlists),
                getString(R.string.music_songs),
                getString(R.string.music_videos),
                getString(R.string.music_albums),
                getString(R.string.music_playlists)
            )

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.choose_filter))
                .setItems(filterOptions) { _, id ->
                    apiSearchFilter = when (id) {
                        0 -> "all"
                        1 -> "videos"
                        2 -> "channels"
                        3 -> "playlists"
                        4 -> "music_songs"
                        5 -> "music_videos"
                        6 -> "music_albums"
                        7 -> "music_playlists"
                        else -> "all"
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
        fetchSearch()

        binding.searchRecycler.viewTreeObserver
            .addOnScrollChangedListener {
                if (!binding.searchRecycler.canScrollVertically(1)) {
                    fetchNextSearchItems()
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
                Log.e(TAG, "IOException, you might not have internet connection $e")
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG, "HttpException, unexpected response")
                return@launchWhenCreated
            }
            runOnUiThread {
                if (response.items?.isNotEmpty() == true) {
                    binding.searchRecycler.layoutManager = LinearLayoutManager(requireContext())
                    binding.searchRecycler.adapter = SearchAdapter(response.items, childFragmentManager)
                }
            }
            nextPage = response.nextpage!!
        }
    }

    private fun fetchNextSearchItems() {
        lifecycleScope.launchWhenCreated {
            val response = try {
                RetrofitInstance.api.getSearchResultsNextPage(
                    query,
                    apiSearchFilter,
                    nextPage
                )
            } catch (e: IOException) {
                println(e)
                Log.e(TAG, "IOException, you might not have internet connection")
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG, "HttpException, unexpected response," + e.response())
                return@launchWhenCreated
            }
            nextPage = response.nextpage!!
            with(binding.searchRecycler.adapter as SearchAdapter) {
                if (response.items?.isNotEmpty() == true) this.updateItems(response.items)
            }
        }
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }
}
