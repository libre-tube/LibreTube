package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import com.github.libretube.databinding.SuggestionRowBinding
import com.github.libretube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.libretube.ui.viewholders.SuggestionsViewHolder
import com.github.libretube.R
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.enums.SearchDataType
import com.github.libretube.obj.SearchDataItem
import kotlin.collections.plus

class SearchSuggestionsAdapter(
    private val onRootClickListener: (String) -> Unit,
    private val onArrowClickListener: (String) -> Unit,
    private val onSearchHistoryItemDeleted: (SearchHistoryItem) -> Unit,
) : ListAdapter<SearchDataItem, SuggestionsViewHolder>(DiffUtilItemCallback<SearchDataItem>()) {

    /**
     *  Allow submit list partially, either [historyList] only or [suggestionList] only, without
     *  updating the whole list.
     */
    fun submitSearchSuggestions(
        historyList: List<SearchDataItem>?,
        suggestionList: List<SearchDataItem>?,
        commitCallback: Runnable? = null,
    ) {
        if (historyList == null && suggestionList == null) return

        val oldList = currentList.toList()
        val histories = historyList ?: oldList.filter { it.type == SearchDataType.HISTORY }
        val suggestions = suggestionList ?: oldList.filter { it.type == SearchDataType.SUGGESTION }
        val newList = (histories + suggestions).distinctBy { it.query }

        super.submitList(newList, commitCallback)
    }

    /**
     * @see [submitSearchSuggestions]
     */
    @Deprecated("Use `submitSearchSuggestions()` instead.")
    override fun submitList(list: List<SearchDataItem>?) {}

    /**
     * @see [submitSearchSuggestions]
     */
    @Deprecated("Use `submitSearchSuggestions()` instead.")
    override fun submitList(list: List<SearchDataItem>?, commitCallback: Runnable?) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SuggestionRowBinding.inflate(layoutInflater, parent, false)
        return SuggestionsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionsViewHolder, position: Int) {
        val item = getItem(holder.bindingAdapterPosition)
        val suggestion = item.query

        holder.binding.apply {
            when (item.type) {
                SearchDataType.HISTORY -> {
                    deleteHistory.isVisible = true
                    deleteHistory.setOnClickListener {
                        onSearchHistoryItemDeleted(SearchHistoryItem(suggestion))
                    }
                    suggestionText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.ic_history, 0, 0, 0
                    )
                }

                SearchDataType.SUGGESTION -> {
                    suggestionText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.ic_search, 0, 0, 0
                    )
                }
            }
            suggestionText.text = suggestion
            root.setOnClickListener {
                onRootClickListener(suggestion)
            }
            arrow.setOnClickListener {
                onArrowClickListener(suggestion)
            }
        }
    }
}
