package com.github.libretube.ui.sheets

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.obj.BottomSheetItem
import com.github.libretube.ui.adapters.BottomSheetAdapter
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class BaseBottomSheet : BottomSheetDialogFragment() {
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener {
            (it as BottomSheetDialog).let { d ->
                (d.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout?)?.let {
                    BottomSheetBehavior.from(it).state =
                        BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

        return dialog
    }

    fun setItems(items: List<BottomSheetItem>, listener: ((index: Int) -> Unit)?) = apply {
        this.items = items
        this.listener = { index ->
            listener?.invoke(index)
            dialog?.dismiss()
        }
    }

    fun setSimpleItems(titles: List<String>, listener: ((index: Int) -> Unit)?) = apply {
        this.items = titles.map { BottomSheetItem(it) }
        this.listener = { index ->
            listener?.invoke(index)
            dialog?.dismiss()
        }
    }

    fun show(fragmentManager: FragmentManager) = show(
        fragmentManager,
        null
    )

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        dialog?.dismiss()
    }
}
