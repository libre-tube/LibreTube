package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.obj.Subscription
import com.github.libretube.databinding.DialogEditChannelGroupBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.ui.adapters.SubscriptionGroupChannelsAdapter
import com.github.libretube.ui.models.EditChannelGroupsModel
import com.github.libretube.ui.models.SubscriptionsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class EditChannelGroupSheet : ExpandedBottomSheet(R.layout.dialog_edit_channel_group) {
    private var _binding: DialogEditChannelGroupBinding? = null
    private val binding get() = _binding!!

    private val subscriptionsModel: SubscriptionsViewModel by activityViewModels()
    private val channelGroupsModel: EditChannelGroupsModel by activityViewModels()
    private var channels = listOf<Subscription>()

    private lateinit var channelsAdapter: SubscriptionGroupChannelsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = DialogEditChannelGroupBinding.bind(view)

        channelsAdapter = SubscriptionGroupChannelsAdapter(
            channelGroupsModel.groupToEdit!!
        ) {
            channelGroupsModel.groupToEdit = it
            updateConfirmStatus()
        }

        binding.channelsRV.adapter = channelsAdapter
        binding.groupName.setText(channelGroupsModel.groupToEdit?.name)
        val oldGroupName = channelGroupsModel.groupToEdit?.name.orEmpty()

        binding.channelsRV.layoutManager = LinearLayoutManager(context)

        binding.groupName.addTextChangedListener {
            updateConfirmStatus()
        }

        binding.searchInput.addTextChangedListener {
            showChannels(channels, it?.toString())
        }

        binding.cancel.setOnClickListener {
            dismiss()
        }

        updateConfirmStatus()
        binding.confirm.setOnClickListener {
            val updatedGroup = channelGroupsModel.groupToEdit?.copy(
                name = binding.groupName.text.toString().ifEmpty { return@setOnClickListener }
            ) ?: return@setOnClickListener
            saveGroup(updatedGroup, oldGroupName)

            dismiss()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch(Dispatchers.IO) {
                    subscriptionsModel.fetchSubscriptions(requireContext())
                }
                launch {
                    subscriptionsModel.subscriptions.asFlow().collectLatest { subscriptions ->
                        subscriptions?.let {
                            channels = it
                            showChannels(it, null)
                        }
                    }
                }
            }
        }
    }

    private fun saveGroup(group: SubscriptionGroup, oldGroupName: String) {
        // delete the old instance if the group already existed and add the updated/new one
        channelGroupsModel.groups.value = channelGroupsModel.groups.value
            ?.filter { it.name != oldGroupName }
            ?.plus(group)

        CoroutineScope(Dispatchers.IO).launch {
            // delete the old version of the group first before updating it, as the name is the
            // primary key
            DatabaseHolder.Database.subscriptionGroupsDao().deleteGroup(oldGroupName)
            DatabaseHolder.Database.subscriptionGroupsDao().createGroup(group)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showChannels(channels: List<Subscription>, query: String?) {
        binding.subscriptionsContainer.isVisible = true
        binding.progress.isVisible = false

        channelsAdapter.submitList(
            channels.filter { query == null || it.name.lowercase().contains(query.lowercase()) }
        )
    }

    private fun updateConfirmStatus() {
        with(binding) {
            val name = groupName.text.toString()
            groupName.error = getGroupNameError(name)

            confirm.isEnabled = groupName.error == null && !channelGroupsModel.groupToEdit?.channels.isNullOrEmpty()
        }
    }

    private fun getGroupNameError(name: String): String? {
        if (name.isBlank()) {
            return getString(R.string.group_name_error_empty)
        }

        val groupExists = runBlocking(Dispatchers.IO) {
            DatabaseHolder.Database.subscriptionGroupsDao().exists(name)
        }
        if (groupExists && channelGroupsModel.groupToEdit?.name != name) {
            return getString(R.string.group_name_error_exists)
        }

        return null
    }
}
