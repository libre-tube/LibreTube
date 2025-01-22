package com.github.libretube.ui.adapters.callbacks

import androidx.recyclerview.widget.DiffUtil


fun <T> diffUtilItemCallback(
    areItemsTheSame: (T, T) -> Boolean = ::equals,
    areContentsTheSame: (T, T) -> Boolean = ::equals,
): DiffUtil.ItemCallback<T> = object : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T & Any, newItem: T & Any) =
        areItemsTheSame(oldItem, newItem)

    override fun areContentsTheSame(oldItem: T & Any, newItem: T & Any) =
        areContentsTheSame(oldItem, newItem)
}

private fun <T> equals(first: T, second: T) = first == second
