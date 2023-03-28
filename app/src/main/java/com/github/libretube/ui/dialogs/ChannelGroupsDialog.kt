package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.databinding.DialogSubscriptionGroupsBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.ui.adapters.SubscriptionGroupsAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class ChannelGroupsDialog(
    private val groups: MutableList<SubscriptionGroup>,
    private val onGroupsChanged: (List<SubscriptionGroup>) -> Unit
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogSubscriptionGroupsBinding.inflate(layoutInflater)

        binding.groupsRV.layoutManager = LinearLayoutManager(context)
        val adapter = SubscriptionGroupsAdapter(
            groups.toMutableList(),
            parentFragmentManager,
            onGroupsChanged
        )
        binding.groupsRV.adapter = adapter

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.channel_groups)
            .setView(binding.root)
            .setPositiveButton(R.string.okay, null)
            .setNeutralButton(R.string.new_group) { _, _ ->
                EditChannelGroupDialog(SubscriptionGroup("", mutableListOf())) {
                    runBlocking(Dispatchers.IO) {
                        DatabaseHolder.Database.subscriptionGroupsDao().createGroup(it)
                    }
                    groups.add(it)
                    adapter.insertItem(it)
                    onGroupsChanged(groups)
                }.show(parentFragmentManager, null)
            }
            .create()
    }
}
