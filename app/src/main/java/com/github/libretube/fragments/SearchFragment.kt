package com.github.libretube.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView.GONE
import android.widget.TextView.OnEditorActionListener
import android.widget.TextView.VISIBLE
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.adapters.SearchAdapter
import com.github.libretube.adapters.SearchHistoryAdapter
import com.github.libretube.adapters.SearchSuggestionsAdapter
import com.github.libretube.databinding.FragmentSearchBinding
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.hideKeyboard
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.HttpException
import java.io.IOException

class SearchFragment : Fragment() {
    private val TAG = "SearchFragment"
    private lateinit var binding: FragmentSearchBinding

    private var apiSearchFilter = "all"
    private var nextPage: String? = null

    private var searchAdapter: SearchAdapter? = null
    private var isLoading: Boolean = true
    private var isFetchingSearch: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
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

        binding.clearSearchImageView.setOnClickListener {
            binding.autoCompleteTextView.text.clear()
            binding.historyRecycler.adapter = null
            showHistory()
        }

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

            MaterialAlertDialogBuilder(view.context)
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
                    fetchSearch(binding.autoCompleteTextView.text.toString())
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // show search history
        binding.historyRecycler.layoutManager = LinearLayoutManager(view.context)
        showHistory()

        binding.searchRecycler.layoutManager = GridLayoutManager(view.context, 1)
        binding.autoCompleteTextView.requestFocus()
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.autoCompleteTextView, InputMethodManager.SHOW_IMPLICIT)

        binding.autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() != "") {
                    binding.searchRecycler.adapter = null

                    binding.searchRecycler.viewTreeObserver
                        .addOnScrollChangedListener {
                            if (!binding.searchRecycler.canScrollVertically(1)) {
                                fetchNextSearchItems(binding.autoCompleteTextView.text.toString())
                            }
                        }
                    fetchSuggestions(s.toString(), binding.autoCompleteTextView)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (s!!.isEmpty()) {
                    binding.historyRecycler.adapter = null
                    showHistory()
                }
            }
        })
        binding.autoCompleteTextView.setOnEditorActionListener(
            OnEditorActionListener { textView, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH && textView.text.toString() != "") {
                    view.let { context?.hideKeyboard(it) }
                    binding.searchRecycler.visibility = VISIBLE
                    binding.historyRecycler.visibility = GONE
                    fetchSearch(binding.autoCompleteTextView.text.toString())
                    return@OnEditorActionListener true
                }
                false
            }
        )
    }

    private fun fetchSuggestions(query: String, autoTextView: EditText) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                binding.searchRecycler.visibility = GONE
                binding.historyRecycler.visibility = VISIBLE
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
                val suggestionsAdapter =
                    SearchSuggestionsAdapter(response, autoTextView, this@SearchFragment)
                binding.historyRecycler.adapter = suggestionsAdapter
            }
        }
        if (!isFetchingSearch) run()
    }

    fun fetchSearch(query: String) {
        runOnUiThread {
            binding.historyRecycler.visibility = GONE
        }
        lifecycleScope.launchWhenCreated {
            isFetchingSearch = true
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
            nextPage = response.nextpage
            if (response.items!!.isNotEmpty()) {
                runOnUiThread {
                    binding.searchRecycler.visibility = VISIBLE
                    searchAdapter = SearchAdapter(response.items, childFragmentManager)
                    binding.searchRecycler.adapter = searchAdapter
                }
            }
            addToHistory(query)
            isLoading = false
            isFetchingSearch = false
        }
    }

    private fun fetchNextSearchItems(query: String) {
        lifecycleScope.launchWhenCreated {
            if (!isLoading) {
                isLoading = true
                val response = try {
                    RetrofitInstance.api.getSearchResultsNextPage(
                        query,
                        apiSearchFilter,
                        nextPage!!
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response," + e.response())
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                searchAdapter?.updateItems(response.items!!)
                isLoading = false
            }
        }
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    override fun onStop() {
        super.onStop()
        view?.let { context?.hideKeyboard(it) }
    }

    private fun showHistory() {
        binding.searchRecycler.visibility = GONE
        val historyList = PreferenceHelper.getSearchHistory()
        if (historyList.isNotEmpty()) {
            binding.historyRecycler.adapter =
                SearchHistoryAdapter(
                    historyList,
                    binding.autoCompleteTextView,
                    this
                )
            binding.historyRecycler.visibility = VISIBLE
        }
    }

    private fun addToHistory(query: String) {
        val searchHistoryEnabled =
            PreferenceHelper.getBoolean(PreferenceKeys.SEARCH_HISTORY_TOGGLE, true)
        if (searchHistoryEnabled && query != "") {
            PreferenceHelper.saveToSearchHistory(query)
        }
    }
}
