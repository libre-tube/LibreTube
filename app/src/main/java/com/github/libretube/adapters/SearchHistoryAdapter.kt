package com.github.libretube.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.fragments.SearchFragment
import com.google.android.material.imageview.ShapeableImageView

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
        val cell = layoutInflater.inflate(R.layout.searchhistory_row, parent, false)
        return SearchHistoryViewHolder(cell)
    }

    override fun onBindViewHolder(holder: SearchHistoryViewHolder, position: Int) {
        val history = historyList[position]
        holder.v.findViewById<TextView>(R.id.history_text).text = history

        holder.v.findViewById<ShapeableImageView>(R.id.delete_history).setOnClickListener {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            historyList = historyList - history
            sharedPreferences.edit().putStringSet("search_history", HashSet(historyList)).apply()
            notifyDataSetChanged()
        }

        holder.v.setOnClickListener {
            editText.setText(history)
            searchFragment.fetchSearch(history)
        }
    }
}

class SearchHistoryViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
