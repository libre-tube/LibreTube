package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.SearchhistoryRowBinding
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.db.obj.SearchHistoryItem

class SearchHistoryAdapter(
    private var historyList: List<String>,
    private val searchView: SearchView
) :
    RecyclerView.Adapter<SearchHistoryViewHolder>() {

    override fun getItemCount(): Int {
        return historyList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHistoryViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SearchhistoryRowBinding.inflate(layoutInflater, parent, false)
        return SearchHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchHistoryViewHolder, position: Int) {
        val historyQuery = historyList[position]
        holder.binding.apply {
            historyText.text = historyQuery

            deleteHistory.setOnClickListener {
                val itemIndex = historyList.indexOf(historyQuery)
                historyList -= historyQuery
                Thread {
                    Database.searchHistoryDao().delete(
                        SearchHistoryItem(query = historyQuery)
                    )
                }.start()
                notifyItemRemoved(itemIndex)
            }

            root.setOnClickListener {
                searchView.setQuery(historyQuery, true)
            }
        }
    }
}

class SearchHistoryViewHolder(val binding: SearchhistoryRowBinding) :
    RecyclerView.ViewHolder(binding.root)
