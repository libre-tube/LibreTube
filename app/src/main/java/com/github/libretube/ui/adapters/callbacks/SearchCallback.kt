package com.github.libretube.ui.adapters.callbacks

import androidx.recyclerview.widget.DiffUtil
import com.github.libretube.api.obj.ContentItem

object SearchCallback : DiffUtil.ItemCallback<ContentItem>() {
    override fun areItemsTheSame(oldItem: ContentItem, newItem: ContentItem): Boolean {
        return oldItem.url == newItem.url
    }

    override fun areContentsTheSame(oldItem: ContentItem, newItem: ContentItem) = true
}
