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
import android.widget.ImageView
import android.widget.TextView.GONE
import android.widget.TextView.OnEditorActionListener
import android.widget.TextView.VISIBLE
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.adapters.SearchAdapter
import com.github.libretube.adapters.SearchHistoryAdapter
import com.github.libretube.adapters.SearchSuggestionsAdapter
import com.github.libretube.hideKeyboard
import com.github.libretube.util.RetrofitInstance
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.HttpException

class SearchFragment : Fragment() {
    private val TAG = "SearchFragment"
    private var selectedFilter = 0
    private var apiSearchFilter = "all"
    private var nextPage: String? = null
    private lateinit var searchRecView: RecyclerView
    private lateinit var historyRecView: RecyclerView
    private lateinit var autoTextView: EditText
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
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchRecView = view.findViewById(R.id.search_recycler)
        historyRecView = view.findViewById(R.id.history_recycler)
        autoTextView = view.findViewById(R.id.autoCompleteTextView)

        val clearSearchButton = view.findViewById<ImageView>(R.id.clearSearch_imageView)
        val filterImageView = view.findViewById<ImageView>(R.id.filterMenu_imageView)

        var tempSelectedItem = 0

        clearSearchButton.setOnClickListener {
            autoTextView.text.clear()
        }

        filterImageView.setOnClickListener {
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
                .setSingleChoiceItems(filterOptions, selectedFilter) { _, id ->
                    tempSelectedItem = id
                }
                .setPositiveButton(
                    getString(R.string.okay),
                ) { _, _ ->
                    selectedFilter = tempSelectedItem
                    apiSearchFilter = when (selectedFilter) {
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
                    fetchSearch(autoTextView.text.toString())
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .create()
                .show()
        }

        // show search history
        historyRecView.layoutManager = LinearLayoutManager(view.context)
        showHistory()

        searchRecView.layoutManager = GridLayoutManager(view.context, 1)
        autoTextView.requestFocus()
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(autoTextView, InputMethodManager.SHOW_IMPLICIT)

        autoTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s!! != "") {
                    searchRecView.adapter = null

                    searchRecView.viewTreeObserver
                        .addOnScrollChangedListener {
                            if (!searchRecView.canScrollVertically(1)) {
                                fetchNextSearchItems(autoTextView.text.toString())
                            }
                        }

                    GlobalScope.launch {
                        fetchSuggestions(s.toString(), autoTextView)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (s!!.isEmpty()) {
                    showHistory()
                }
            }
        })
        autoTextView.setOnEditorActionListener(
            OnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard()
                    searchRecView.visibility = VISIBLE
                    historyRecView.visibility = GONE
                    fetchSearch(autoTextView.text.toString())
                    return@OnEditorActionListener true
                }
                false
            }
        )
    }

    private fun fetchSuggestions(query: String, autoTextView: EditText) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                searchRecView.visibility = GONE
                historyRecView.visibility = VISIBLE
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
                historyRecView.adapter = suggestionsAdapter
            }
        }
        if (!isFetchingSearch) run()
    }

    fun fetchSearch(query: String) {
        lifecycleScope.launchWhenCreated {
            isFetchingSearch = true
            hideKeyboard()
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
                    historyRecView.visibility = GONE
                    searchRecView.visibility = VISIBLE
                    searchAdapter = SearchAdapter(response.items, childFragmentManager)
                    searchRecView.adapter = searchAdapter
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
        hideKeyboard()
    }

    private fun showHistory() {
        searchRecView.visibility = GONE
        val historyList = getHistory()
        if (historyList.isNotEmpty()) {
            historyRecView.adapter =
                SearchHistoryAdapter(requireContext(), historyList, autoTextView, this)
            historyRecView.visibility = VISIBLE
        }
    }

    private fun addToHistory(query: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val searchHistoryEnabled = sharedPreferences.getBoolean("search_history_toggle", true)
        if (searchHistoryEnabled) {
            var historyList = getHistory()

            if ((historyList.isNotEmpty() && historyList.contains(query)) || query == "") {
                return
            } else {
                historyList = historyList + query
            }

            if (historyList.size > 10) {
                historyList = historyList.takeLast(10)
            }

            val set: Set<String> = HashSet(historyList)

            sharedPreferences.edit().putStringSet("search_history", set)
                .apply()
        }
    }

    private fun getHistory(): List<String> {
        return try {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val set: Set<String> = sharedPreferences.getStringSet("search_history", HashSet())!!
            set.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
