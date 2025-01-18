package com.github.libretube.ui.sheets

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.extensions.dpToPx
import com.github.libretube.obj.BottomSheetItem
import com.github.libretube.ui.adapters.BottomSheetAdapter
import kotlinx.coroutines.launch

open class BaseBottomSheet(@LayoutRes layoutResId: Int = R.layout.bottom_sheet) : ExpandedBottomSheet(layoutResId) {

    private var title: String? = null
    private var preselectedItem: String? = null
    private lateinit var items: List<BottomSheetItem>
    private lateinit var listener: (index: Int) -> Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = BottomSheetBinding.bind(view)

        if (title != null) {
            binding.bottomSheetTitleLayout.isVisible = true

            binding.bottomSheetTitle.text = title
            binding.bottomSheetTitle.textSize = titleTextSize
            binding.bottomSheetTitle.updateLayoutParams<MarginLayoutParams> {
                marginStart = titleMargin
                marginEnd = titleMargin
            }
        }

        // set the selected item
        for (item in items) {
            Log.e(item.title, preselectedItem.toString())
        }
        for (item in items.filter { it.title == preselectedItem }) {
            item.title = "${item.title} ✓"
        }
        binding.optionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.optionsRecycler.adapter = BottomSheetAdapter(items, listener)
    }

    fun setItems(items: List<BottomSheetItem>, listener: (suspend (index: Int) -> Unit)?) = apply {
        this.items = items
        this.listener = { index ->
            lifecycleScope.launch {
                dialog?.hide()
                listener?.invoke(index)
                runCatching {
                    dismiss()
                }
            }
        }
    }

    fun setTitle(title: String?) {
        this.title = title
    }

    fun setSimpleItems(
        titles: List<String>,
        preselectedItem: String? = null,
        listener: (suspend (index: Int) -> Unit)?
    ) = apply {
        setItems(titles.map { BottomSheetItem(it) }, listener)
        this.preselectedItem = preselectedItem
    }

    companion object {
        private val titleTextSize = 7f.dpToPx().toFloat()
        private val titleMargin = 24f.dpToPx()
    }
}
