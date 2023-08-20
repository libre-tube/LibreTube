package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.obj.Instances
import com.github.libretube.databinding.InstanceRowBinding
import com.github.libretube.ui.models.WelcomeModel
import com.github.libretube.ui.viewholders.InstancesViewHolder

class InstancesAdapter(
    private val instances: List<Instances>,
    viewModel: WelcomeModel,
    private val onSelectInstance: (index: Int) -> Unit
) : RecyclerView.Adapter<InstancesViewHolder>() {
    private var selectedInstanceIndex = viewModel.selectedInstanceIndex.value

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
            var instanceText = "${instance.name}   ${instance.locations}"
            if (instance.cdn) instanceText += "   (\uD83C\uDF10 CDN)"
            if (instance.registrationDisabled) {
                instanceText +=
                    "   (${root.context.getString(R.string.registration_disabled)})"
            }
            radioButton.text = instanceText
            radioButton.setOnCheckedChangeListener(null)
            radioButton.isChecked = selectedInstanceIndex == position
            radioButton.setOnCheckedChangeListener { _, isChecked ->
                val oldIndex = selectedInstanceIndex
                selectedInstanceIndex = holder.absoluteAdapterPosition
                if (isChecked) onSelectInstance(position)
                oldIndex?.let { notifyItemChanged(oldIndex) }
                notifyItemChanged(position)
            }
        }
    }
}
