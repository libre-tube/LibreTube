package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.BottomSheetItemBinding
import com.github.libretube.obj.BottomSheetItem

class BottomSheetAdapter(
    private val items: List<BottomSheetItem>,
    private val listener: (index: Int) -> Unit
) : RecyclerView.Adapter<BottomSheetViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomSheetViewHolder {
        val binding = BottomSheetItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BottomSheetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BottomSheetViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            title.text =
                if (item.currentValue != null) "${item.title} (${item.currentValue})" else item.title
            if (item.drawable != null) {
                drawable.setImageResource(item.drawable)
            } else {
                drawable.visibility =
                    View.GONE
            }

            root.setOnClickListener {
                listener.invoke(position)
            }

            // increase padding if there's no drawable
            if (item.drawable == null) {
                root.setPadding(
                    root.paddingLeft * 2,
                    root.paddingTop,
                    root.paddingRight,
                    root.paddingBottom
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class BottomSheetViewHolder(
    val binding: BottomSheetItemBinding
) : RecyclerView.ViewHolder(binding.root)
