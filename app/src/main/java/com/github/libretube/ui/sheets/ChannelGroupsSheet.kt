package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.databinding.DialogSubscriptionGroupsBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.ui.adapters.SubscriptionGroupsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class ChannelGroupsSheet(
    private val groups: MutableList<SubscriptionGroup>,
    private val onGroupsChanged: (List<SubscriptionGroup>) -> Unit,
) : ExpandedBottomSheet() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = DialogSubscriptionGroupsBinding.inflate(layoutInflater)

        binding.groupsRV.layoutManager = LinearLayoutManager(context)
        val adapter = SubscriptionGroupsAdapter(
            groups.toMutableList(),
            parentFragmentManager,
            onGroupsChanged,
        )
        binding.groupsRV.adapter = adapter

        binding.newGroup.setOnClickListener {
            EditChannelGroupSheet(SubscriptionGroup("", mutableListOf())) {
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

        return binding.root
    }
}
