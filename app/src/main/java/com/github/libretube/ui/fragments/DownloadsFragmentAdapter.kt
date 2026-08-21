package com.github.libretube.ui.fragments

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.libretube.constants.IntentData
import com.github.libretube.enums.DownloadTab
import androidx.core.os.bundleOf

class DownloadsFragmentAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount() = DownloadTab.entries.size

    override fun createFragment(position: Int): Fragment {
        if (position == DownloadTab.PLAYLIST.ordinal) {
            return PlaylistDownloadsFragmentPage()
        }

        return DownloadsFragmentPage().apply {
            arguments = bundleOf(IntentData.downloadTab to DownloadTab.entries[position])
        }
    }
}
