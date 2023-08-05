package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.SuggestionRowBinding
import com.github.libretube.ui.viewholders.SuggestionsViewHolder

class SearchSuggestionsAdapter(
    private var suggestionsList: List<String>,
    private val searchView: SearchView
) :
    RecyclerView.Adapter<SuggestionsViewHolder>() {

    override fun getItemCount() = suggestionsList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SuggestionRowBinding.inflate(layoutInflater, parent, false)
        return SuggestionsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionsViewHolder, position: Int) {
        val suggestion = suggestionsList[position]
        holder.binding.apply {
            suggestionText.text = suggestion
            root.setOnClickListener {
                searchView.setQuery(suggestion, true)
            }
            arrow.setOnClickListener {
                searchView.setQuery(suggestion, false)
            }
        }
    }
}
