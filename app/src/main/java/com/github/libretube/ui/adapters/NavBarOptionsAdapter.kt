package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
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

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: NavBarOptionsViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            title.text = item.title
            checkbox.isChecked = item.isVisible
            home.setImageResource(
                if (item.itemId == selectedHomeTabId) R.drawable.ic_home else R.drawable.ic_home_outlined
            )
            home.setOnClickListener {
                if (selectedHomeTabId == item.itemId) {
                    return@setOnClickListener
                }
                if (!item.isVisible) {
                    Toast.makeText(root.context, R.string.not_enabled, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val oldSelection = items.indexOfFirst { it.itemId == selectedHomeTabId }
                selectedHomeTabId = item.itemId
                listOf(position, oldSelection).forEach {
                    notifyItemChanged(it)
                }
            }
            checkbox.setOnClickListener {
                if (item.itemId == selectedHomeTabId) {
                    Toast.makeText(root.context, R.string.select_other_start_tab, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!checkbox.isChecked && getVisibleItemsCount() < 2) {
                    checkbox.isChecked = true
                    Toast.makeText(
                        root.context,
                        R.string.select_at_least_one,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                item.isVisible = checkbox.isChecked
            }
        }
    }

    private fun getVisibleItemsCount(): Int {
        return items.filter { it.isVisible }.size
    }
}
