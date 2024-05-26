package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentSearchSuggestionsBinding
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.extensions.TAG
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.adapters.SearchHistoryAdapter
import com.github.libretube.ui.adapters.SearchSuggestionsAdapter
import com.github.libretube.ui.models.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchSuggestionsFragment : Fragment() {
    private var _binding: FragmentSearchSuggestionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.searchQuery.value = arguments?.getString(IntentData.query)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchSuggestionsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.suggestionsRecycler.layoutManager = LinearLayoutManager(requireContext()).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        // waiting for the query to change
        viewModel.searchQuery.observe(viewLifecycleOwner) {
            showData(it)
        }
    }

    private fun showData(query: String?) {
        // fetch search suggestions if enabled or show the search history
        binding.historyEmpty.isGone = true
        binding.suggestionsRecycler.isVisible = true
        if (query.isNullOrEmpty()) {
            showHistory()
        } else if (PreferenceHelper.getBoolean(PreferenceKeys.SEARCH_SUGGESTIONS, true)) {
            fetchSuggestions(query)
        }
    }

    private fun fetchSuggestions(query: String) {
        lifecycleScope.launch {
            val response = try {
                withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getSuggestions(query)
                }
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                return@launch
            }
            // only load the suggestions if the input field didn't get cleared yet
            val suggestionsAdapter = SearchSuggestionsAdapter(
                response.reversed(),
                (activity as MainActivity).searchView
            )
            if (isAdded && !viewModel.searchQuery.value.isNullOrEmpty()) {
                binding.suggestionsRecycler.adapter = suggestionsAdapter
            }
        }
    }

    private fun showHistory() {
        lifecycleScope.launch {
            val historyList = withContext(Dispatchers.IO) {
                Database.searchHistoryDao().getAll().map { it.query }
            }
            if (historyList.isNotEmpty()) {
                binding.suggestionsRecycler.adapter = SearchHistoryAdapter(
                    historyList,
                    (activity as MainActivity).searchView
                )
            } else {
                binding.suggestionsRecycler.isGone = true
                binding.historyEmpty.isVisible = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        _binding = null
    }
}
