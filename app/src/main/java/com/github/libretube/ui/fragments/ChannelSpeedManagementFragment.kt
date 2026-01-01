package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.databinding.FragmentChannelSpeedManagementBinding
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.ui.adapters.ChannelSpeedAdapter
import com.github.libretube.ui.adapters.ChannelSpeedItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelSpeedManagementFragment : Fragment(R.layout.fragment_channel_speed_management) {
    private var _binding: FragmentChannelSpeedManagementBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChannelSpeedAdapter
    private val channelSpeedItems = mutableListOf<ChannelSpeedItem>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChannelSpeedManagementBinding.bind(view)

        setupRecyclerView()
        loadChannelSpeeds()
    }

    private fun setupRecyclerView() {
        adapter = ChannelSpeedAdapter(
            onSpeedChanged = { channelId, speed ->
                PlayerHelper.saveChannelPlaybackSpeed(channelId, speed)
            },
            onDelete = { channelId ->
                showDeleteConfirmation(channelId)
            }
        )

        binding.channelSpeedRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.channelSpeedRecycler.adapter = adapter

        adapter.registerAdapterDataObserver(object : androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                updateEmptyState()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                updateEmptyState()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                updateEmptyState()
            }
        })
    }

    private fun updateEmptyState() {
        val isEmpty = adapter.itemCount == 0
        binding.emptyState.isVisible = isEmpty
        binding.channelSpeedRecycler.isGone = isEmpty
    }

    private fun showLoading(show: Boolean) {
        binding.loadingIndicator.isVisible = show
        binding.channelSpeedRecycler.isGone = show
        binding.emptyState.isGone = show
    }

    private fun loadChannelSpeeds() = lifecycleScope.launch {
        try {
            showLoading(true)

            val savedSpeeds = PlayerHelper.getAllSavedChannelSpeeds()

            if (savedSpeeds.isEmpty()) {
                showLoading(false)
                updateEmptyState()
                return@launch
            }

            channelSpeedItems.clear()
            savedSpeeds.forEach { (channelId, speed) ->
                try {
                    val channel = withContext(Dispatchers.IO) {
                        MediaServiceRepository.instance.getChannel(channelId)
                    }
                    channelSpeedItems.add(
                        ChannelSpeedItem(
                            channelId = channelId,
                            channelName = channel.name,
                            speed = speed
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch channel name for $channelId: ${e.message}")
                    channelSpeedItems.add(
                        ChannelSpeedItem(
                            channelId = channelId,
                            channelName = null,
                            speed = speed
                        )
                    )
                }
            }

            channelSpeedItems.sortBy { it.channelName ?: it.channelId }

            adapter.submitList(channelSpeedItems.toList())
            
            showLoading(false)
            updateEmptyState()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading channel speeds: ${e.message}")
            showLoading(false)
            requireContext().toastFromMainDispatcher(getString(R.string.error_loading_channel_speeds))
            updateEmptyState()
        }
    }

    private fun showDeleteConfirmation(channelId: String) {
        val item = channelSpeedItems.find { it.channelId == channelId }
        val channelName = item?.channelName ?: channelId

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_channel_speed)
            .setMessage(getString(R.string.delete_channel_speed_confirmation, channelName))
            .setPositiveButton(R.string.delete_channel_speed_button) { _, _ ->
                deleteChannelSpeed(channelId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteChannelSpeed(channelId: String) {
        PlayerHelper.removeChannelPlaybackSpeed(channelId)
        
        channelSpeedItems.removeAll { it.channelId == channelId }
        
        adapter.submitList(channelSpeedItems.toList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ChannelSpeedManagement"
    }
}
