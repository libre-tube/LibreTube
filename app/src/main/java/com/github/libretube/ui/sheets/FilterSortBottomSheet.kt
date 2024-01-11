package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.github.libretube.databinding.FilterSortSheetBinding
import com.github.libretube.enums.ContentFilter
import com.github.libretube.obj.SelectableOption

class FilterSortBottomSheet: ExpandedBottomSheet() {

    private var _binding: FilterSortSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var sortOptions: Collection<SelectableOption>

    private var selectedIndex: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FilterSortSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        addSortOptions()
        observeSortChanges()
        setInitialFiltersState()
        observeFiltersChanges()
    }

    private fun addSortOptions() {
        for (i in sortOptions.indices) {
            val option = sortOptions.elementAt(i)
            val rb = createRadioButton(i, option.name)

            binding.sortRadioGroup.addView(rb)

            if (option.isSelected) {
                selectedIndex = i
                binding.sortRadioGroup.check(rb.id)
            }
        }
    }

    private fun createRadioButton(index: Int, name: String): RadioButton {
        return RadioButton(context).apply {
            tag = index
            text = name
        }
    }

    private fun observeSortChanges() {
        binding.sortRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            val index = group.findViewById<RadioButton>(checkedId).tag as Int
            selectedIndex = index
            notifyChange()
        }
    }

    private fun setInitialFiltersState() {
        binding.filterVideos.isChecked = ContentFilter.VIDEOS.isEnabled()
        binding.filterShorts.isChecked =  ContentFilter.SHORTS.isEnabled()
        binding.filterLivestreams.isChecked = ContentFilter.LIVESTREAMS.isEnabled()
    }

    private fun observeFiltersChanges() {
        binding.filters.setOnCheckedStateChangeListener { _, _ ->
            ContentFilter.VIDEOS.setState(binding.filterVideos.isChecked)
            ContentFilter.SHORTS.setState(binding.filterShorts.isChecked)
            ContentFilter.LIVESTREAMS.setState(binding.filterLivestreams.isChecked)
            notifyChange()
        }
    }

    private fun notifyChange() {
        setFragmentResult(
            requestKey = FILTER_SORT_REQUEST_KEY,
            result = bundleOf(SELECTED_SORT_OPTION_KEY to selectedIndex)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        const val FILTER_SORT_REQUEST_KEY = "filter_sort_request_key"
        const val SELECTED_SORT_OPTION_KEY = "selected_sort_option_key"

        fun createWith(
            sortOptions: Collection<SelectableOption>
        ) = FilterSortBottomSheet().apply {
            this.sortOptions = sortOptions
        }

    }

}