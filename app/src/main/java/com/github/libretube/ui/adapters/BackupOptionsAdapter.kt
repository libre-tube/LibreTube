package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.BackupRowBinding

class BackupOptionsAdapter(
    private val options: List<Int>,
    private val onChange: (position: Int, isChecked: Boolean) -> Unit
) : RecyclerView.Adapter<BackupOptionsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupOptionsViewHolder {
        val binding = BackupRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BackupOptionsViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return options.size
    }

    override fun onBindViewHolder(holder: BackupOptionsViewHolder, position: Int) {
        holder.binding.apply {
            title.text = root.context?.getString(options[position])
            switchWidget.setOnCheckedChangeListener { _, isChecked ->
                onChange.invoke(position, isChecked)
            }
        }
    }
}

class BackupOptionsViewHolder(
    val binding: BackupRowBinding
) : RecyclerView.ViewHolder(binding.root)
