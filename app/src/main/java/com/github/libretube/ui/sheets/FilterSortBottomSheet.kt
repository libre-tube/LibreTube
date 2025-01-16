package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FilterSortSheetBinding
import com.github.libretube.enums.ContentFilter
import com.github.libretube.extensions.parcelableArrayList
import com.github.libretube.obj.SelectableOption

class FilterSortBottomSheet : ExpandedBottomSheet(R.layout.filter_sort_sheet) {
    private var _binding: FilterSortSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var sortOptions: List<SelectableOption>

    private var selectedIndex = 0
    private var hideWatched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val arguments = requireArguments()
        sortOptions = arguments.parcelableArrayList(IntentData.sortOptions)!!
        hideWatched = arguments.getBoolean(IntentData.hideWatched)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FilterSortSheetBinding.bind(view)
        addSortOptions()
        setInitialFiltersState()

        observeSortChanges()
        observeHideWatchedChanges()
        observeFiltersChanges()
    }

    private fun addSortOptions() {
        sortOptions.forEachIndexed { i, option ->
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

    private fun setInitialFiltersState() {
        binding.filterVideos.isChecked = ContentFilter.VIDEOS.isEnabled
        binding.filterShorts.isChecked = ContentFilter.SHORTS.isEnabled
        binding.filterLivestreams.isChecked = ContentFilter.LIVESTREAMS.isEnabled
        binding.hideWatchedCheckbox.isChecked = hideWatched
    }

    private fun observeSortChanges() {
        binding.sortRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            val index = group.findViewById<RadioButton>(checkedId).tag as Int
            selectedIndex = index
            notifyChange()
        }
    }

    private fun observeHideWatchedChanges() {
        binding.hideWatchedCheckbox.setOnCheckedChangeListener { _, checked ->
            hideWatched = checked
            notifyChange()
        }
    }

    private fun observeFiltersChanges() {
        binding.filters.setOnCheckedStateChangeListener { _, _ ->
            ContentFilter.VIDEOS.isEnabled = binding.filterVideos.isChecked
            ContentFilter.SHORTS.isEnabled = binding.filterShorts.isChecked
            ContentFilter.LIVESTREAMS.isEnabled = binding.filterLivestreams.isChecked
            notifyChange()
        }
    }

    private fun notifyChange() {
        setFragmentResult(
            requestKey = FILTER_SORT_REQUEST_KEY,
            result = bundleOf(
                IntentData.sortOptions to selectedIndex,
                IntentData.hideWatched to hideWatched
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val FILTER_SORT_REQUEST_KEY = "filter_sort_request_key"
    }
}
