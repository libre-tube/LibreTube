package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.AddChannelToGroupRowBinding
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.ui.viewholders.AddChannelToGroupViewHolder

class AddChannelToGroupAdapter(
    private val channelGroups: MutableList<SubscriptionGroup>,
    private val channelId: String
) : RecyclerView.Adapter<AddChannelToGroupViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddChannelToGroupViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = AddChannelToGroupRowBinding.inflate(layoutInflater, parent, false)
        return AddChannelToGroupViewHolder(binding)
    }

    override fun getItemCount() = channelGroups.size

    override fun onBindViewHolder(holder: AddChannelToGroupViewHolder, position: Int) {
        val channelGroup = channelGroups[position]

        holder.binding.apply {
            groupName.text = channelGroup.name
            groupCheckbox.isChecked = channelGroup.channels.contains(channelId)

            groupCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    channelGroup.channels = channelGroup.channels + channelId
                } else {
                    channelGroup.channels = channelGroup.channels - channelId
                }

                notifyItemChanged(position)
            }
        }
    }
}
