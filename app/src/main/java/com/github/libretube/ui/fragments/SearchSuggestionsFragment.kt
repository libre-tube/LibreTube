package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentSearchSuggestionsBinding
import com.github.libretube.extensions.anyChildFocused
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.adapters.SearchHistoryAdapter
import com.github.libretube.ui.adapters.SearchSuggestionsAdapter
import com.github.libretube.ui.extensions.setOnBackPressed
import com.github.libretube.ui.models.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchSuggestionsFragment : Fragment(R.layout.fragment_search_suggestions) {
    private var _binding: FragmentSearchSuggestionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by activityViewModels()
    private val mainActivity get() = activity as MainActivity

    private val historyAdapter = SearchHistoryAdapter(
        onRootClickListener = { historyQuery ->
            runCatching {
                (activity as MainActivity?)?.searchView
            }.getOrNull()?.setQuery(historyQuery, true)
        },
        onArrowClickListener = { historyQuery ->
            runCatching {
                (activity as MainActivity?)?.searchView
            }.getOrNull()?.setQuery(historyQuery, false)
        }
    )
    private val suggestionsAdapter = SearchSuggestionsAdapter(
        onRootClickListener = { suggestion ->
            (activity as MainActivity?)?.searchView?.setQuery(suggestion, true)
        },
        onArrowClickListener = { suggestion ->
            (activity as MainActivity?)?.searchView?.setQuery(suggestion, false)
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.searchQuery.value = arguments?.getString(IntentData.query)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSearchSuggestionsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        viewModel.searchQuery.observe(viewLifecycleOwner) {
            val isEmpty = it.isNullOrEmpty()
            binding.suggestionsRecycler.adapter = if (isEmpty) historyAdapter else suggestionsAdapter

            toggleHistoryVisibility()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchSuggestions.collect { suggestions ->
                    withContext(Dispatchers.Main) {
                        suggestionsAdapter.submitList(suggestions.reversed())
                        toggleHistoryVisibility()
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchHistory.collect { historyList ->
                    withContext(Dispatchers.Main) {
                        historyAdapter.submitList(historyList.map { it.query })
                        toggleHistoryVisibility()
                    }
                }
            }
        }

        setOnBackPressed {
            if (mainActivity.searchView.anyChildFocused()) mainActivity.searchView.clearFocus()
            else findNavController().popBackStack()
        }
    }

    private fun toggleHistoryVisibility() {
        val isEmpty = viewModel.searchQuery.value.isNullOrEmpty()
        val showHistoryEmpty = isEmpty && historyAdapter.currentList.isEmpty()
        binding.historyEmpty.isVisible = showHistoryEmpty
        binding.suggestionsRecycler.isGone = showHistoryEmpty
    }

    override fun onDestroy() {
        super.onDestroy()

        _binding = null
    }
}
