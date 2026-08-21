package com.github.libretube.ui.fragments

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.libretube.api.TrendingCategory
import com.github.libretube.constants.IntentData

class TrendsAdapter(fragment: Fragment, private val categories: List<TrendingCategory>) :
    FragmentStateAdapter(fragment) {

    private val fragments: MutableList<TrendsContentFragment?> =
        MutableList(categories.size) { null }

    override fun createFragment(position: Int): Fragment {
        val trendContentFragment = TrendsContentFragment().apply {
            arguments = bundleOf(IntentData.category to categories[position])
        }
        fragments[position] = trendContentFragment
        return trendContentFragment
    }

    override fun getItemCount(): Int = categories.size

    fun getFragmentAt(position: Int): TrendsContentFragment? = fragments[position]
}
