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
    val items: MutableList<MenuItem>
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
            checkbox.setOnClickListener {
                if (!checkbox.isChecked && getVisibleItemsCount() < 2) {
                    checkbox.isChecked = true
                    Toast.makeText(
                        root.context,
                        R.string.select_at_least_one,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                items[position].isVisible = checkbox.isChecked
            }
        }
    }

    private fun getVisibleItemsCount(): Int {
        return items.filter { it.isVisible }.size
    }
}
