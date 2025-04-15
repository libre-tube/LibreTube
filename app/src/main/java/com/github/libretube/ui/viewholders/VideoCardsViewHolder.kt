package com.github.libretube.ui.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.AllCaughtUpRowBinding
import com.github.libretube.databinding.TrendingRowBinding

class VideoCardsViewHolder : RecyclerView.ViewHolder {
    var trendingRowBinding: TrendingRowBinding? = null
    var allCaughtUpBinding: AllCaughtUpRowBinding? = null

    constructor(binding: TrendingRowBinding) : super(binding.root) {
        trendingRowBinding = binding
    }

    constructor(binding: AllCaughtUpRowBinding) : super(binding.root) {
        allCaughtUpBinding = binding
    }
}
