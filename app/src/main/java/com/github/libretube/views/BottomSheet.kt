package com.github.libretube.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.adapters.BottomSheetAdapter
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.obj.BottomSheetItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheet : BottomSheetDialogFragment() {
    private lateinit var items: List<BottomSheetItem>
    private lateinit var listener: (index: Int) -> Unit
    private lateinit var binding: BottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog!!.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheetInternal =
                d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)!!
            BottomSheetBehavior.from(bottomSheetInternal).state =
                BottomSheetBehavior.STATE_EXPANDED
        }

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
