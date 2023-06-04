package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.api.obj.Instances
import com.github.libretube.databinding.InstanceRowBinding
import com.github.libretube.ui.viewholders.InstancesViewHolder

class InstancesAdapter(
    private val instances: List<Instances>,
    private val onSelectInstance: (index: Int) -> Unit
): RecyclerView.Adapter<InstancesViewHolder>() {
    private var selectedInstanceIndex: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstancesViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = InstanceRowBinding.inflate(layoutInflater)
        return InstancesViewHolder(binding)
    }

    override fun getItemCount() = instances.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: InstancesViewHolder, position: Int) {
        val instance = instances[position]
        holder.binding.apply {
            val cdnText = if (instance.cdn) "   (\uD83C\uDF10 CDN)" else ""
            radioButton.text = "${instance.name}   ${instance.locations} $cdnText"
            radioButton.setOnCheckedChangeListener(null)
            radioButton.isChecked = selectedInstanceIndex == position
            radioButton.setOnCheckedChangeListener { _, isChecked ->
                val oldIndex = selectedInstanceIndex
                selectedInstanceIndex = holder.absoluteAdapterPosition
                if (isChecked) onSelectInstance(position)
                oldIndex?.let { notifyItemChanged(it) }
                notifyItemChanged(position)
            }
        }
    }
}
