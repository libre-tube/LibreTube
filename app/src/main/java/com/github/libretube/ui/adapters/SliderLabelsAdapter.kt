package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.SliderLabelItemBinding
import com.github.libretube.ui.viewholders.SliderLabelViewHolder

class SliderLabelsAdapter(
    private val playbackSpeeds: List<Float>,
    private val onItemClick: (Float) -> Unit
) : RecyclerView.Adapter<SliderLabelViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderLabelViewHolder {
        val binding = SliderLabelItemBinding.inflate(LayoutInflater.from(parent.context))
        return SliderLabelViewHolder(binding)
    }

    override fun getItemCount() = playbackSpeeds.size

    override fun onBindViewHolder(holder: SliderLabelViewHolder, position: Int) {
        val speed = playbackSpeeds[position]
        holder.binding.apply {
            speedText.text = String.format("%.2f", speed)
            speedCard.setOnClickListener {
                onItemClick.invoke(speed)
            }
        }
    }
}
