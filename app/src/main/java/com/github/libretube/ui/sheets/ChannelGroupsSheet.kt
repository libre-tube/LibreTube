package com.github.libretube.ui.sheets

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.databinding.DialogSubscriptionGroupsBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.extensions.move
import com.github.libretube.extensions.setOnDraggedListener
import com.github.libretube.ui.adapters.SubscriptionGroupsAdapter
import com.github.libretube.ui.models.EditChannelGroupsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelGroupsSheet : ExpandedBottomSheet() {
    private val channelGroupsModel: EditChannelGroupsModel by activityViewModels()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DialogSubscriptionGroupsBinding.inflate(layoutInflater)

        binding.groupsRV.layoutManager = LinearLayoutManager(context)
        val groups = channelGroupsModel.groups.value.orEmpty().toMutableList()
        val adapter = SubscriptionGroupsAdapter(groups, channelGroupsModel, parentFragmentManager)
        binding.groupsRV.adapter = adapter

        binding.newGroup.setOnClickListener {
            channelGroupsModel.groupToEdit = SubscriptionGroup("", mutableListOf(), 0)
            EditChannelGroupSheet().show(parentFragmentManager, null)
        }

        channelGroupsModel.groups.observe(viewLifecycleOwner) {
            adapter.groups = channelGroupsModel.groups.value.orEmpty().toMutableList()
            lifecycleScope.launch { adapter.notifyDataSetChanged() }
        }

        binding.confirm.setOnClickListener {
            channelGroupsModel.groups.value = adapter.groups
            channelGroupsModel.groups.value?.forEachIndexed { index, group -> group.index = index }
            CoroutineScope(Dispatchers.IO).launch {
                DatabaseHolder.Database.subscriptionGroupsDao()
                    .updateAll(channelGroupsModel.groups.value.orEmpty())
            }
            dismiss()
        }

        binding.groupsRV.setOnDraggedListener { from, to ->
            adapter.groups.move(from, to)
            adapter.notifyItemMoved(from, to)
        }

        return binding.root
    }
}
