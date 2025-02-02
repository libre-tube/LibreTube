package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.github.libretube.databinding.SuggestionRowBinding
import com.github.libretube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.libretube.ui.viewholders.SuggestionsViewHolder

class SearchSuggestionsAdapter(
    private val onRootClickListener: (String) -> Unit,
    private val onArrowClickListener: (String) -> Unit,
) : ListAdapter<String, SuggestionsViewHolder>(DiffUtilItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SuggestionRowBinding.inflate(layoutInflater, parent, false)
        return SuggestionsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionsViewHolder, position: Int) {
        val suggestion = getItem(holder.bindingAdapterPosition)
        holder.binding.apply {
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
