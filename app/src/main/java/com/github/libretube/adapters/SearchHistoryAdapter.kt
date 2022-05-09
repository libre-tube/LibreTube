package com.github.libretube.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.google.android.material.imageview.ShapeableImageView


class SearchHistoryAdapter(private val context: Context, private val historyList: List<String>) :
    RecyclerView.Adapter<SearchHistoryViewHolder>() {
    override fun getItemCount(): Int {
        return historyList.size -1
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHistoryViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(R.layout.searchhistory_row, parent, false)
        return SearchHistoryViewHolder(cell)
    }

    override fun onBindViewHolder(holder: SearchHistoryViewHolder, position: Int) {
        val history = historyList[position+1]
        holder.v.findViewById<TextView>(R.id.history_text).text = history


        holder.v.findViewById<ShapeableImageView>(R.id.delete_history).setOnClickListener {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            var splited_history = sharedPreferences.getString("search_history", "")!!.split("|")

            splited_history = splited_history - history

            sharedPreferences.edit().putString("search_history", splited_history.joinToString("|"))
                .apply()

        }
    }
}

class SearchHistoryViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}