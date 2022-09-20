package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.NavOptionsItemBinding
import com.github.libretube.obj.NavBarItem

class NavBarOptionsAdapter(
    val items: MutableList<NavBarItem>
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
            title.text = root.context.getString(item.titleResource)
            checkbox.isChecked = item.isEnabled
            checkbox.setOnClickListener {
                if (!checkbox.isChecked && getEnabledItemsCount() < 2) {
                    checkbox.isChecked = true
                    Toast.makeText(
                        root.context,
                        R.string.select_at_least_one,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                items[position].isEnabled = checkbox.isChecked
            }
        }
    }

    private fun getEnabledItemsCount(): Int {
        return items.filter { it.isEnabled }.size
    }
}

class NavBarOptionsViewHolder(
    val binding: NavOptionsItemBinding
) : RecyclerView.ViewHolder(binding.root)
