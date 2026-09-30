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
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.repo.UserDataRepositoryHelper
import com.github.libretube.ui.adapters.SubscriptionGroupChannelsAdapter
import com.github.libretube.ui.models.SubscriptionsViewModel
import com.github.libretube.ui.sheets.AddChannelToGroupSheet.Companion.applyChannelGroupsDiff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditChannelGroupSheet : ExpandedBottomSheet(R.layout.dialog_edit_channel_group) {
    private var _binding: DialogEditChannelGroupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubscriptionsViewModel by activityViewModels()
    private var channels = emptyList<Subscription>()
    private var channelsInGroup = emptyList<String>()

    private lateinit var channelsAdapter: SubscriptionGroupChannelsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = DialogEditChannelGroupBinding.bind(view)

        channelsInGroup = viewModel.groupToEdit?.channels.orEmpty()
        channelsAdapter = SubscriptionGroupChannelsAdapter(
            viewModel.groupToEdit!!.copy()
        ) {
            channelsInGroup = it
            updateConfirmStatus()
        }

        binding.channelsRV.adapter = channelsAdapter
        binding.groupName.setText(viewModel.groupToEdit?.name)
        val oldGroupName = viewModel.groupToEdit?.name.orEmpty()

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
            val updatedGroup = viewModel.groupToEdit?.copy(
                name = binding.groupName.text.toString().ifEmpty { return@setOnClickListener }
            ) ?: return@setOnClickListener
            saveGroup(updatedGroup, oldGroupName)

            dismiss()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch(Dispatchers.IO) {
                    viewModel.fetchSubscriptions(requireContext())
                }
                launch {
                    viewModel.subscriptions.asFlow().collectLatest { subscriptions ->
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
        val context = requireContext().applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                group.id =
                    UserDataRepositoryHelper.userDataRepository.createSubscriptionGroup(group.name)
                applyChannelGroupsDiff(group.id, group.channels, channelsInGroup)
                group.channels = channelsInGroup

                viewModel.groups.postValue(
                    viewModel.groups.value
                        ?.filter { it.name != oldGroupName }
                        ?.plus(group)
                )
            } catch (e: Exception) {
                context.toastFromMainDispatcher(e.message.orEmpty())
            }
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

            confirm.isEnabled = groupName.error == null && !channelsInGroup.isEmpty()
        }
    }

    private fun getGroupNameError(name: String): String? {
        if (name.isBlank()) {
            return getString(R.string.group_name_error_empty)
        }

        // TODO: either remove this check or figure out how to support this for LibreTube sync server
        //        val groupExists = runBlocking(Dispatchers.IO) {
        //            DatabaseHolder.Database.subscriptionGroupsDao().exists(name)
        //        }
        //        if (groupExists && viewModel.groupToEdit?.name != name) {
        //            return getString(R.string.group_name_error_exists)
        //        }

        return null
    }
}
