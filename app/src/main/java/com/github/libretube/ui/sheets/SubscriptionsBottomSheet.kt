package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.github.libretube.R
import com.github.libretube.api.obj.Subscription
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.SheetSubscriptionsBinding
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.adapters.SubscriptionChannelAdapter
import com.github.libretube.ui.models.EditChannelGroupsModel
import com.github.libretube.ui.models.SubscriptionsViewModel
import java.util.Locale

class SubscriptionsBottomSheet : ExpandedBottomSheet(R.layout.sheet_subscriptions) {
    private var _binding: SheetSubscriptionsBinding? = null
    private val binding get() = _binding!!
    private val adapter = SubscriptionChannelAdapter()

    private val selectedChannelGroup
        get() = PreferenceHelper.getInt(PreferenceKeys.SELECTED_CHANNEL_GROUP, 0)

    private val searchInputText
        get() = binding.subscriptionsSearchInput.text.toString()

    private val viewModel: SubscriptionsViewModel by activityViewModels()
    private val channelGroupsModel: EditChannelGroupsModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = SheetSubscriptionsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.channelsRecycler.adapter = adapter

        initHeaderLayout()

        binding.subscriptionsSearchInput.addTextChangedListener { query ->
            showFilteredSubscriptions(query.toString())
        }

        viewModel.subscriptions.observe(viewLifecycleOwner) {
            showFilteredSubscriptions()
        }
    }

    private fun initHeaderLayout(){
        binding.allSubsBtn.text = String.format(
            Locale.getDefault(),
            "%s (%d)",
            ContextCompat.getString(requireContext(), R.string.subscriptions),
            viewModel.subscriptions.value?.size
        )
        binding.allSubsBtn.isToggleCheckedStateOnClick = false
        binding.allSubsBtn.setOnClickListener {
            binding.groupEditBtn.isVisible = false

            showFilteredSubscriptions()
        }

        channelGroupsModel.groups.value
            ?.getOrNull(selectedChannelGroup - 1)
            ?.let { channelGroup ->
            binding.allSubsBtn.isToggleCheckedStateOnClick = true
            binding.allSubsBtn.text = String.format(
                Locale.getDefault(),
                "%s (%d)",
                ContextCompat.getString(requireContext(), R.string.all),
                viewModel.subscriptions.value?.size
            )

            binding.groupSubsBtn.isVisible = true
            binding.groupSubsBtn.isChecked = true
            binding.groupSubsBtn.text = String.format(
                Locale.getDefault(),
                "%s (%d)",
                channelGroup.name,
                channelGroup.channels.size
            )
            binding.groupSubsBtn.setOnClickListener {
                binding.groupEditBtn.isVisible = true

                showFilteredSubscriptions()
            }

            binding.groupEditBtn.isVisible = true
            binding.groupEditBtn.setOnClickListener {
                channelGroupsModel.groupToEdit = channelGroupsModel.groups.value
                    ?.getOrNull(selectedChannelGroup - 1)?.also {
                        dismiss()
                        EditChannelGroupSheet().show(parentFragmentManager, null)
                    }
            }
        }
    }

    private fun showFilteredSubscriptions(query: String? = null) {
        val loweredQuery = (query?: searchInputText).lowercase()

        val shouldFilterByGroup = binding.groupSubsBtn.isChecked
        val filteredSubscriptions = viewModel.subscriptions.value.orEmpty()
            .filterByGroup(if (shouldFilterByGroup) selectedChannelGroup else 0)
            .filter { it.name.lowercase().contains(loweredQuery) }

        adapter.submitList(filteredSubscriptions)
    }

    private fun List<Subscription>.filterByGroup(groupIndex: Int): List<Subscription> {
        if (groupIndex == 0) return this

        val group = channelGroupsModel.groups.value?.getOrNull(groupIndex - 1)
        if (group == null) return this

        return filter { group.channels.contains(it.url.toID()) != false }
    }
}