package com.github.libretube.ui.fragments

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.constants.IntentData

class ChannelContentAdapter(
    private val list: List<ChannelTab>,
    private val channelId: String?,
    fragment: Fragment
) : FragmentStateAdapter(fragment) {
    override fun getItemCount() = list.size

    override fun createFragment(position: Int) = ChannelContentFragment().apply {
        arguments = bundleOf(
            IntentData.tabData to list[position],
            IntentData.channelId to channelId
        )
    }
}
