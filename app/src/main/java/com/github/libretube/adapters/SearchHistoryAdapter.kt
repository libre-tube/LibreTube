package com.github.libretube.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.SearchhistoryRowBinding
import com.github.libretube.fragments.SearchFragment
import com.github.libretube.preferences.PreferenceHelper

class SearchHistoryAdapter(
    private val context: Context,
    private var historyList: List<String>,
    private val editText: EditText,
    private val searchFragment: SearchFragment
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
        val history = historyList[position]
        holder.binding.apply {
            historyText.text = history

            deleteHistory.setOnClickListener {
                historyList = historyList - history
                PreferenceHelper.saveHistory(context, historyList)
                notifyDataSetChanged()
            }

            root.setOnClickListener {
                editText.setText(history)
                searchFragment.fetchSearch(history)
            }
        }
    }
}

class SearchHistoryViewHolder(val binding: SearchhistoryRowBinding) :
    RecyclerView.ViewHolder(binding.root)
