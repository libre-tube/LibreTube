package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.NavOptionsItemBinding
import com.github.libretube.ui.viewholders.NavBarOptionsViewHolder

class NavBarOptionsAdapter(
    val items: MutableList<MenuItem>,
    var selectedHomeTabId: Int
) : RecyclerView.Adapter<NavBarOptionsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NavBarOptionsViewHolder {
        val binding = NavOptionsItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NavBarOptionsViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: NavBarOptionsViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            title.text = item.title
            checkbox.isChecked = item.isVisible
            home.setImageResource(
                if (item.itemId == selectedHomeTabId) R.drawable.ic_home_dark else R.drawable.ic_home_outlined
            )
            home.setOnClickListener {
                if (selectedHomeTabId == item.itemId) {
                    return@setOnClickListener
                }
                val oldSelection = items.indexOfFirst { it.itemId == selectedHomeTabId }
                selectedHomeTabId = item.itemId
                listOf(position, oldSelection).forEach {
                    notifyItemChanged(it)
                }
            }
            checkbox.setOnClickListener {
                item.isVisible = checkbox.isChecked
            }
        }
    }
}
