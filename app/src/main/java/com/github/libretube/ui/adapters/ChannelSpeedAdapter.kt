package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.ChannelSpeedRowBinding
import com.github.libretube.extensions.round
import com.github.libretube.helpers.PlayerHelper

data class ChannelSpeedItem(
    val channelId: String,
    val channelName: String?,
    val speed: Float
)

class ChannelSpeedAdapter(
    private val onSpeedChanged: (channelId: String, speed: Float) -> Unit,
    private val onDelete: (channelId: String) -> Unit
) : ListAdapter<ChannelSpeedItem, ChannelSpeedAdapter.ChannelSpeedViewHolder>(ChannelSpeedDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelSpeedViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ChannelSpeedRowBinding.inflate(layoutInflater, parent, false)
        return ChannelSpeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelSpeedViewHolder, position: Int) {
        val item = getItem(holder.bindingAdapterPosition)
        holder.bind(item)
    }

    inner class ChannelSpeedViewHolder(
        val binding: ChannelSpeedRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChannelSpeedItem) {
            binding.apply {
                channelName.text = item.channelName ?: item.channelId

                speedSlider.value = item.speed
                speedSlider.valueFrom = 0.2f
                speedSlider.valueTo = 4.0f
                speedSlider.stepSize = 0.05f

                updateSpeedDisplay(item.speed)

                minus.setOnClickListener {
                    val newValue = maxOf(0.2f, speedSlider.value - 0.05f)
                    speedSlider.value = newValue
                    updateSpeedDisplay(newValue)
                    onSpeedChanged(item.channelId, newValue)
                }

                plus.setOnClickListener {
                    val newValue = minOf(4.0f, speedSlider.value + 0.05f)
                    speedSlider.value = newValue
                    updateSpeedDisplay(newValue)
                    onSpeedChanged(item.channelId, newValue)
                }

                speedSlider.addOnChangeListener { slider, value, fromUser ->
                    if (fromUser) {
                        updateSpeedDisplay(value)
                        onSpeedChanged(item.channelId, value)
                    }
                    minus.alpha = if (slider.value <= slider.valueFrom) 0.5f else 1f
                    plus.alpha = if (slider.value >= slider.valueTo) 0.5f else 1f
                }

                deleteButton.setOnClickListener {
                    onDelete(item.channelId)
                }
            }
        }

        private fun updateSpeedDisplay(speed: Float) {
            binding.speedValue.text = "${speed.round(2)}x"
        }
    }

    class ChannelSpeedDiffCallback : DiffUtil.ItemCallback<ChannelSpeedItem>() {
        override fun areItemsTheSame(oldItem: ChannelSpeedItem, newItem: ChannelSpeedItem): Boolean {
            return oldItem.channelId == newItem.channelId
        }

        override fun areContentsTheSame(oldItem: ChannelSpeedItem, newItem: ChannelSpeedItem): Boolean {
            return oldItem.channelId == newItem.channelId &&
                    oldItem.channelName == newItem.channelName &&
                    oldItem.speed == newItem.speed
        }
    }
}
