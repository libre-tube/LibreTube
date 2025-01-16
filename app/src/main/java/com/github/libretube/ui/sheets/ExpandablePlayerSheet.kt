package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.activityViewModels
import com.github.libretube.ui.models.CommonPlayerViewModel

abstract class ExpandablePlayerSheet(@LayoutRes layoutResId: Int) :
    UndimmedBottomSheet(layoutResId) {
    private val commonPlayerViewModel: CommonPlayerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        commonPlayerViewModel.setSheetExpand(true)
        commonPlayerViewModel.sheetExpand.observe(viewLifecycleOwner) {
            when (it) {
                true -> expand()
                false -> expand(true)
                else -> dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        commonPlayerViewModel.setSheetExpand(null)
    }
}