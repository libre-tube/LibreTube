package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import com.github.libretube.databinding.FilterSortSheetBinding
import com.github.libretube.enums.ContentFilter
import com.github.libretube.obj.SelectableOption

class FilterSortBottomSheet: ExpandedBottomSheet() {

    private var _binding: FilterSortSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var sortOptions: Collection<SelectableOption>
    private lateinit var sortFilter: (index: Int, isSelected: Boolean) -> Unit
    private lateinit var filterListener: () -> Unit

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
            sortFilter(index, true)
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

            filterListener.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        fun createWith(
            sortOptions: Collection<SelectableOption>,
            sortListener: (index: Int, isSelected: Boolean) -> Unit,
            filtersListener: () -> Unit
        ) = FilterSortBottomSheet().apply {
            this.sortOptions = sortOptions
            this.sortFilter = sortListener
            this.filterListener = filtersListener
        }

    }

}