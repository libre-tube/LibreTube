package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.SuggestionRowBinding
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.extensions.query
import com.github.libretube.ui.viewholders.SuggestionsViewHolder

class SearchHistoryAdapter(
    private var historyList: List<String>,
    private val searchView: SearchView
) :
    RecyclerView.Adapter<SuggestionsViewHolder>() {

    override fun getItemCount(): Int {
        return historyList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SuggestionRowBinding.inflate(layoutInflater, parent, false)
        return SuggestionsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionsViewHolder, position: Int) {
        val historyQuery = historyList[position]
        holder.binding.apply {
            suggestionText.text = historyQuery

            deleteHistory.visibility = View.VISIBLE

            deleteHistory.setOnClickListener {
                historyList -= historyQuery
                query {
                    Database.searchHistoryDao().delete(
                        SearchHistoryItem(query = historyQuery)
                    )
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
