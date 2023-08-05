package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.SuggestionRowBinding
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.ui.viewholders.SuggestionsViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class SearchHistoryAdapter(
    private var historyList: List<String>,
    private val searchView: SearchView
) :
    RecyclerView.Adapter<SuggestionsViewHolder>() {

    override fun getItemCount() = historyList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SuggestionRowBinding.inflate(layoutInflater, parent, false)
        return SuggestionsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionsViewHolder, position: Int) {
        val historyQuery = historyList[position]
        holder.binding.apply {
            suggestionText.text = historyQuery

            deleteHistory.isVisible = true

            deleteHistory.setOnClickListener {
                historyList -= historyQuery
                runBlocking(Dispatchers.IO) {
                    Database.searchHistoryDao().delete(SearchHistoryItem(historyQuery))
                }
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, itemCount)
            }

            root.setOnClickListener {
                searchView.setQuery(historyQuery, true)
            }
            arrow.setOnClickListener {
                searchView.setQuery(historyQuery, false)
            }
        }
    }
}
