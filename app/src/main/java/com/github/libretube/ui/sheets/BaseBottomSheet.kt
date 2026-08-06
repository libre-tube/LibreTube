package com.github.libretube.ui.sheets

import android.os.Bundle
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
import com.github.libretube.ui.extensions.onSystemInsets
import kotlinx.coroutines.launch


open class BaseBottomSheet(@LayoutRes layoutResId: Int = R.layout.bottom_sheet) : ExpandedBottomSheet(layoutResId) {

    private var title: String? = null
    private lateinit var items: List<BottomSheetItem>
    private lateinit var listener: (index: Int) -> Unit

    private lateinit var adapter: BottomSheetAdapter

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

        binding.optionsRecycler.layoutManager = LinearLayoutManager(requireContext())

        val adapter = BottomSheetAdapter(listener)
        adapter.submitList(items)
        binding.optionsRecycler.adapter = adapter

        // add bottom padding to the list, to ensure that the last item is not overlapped by the system bars
        binding.optionsRecycler.onSystemInsets { v, systemInsets ->
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                systemInsets.bottom
            )
        }

    }

    fun setItems(items: List<BottomSheetItem>, listener: (suspend (index: Int) -> Unit)?) = apply {
        this.items = items

        // if the caller calls `setItems` while the sheet is already visible, update the existing list
        if (::adapter.isInitialized) adapter.submitList(items)

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
        setItems(titles.map { BottomSheetItem(it, isSelected = it == preselectedItem) }, listener)
    }

    companion object {
        private val titleTextSize = 7f.dpToPx().toFloat()
        private val titleMargin = 24f.dpToPx()
    }
}
