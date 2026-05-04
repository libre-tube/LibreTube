package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogAddChannelToGroupBinding
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.repo.UserDataRepositoryHelper
import com.github.libretube.ui.adapters.AddChannelToGroupAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddChannelToGroupSheet : ExpandedBottomSheet(R.layout.dialog_add_channel_to_group) {
    private lateinit var channelId: String

    private val addToGroupAdapter by lazy(LazyThreadSafetyMode.NONE) {
        AddChannelToGroupAdapter(channelId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        channelId = arguments?.getString(IntentData.channelId)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DialogAddChannelToGroupBinding.bind(view)

        binding.groupsRV.adapter = addToGroupAdapter

        binding.cancel.setOnClickListener {
            requireDialog().dismiss()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val initialSubscriptionGroups = UserDataRepositoryHelper.userDataRepository
                .getSubscriptionGroups().sortedBy { it.index }

            val modifiableGroups = initialSubscriptionGroups.toMutableList()
            withContext(Dispatchers.Main) {
                addToGroupAdapter.submitList(initialSubscriptionGroups)

                binding.okay.setOnClickListener {
                    requireDialog().hide()

                    lifecycleScope.launch(Dispatchers.IO) {
                        applyGroupsDiff(initialSubscriptionGroups, modifiableGroups.toList())

                        withContext(Dispatchers.Main) {
                            dialog?.dismiss()
                        }
                    }
                }
            }
        }
    }

    companion object {
        suspend fun applyGroupsDiff(
            initialChannelGroups: List<SubscriptionGroup>,
            modifiedChannelGroups: List<SubscriptionGroup>
        ) {
            // TODO: very ugly, refactor the UI part of this to tell what changed
            // so that we don't need to diff manually

            for ((modifiedGroup, initialGroup) in modifiedChannelGroups.associateWith { mod ->
                initialChannelGroups.find { mod.id == it.id }
            }) {
                if (initialGroup?.channels != modifiedGroup.channels) {
                    // search for channels that were remove from the group
                    for (initialChannelId in initialGroup?.channels.orEmpty()) {
                        if (initialChannelId !in modifiedGroup.channels) {
                            UserDataRepositoryHelper.userDataRepository
                                .removeFromSubscriptionGroup(
                                    modifiedGroup.id,
                                    initialChannelId
                                )
                        }
                    }

                    // search for channels that were added to the group
                    for (modifiedChannelId in modifiedGroup.channels) {
                        if (modifiedChannelId !in initialGroup?.channels.orEmpty()) {
                            UserDataRepositoryHelper.userDataRepository
                                .addToSubscriptionGroup(
                                    modifiedGroup.id,
                                    modifiedChannelId
                                )
                        }
                    }
                }
            }
        }
    }
}
