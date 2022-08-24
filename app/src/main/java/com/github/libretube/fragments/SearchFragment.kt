package com.github.libretube.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.activities.MainActivity
import com.github.libretube.adapters.SearchHistoryAdapter
import com.github.libretube.adapters.SearchSuggestionsAdapter
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.databinding.FragmentSearchBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.extensions.BaseFragment
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.await
import com.github.libretube.models.SearchViewModel
import retrofit2.HttpException
import java.io.IOException

class SearchFragment() : BaseFragment() {
    private lateinit var binding: FragmentSearchBinding
    private val viewModel: SearchViewModel by activityViewModels()

    private var query: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        query = arguments?.getString("query")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.suggestionsRecycler.layoutManager = LinearLayoutManager(requireContext())

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
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getSuggestions(query)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG(), "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG(), "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                // only load the suggestions if the input field didn't get cleared yet
                val suggestionsAdapter =
                    SearchSuggestionsAdapter(
                        response,
                        (activity as MainActivity).searchView
                    )
                runOnUiThread {
                    if (viewModel.searchQuery.value != "") {
                        binding.suggestionsRecycler.adapter = suggestionsAdapter
                    }
                }
            }
        }
        run()
    }

    private fun showHistory() {
        var historyList = listOf<String>()
        Thread {
            val history = DatabaseHolder.db.searchHistoryDao().getAll()
            historyList = history.map { it.query }
        }.await()
        if (historyList.isNotEmpty()) {
            binding.suggestionsRecycler.adapter =
                SearchHistoryAdapter(
                    historyList,
                    (activity as MainActivity).searchView
                )
        } else {
            binding.suggestionsRecycler.visibility = View.GONE
            binding.historyEmpty.visibility = View.VISIBLE
        }
    }
}
