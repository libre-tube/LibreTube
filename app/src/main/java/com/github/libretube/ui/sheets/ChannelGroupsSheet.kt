package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.DialogSubscriptionGroupsBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.extensions.move
import com.github.libretube.ui.adapters.SubscriptionGroupsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class ChannelGroupsSheet(
    private val groups: MutableList<SubscriptionGroup>,
    private val onGroupsChanged: (List<SubscriptionGroup>) -> Unit
) : ExpandedBottomSheet() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DialogSubscriptionGroupsBinding.inflate(layoutInflater)

        binding.groupsRV.layoutManager = LinearLayoutManager(context)
        val adapter = SubscriptionGroupsAdapter(
            groups.toMutableList(),
            parentFragmentManager,
            onGroupsChanged
        )
        binding.groupsRV.adapter = adapter

        binding.newGroup.setOnClickListener {
            EditChannelGroupSheet(SubscriptionGroup("", mutableListOf(), 0)) {
                runBlocking(Dispatchers.IO) {
                    DatabaseHolder.Database.subscriptionGroupsDao().createGroup(it)
                }
                groups.add(it)
                adapter.insertItem(it)
                onGroupsChanged(groups)
            }.show(parentFragmentManager, null)
        }

        binding.confirm.setOnClickListener {
            dismiss()
        }

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.absoluteAdapterPosition
                val to = target.absoluteAdapterPosition

                groups.move(from, to)
                adapter.notifyItemMoved(from, to)

                groups.mapIndexed { index, subscriptionGroup -> subscriptionGroup.index = index }
                runBlocking(Dispatchers.IO) {
                    DatabaseHolder.Database.subscriptionGroupsDao().updateAll(groups)
                }
                onGroupsChanged(groups)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }

        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.groupsRV)

        return binding.root
    }
}
