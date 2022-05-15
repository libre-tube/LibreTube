package com.github.libretube

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.adapters.SearchAdapter
import com.github.libretube.adapters.SearchHistoryAdapter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.chromium.base.ThreadUtils.runOnUiThread
import retrofit2.HttpException
import java.io.IOException


class SearchFragment : Fragment() {
    private val TAG = "SearchFragment"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.search_recycler)

        val autoTextView = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView)

        val historyRecycler = view.findViewById<RecyclerView>(R.id.history_recycler)

        val filterImageView = view.findViewById<ImageView>(R.id.filterMenu_imageView)

        var checkedItem = 0
        var tempSelectedItem = 0

        filterImageView.setOnClickListener {
            val options = arrayOf(getString(R.string.all), getString(R.string.videos), getString(R.string.channels), getString(R.string.playlists))
            AlertDialog.Builder(view.context)
                .setTitle(getString(R.string.choose_filter))
                .setSingleChoiceItems(options, checkedItem, DialogInterface.OnClickListener {
                        dialog, id -> tempSelectedItem = id
                })
                .setPositiveButton(getString(R.string.okay), DialogInterface.OnClickListener {
                        dialog, id -> checkedItem = tempSelectedItem
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .create()
                .show()
        }

        //show search history

        recyclerView.visibility = GONE
        historyRecycler.visibility = VISIBLE

        historyRecycler.layoutManager = LinearLayoutManager(view.context)

        var historylist = getHistory()
        if (historylist.size != 0) {
            historyRecycler.adapter =
                SearchHistoryAdapter(requireContext(), historylist, autoTextView)
        }

        recyclerView.layoutManager = GridLayoutManager(view.context, 1)
        autoTextView.requestFocus()
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm!!.showSoftInput(autoTextView, InputMethodManager.SHOW_IMPLICIT)
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
                    recyclerView.visibility = VISIBLE
                    historyRecycler.visibility = GONE
                    recyclerView.adapter = null

                    GlobalScope.launch {
                        fetchSuggestions(s.toString(), autoTextView)
                        delay(3000)
                        addtohistory(s.toString())
                        fetchSearch(s.toString(), recyclerView)
                    }


                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (s!!.isEmpty()) {
                    recyclerView.visibility = GONE
                    historyRecycler.visibility = VISIBLE
                    var historylist = getHistory()
                    if (historylist.size != 0) {
                        historyRecycler.adapter =
                            SearchHistoryAdapter(requireContext(), historylist, autoTextView)
                    }
                }
            }

        })
        autoTextView.setOnEditorActionListener(OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                autoTextView.dismissDropDown();
                return@OnEditorActionListener true
            }
            false
        })
        autoTextView.setOnItemClickListener { _, _, _, _ ->
            hideKeyboard()
        }
    }

    private fun fetchSuggestions(query: String, autoTextView: AutoCompleteTextView){
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
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, response)
            autoTextView.setAdapter(adapter)
        }
    }
    private fun fetchSearch(query: String, recyclerView: RecyclerView){
        lifecycleScope.launchWhenCreated {
            val response = try {
                RetrofitInstance.api.getSearchResults(query, "all")
            } catch (e: IOException) {
                println(e)
                Log.e(TAG, "IOException, you might not have internet connection $e")
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG, "HttpException, unexpected response")
                return@launchWhenCreated
            }
            if(response.items!!.isNotEmpty()){
               runOnUiThread {
                   recyclerView.adapter = SearchAdapter(response.items)
               }
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
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    override fun onStop() {
        super.onStop()
        hideKeyboard()
    }

    private fun addtohistory(query: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        var historyList = getHistory()


        if (historyList.size != 0 && query == historyList.get(historyList.size - 1)) {
            return
        } else if (query == "") {
            return
        } else {
            historyList = historyList + query

        }



        if (historyList.size > 10) {
            historyList = historyList.takeLast(10)
        }

        var set: Set<String> = HashSet(historyList)

        sharedPreferences.edit().putStringSet("search_history", set)
            .apply()
    }

    private fun getHistory(): List<String> {
        try {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val set: Set<String> = sharedPreferences.getStringSet("search_history", HashSet())!!
            return set.toList()
        } catch (e: Exception) {
            return emptyList()
        }

    }
}

