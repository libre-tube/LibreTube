package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.Subscription
import com.github.libretube.databinding.DialogEditChannelGroupBinding
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.ui.adapters.SubscriptionGroupChannelsAdapter
import com.github.libretube.ui.models.SubscriptionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditChannelGroupSheet(
    private var group: SubscriptionGroup,
    private val onGroupChanged: (SubscriptionGroup) -> Unit
) : ExpandedBottomSheet() {
    private val subscriptionsModel: SubscriptionsViewModel by activityViewModels()
    private lateinit var binding: DialogEditChannelGroupBinding
    private var channels: List<Subscription> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogEditChannelGroupBinding.inflate(layoutInflater)
        binding.groupName.setText(group.name)

        binding.channelsRV.layoutManager = LinearLayoutManager(context)
        fetchSubscriptions()

        binding.searchInput.addTextChangedListener {
            showChannels(channels, it?.toString())
        }

        binding.cancel.setOnClickListener {
            dismiss()
        }

        binding.confirm.setOnClickListener {
            group.name = binding.groupName.text.toString()
            if (group.name.isBlank()) return@setOnClickListener
            onGroupChanged(group)
            dismiss()
        }

        return binding.root
    }

    private fun fetchSubscriptions() {
        subscriptionsModel.subscriptions.value?.let {
            channels = it
            showChannels(it, null)
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            channels = runCatching {
                SubscriptionHelper.getSubscriptions()
            }.getOrNull().orEmpty()
            withContext(Dispatchers.Main) {
                showChannels(channels, null)
            }
        }
    }

    private fun showChannels(channels: List<Subscription>, query: String?) {
        binding.channelsRV.adapter = SubscriptionGroupChannelsAdapter(
            channels.filter { query == null || it.name.lowercase().contains(query.lowercase()) },
            group
        ) {
            group = it
        }
        binding.subscriptionsContainer.isVisible = true
        binding.progress.isVisible = false
    }
}
