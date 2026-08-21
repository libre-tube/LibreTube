package com.github.libretube.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import com.github.libretube.R
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentTrendsBinding
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.base.BaseBindingFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator

class TrendsFragment : BaseBindingFragment<FragmentTrendsBinding>(R.layout.fragment_trends) {

    override fun setBinding(view: View) {
        setBindingDirect(FragmentTrendsBinding.bind(view))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categories = MediaServiceRepository.instance.getTrendingCategories()

        val adapter = TrendsAdapter(this, categories)
        binding.pager.adapter = adapter

        if (categories.size <= 1) binding.tabLayout.isGone = true
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            val category = categories[position]
            tab.text = getString(category.titleRes)
        }.attach()

        binding.trendingRegion.setOnClickListener {
            showChangeRegionDialog(requireContext()) {
                adapter.getFragmentAt(binding.pager.currentItem)?.also {
                    it.refreshTrending()
                }
            }
        }
    }

    companion object {
        fun showChangeRegionDialog(context: Context, onPositiveButtonClick: () -> Unit) {
            val currentRegionPref = PreferenceHelper.getTrendingRegion(context)

            val countries = LocaleHelper.getAvailableCountries()
            var selected = countries.indexOfFirst { it.code == currentRegionPref }
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.region)
                .setSingleChoiceItems(
                    countries.map { it.name }.toTypedArray(),
                    selected
                ) { _, checked ->
                    selected = checked
                }
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.okay) { _, _ ->
                    PreferenceHelper.putString(PreferenceKeys.REGION, countries[selected].code)
                    onPositiveButtonClick()
                }
                .show()
        }
    }
}
