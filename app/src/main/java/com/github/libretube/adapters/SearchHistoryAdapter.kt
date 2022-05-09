package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R

class SearchHistoryAdapter(private val historyList: List<String>) :
    RecyclerView.Adapter<SearchHistoryViewHolder>() {
    override fun getItemCount(): Int {
        return historyList.size
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHistoryViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(R.layout.searchhistory_row, parent, false)
        return SearchHistoryViewHolder(cell)
    }

    override fun onBindViewHolder(holder: SearchHistoryViewHolder, position: Int) {
        val history = historyList[position]
        holder.v.findViewById<TextView>(R.id.history_text).text = history
//        holder.v.findViewById<TextView>(R.id.delete_history).setOnClickListener(
//
//        )
    }
}

class SearchHistoryViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}