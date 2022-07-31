package com.github.libretube.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.adapters.SearchHistoryAdapter
import com.github.libretube.adapters.SearchSuggestionsAdapter
import com.github.libretube.databinding.FragmentSearchBinding
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import com.github.libretube.util.RetrofitInstance
import retrofit2.HttpException
import java.io.IOException

class SearchFragment() : Fragment() {
    private val TAG = "SearchFragment"
    private lateinit var binding: FragmentSearchBinding

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

        // add the query to the history
        if (query != null) addToHistory(query!!)

        binding.suggestionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        // fetch the search or history
        if (query == null || query == "") showHistory()
        else fetchSuggestions(query!!)
    }

    private fun addToHistory(query: String) {
        val searchHistoryEnabled =
            PreferenceHelper.getBoolean(PreferenceKeys.SEARCH_HISTORY_TOGGLE, true)
        if (searchHistoryEnabled && query != "") {
            PreferenceHelper.saveToSearchHistory(query)
        }
    }

    private fun fetchSuggestions(query: String) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getSuggestions(query)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                // only load the suggestions if the input field didn't get cleared yet
                val suggestionsAdapter =
                    SearchSuggestionsAdapter(
                        response,
                        (activity as MainActivity).searchView
                    )
                runOnUiThread {
                    binding.suggestionsRecycler.adapter = suggestionsAdapter
                }
            }
        }
        run()
    }

    private fun showHistory() {
        val historyList = PreferenceHelper.getSearchHistory()
        if (historyList.isNotEmpty()) {
            binding.suggestionsRecycler.adapter =
                SearchHistoryAdapter(
                    historyList,
                    (activity as MainActivity).searchView
                )
        }
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }

    override fun onDestroy() {
        // remove the backstack entries
        findNavController().popBackStack(R.id.searchFragment, true)
        super.onDestroy()
    }
}
