package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.fragments.SearchFragment

class SearchSuggestionsAdapter(
    private var suggestionsList: List<String>,
    private var editText: EditText,
    private val searchFragment: SearchFragment
) :
    RecyclerView.Adapter<SearchSuggestionsViewHolder>() {

    override fun getItemCount(): Int {
        return suggestionsList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchSuggestionsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(R.layout.searchsuggestion_row, parent, false)
        return SearchSuggestionsViewHolder(cell)
    }

    override fun onBindViewHolder(holder: SearchSuggestionsViewHolder, position: Int) {
        val suggestion = suggestionsList[position]
        val suggestionTextView = holder.v.findViewById<TextView>(R.id.suggestion_text)
        suggestionTextView.text = suggestion
        holder.v.setOnClickListener {
            editText.setText(suggestion)
            searchFragment.fetchSearch(editText.text.toString())
        }
    }
}

class SearchSuggestionsViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
