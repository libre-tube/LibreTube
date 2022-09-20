package com.github.libretube.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.obj.BottomSheetItem
import com.github.libretube.ui.adapters.BottomSheetAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class BottomSheet : BottomSheetDialogFragment() {
    private lateinit var items: List<BottomSheetItem>
    private lateinit var listener: (index: Int) -> Unit
    private lateinit var binding: BottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.optionsRecycler.adapter = BottomSheetAdapter(items, listener)
    }

    fun setItems(items: List<BottomSheetItem>, listener: (index: Int) -> Unit) {
        this.items = items
        this.listener = { index ->
            listener.invoke(index)
            dialog?.dismiss()
        }
    }

    fun setSimpleItems(titles: List<String>, listener: (index: Int) -> Unit) {
        this.items = titles.map { BottomSheetItem(it) }
        this.listener = { index ->
            listener.invoke(index)
            dialog?.dismiss()
        }
    }
}
