package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.View
import com.github.libretube.R
import com.github.libretube.databinding.FragmentDownloadsBinding
import com.github.libretube.enums.DownloadTab
import com.github.libretube.ui.base.BaseBindingFragment
import com.google.android.material.tabs.TabLayoutMediator

class DownloadsFragment : BaseBindingFragment<FragmentDownloadsBinding>(R.layout.fragment_downloads) {

    override fun setBinding(view: View) {
        setBindingDirect(FragmentDownloadsBinding.bind(view))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.downloadsPager.adapter = DownloadsFragmentAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.downloadsPager) { tab, position ->
            tab.text = when (position) {
                DownloadTab.VIDEO.ordinal -> getString(R.string.video)
                DownloadTab.AUDIO.ordinal -> getString(R.string.audio)
                DownloadTab.PLAYLIST.ordinal -> getString(R.string.playlists)
                else -> throw IllegalArgumentException()
            }
        }.attach()
    }

    fun bindDownloadService() {
        childFragmentManager.fragments.filterIsInstance<DownloadsFragmentPage>().forEach {
            it.bindDownloadService()
        }
    }
}
