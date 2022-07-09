package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.SearchsuggestionRowBinding
import com.github.libretube.fragments.SearchFragment

class SearchSuggestionsAdapter(
    private var suggestionsList: List<String>,
    private var editText: EditText,
    private val searchFragment: SearchFragment
) :
    RecyclerView.Adapter<SearchSuggestionsViewHolder>() {

    private val TAG = "SearchSuggestionsAdapter"

    override fun getItemCount(): Int {
        return suggestionsList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchSuggestionsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SearchsuggestionRowBinding.inflate(layoutInflater, parent, false)
        return SearchSuggestionsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchSuggestionsViewHolder, position: Int) {
        val suggestion = suggestionsList[position]
        holder.binding.apply {
            suggestionText.text = suggestion
            root.setOnClickListener {
                editText.setText(suggestion)
                searchFragment.fetchSearch(editText.text.toString())
            }
        }
    }
}

class SearchSuggestionsViewHolder(val binding: SearchsuggestionRowBinding) :
    RecyclerView.ViewHolder(binding.root)
