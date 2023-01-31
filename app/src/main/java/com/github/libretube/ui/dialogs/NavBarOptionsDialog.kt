package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.DialogNavbarOptionsBinding
import com.github.libretube.ui.adapters.NavBarOptionsAdapter
import com.github.libretube.helpers.NavBarHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NavBarOptionsDialog : DialogFragment() {
    private lateinit var binding: DialogNavbarOptionsBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogNavbarOptionsBinding.inflate(layoutInflater)

        val options = NavBarHelper.getNavBarItems(requireContext())

        val adapter = NavBarOptionsAdapter(
            options.toMutableList(),
            NavBarHelper.getStartFragmentId(requireContext())
        )

        val itemTouchCallback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                return makeMovementFlags(dragFlags, 0)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val itemToMove = adapter.items[viewHolder.absoluteAdapterPosition]
                adapter.items.remove(itemToMove)
                adapter.items.add(target.absoluteAdapterPosition, itemToMove)

                adapter.notifyItemMoved(
                    viewHolder.absoluteAdapterPosition,
                    target.absoluteAdapterPosition
                )
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // do nothing
            }
        }

        binding.optionsRecycler.layoutManager = LinearLayoutManager(context)
        binding.optionsRecycler.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchHelper.attachToRecyclerView(binding.optionsRecycler)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.navigation_bar)
            .setView(binding.root)
            .setPositiveButton(R.string.okay) { _, _ ->
                NavBarHelper.setNavBarItems(adapter.items, requireContext())
                NavBarHelper.setStartFragment(requireContext(), adapter.selectedHomeTabId)
                RequireRestartDialog()
                    .show(requireParentFragment().childFragmentManager, null)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
