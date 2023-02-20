package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.databinding.FragmentSearchBinding
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.extensions.TAG
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.adapters.SearchHistoryAdapter
import com.github.libretube.ui.adapters.SearchSuggestionsAdapter
import com.github.libretube.ui.models.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment(R.layout.fragment_search) {
    private val binding by viewBinding(FragmentSearchBinding::bind)

    private val viewModel: SearchViewModel by activityViewModels()

    private var query: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        query = arguments?.getString("query")
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
        // fetch the search or history
        binding.historyEmpty.visibility = View.GONE
        binding.suggestionsRecycler.visibility = View.VISIBLE
        if (query == null || query == "") {
            showHistory()
        } else {
            fetchSuggestions(query)
        }
    }

    private fun fetchSuggestions(query: String) {
        lifecycleScope.launchWhenCreated {
            val response = try {
                RetrofitInstance.api.getSuggestions(query)
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                return@launchWhenCreated
            }
            // only load the suggestions if the input field didn't get cleared yet
            val suggestionsAdapter = SearchSuggestionsAdapter(
                response.reversed(),
                (activity as MainActivity).searchView
            )
            if (isAdded && viewModel.searchQuery.value != "") {
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
                binding.suggestionsRecycler.visibility = View.GONE
                binding.historyEmpty.visibility = View.VISIBLE
            }
        }
    }
}
