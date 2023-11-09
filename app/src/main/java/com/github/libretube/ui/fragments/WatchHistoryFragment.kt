package com.github.libretube.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.postDelayed
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentWatchHistoryBinding
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.extensions.dpToPx
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ProxyHelper
import com.github.libretube.ui.adapters.WatchHistoryAdapter
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.ui.sheets.BaseBottomSheet
import com.github.libretube.util.PlayingQueue
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class WatchHistoryFragment : Fragment() {
    private var _binding: FragmentWatchHistoryBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val playerViewModel: PlayerViewModel by activityViewModels()
    private var isLoading = false

    private var selectedStatusFilter = PreferenceHelper.getInt(PreferenceKeys.SELECTED_HISTORY_STATUS_FILTER, 0)
        set(value) {
            PreferenceHelper.putInt(PreferenceKeys.SELECTED_HISTORY_STATUS_FILTER, value)
            field = value
        }
    private var selectedTypeFilter = PreferenceHelper.getInt(PreferenceKeys.SELECTED_HISTORY_TYPE_FILTER, 0)
        set(value) {
            PreferenceHelper.putInt(PreferenceKeys.SELECTED_HISTORY_TYPE_FILTER, value)
            field = value
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerViewModel.isMiniPlayerVisible.observe(viewLifecycleOwner) {
            _binding?.watchHistoryRecView?.updatePadding(bottom = if (it) 64f.dpToPx() else 0)
        }

        val allHistory = runBlocking(Dispatchers.IO) {
            Database.watchHistoryDao().getAll().reversed()
        }

        if (allHistory.isEmpty()) return


        binding.filterTypeTV.text = resources.getStringArray(R.array.filterOptions)[selectedTypeFilter]
        binding.filterStatusTV.text = resources.getStringArray(R.array.filterStatusOptions)[selectedStatusFilter]

        binding.clear.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_history)
                .setMessage(R.string.irreversible)
                .setPositiveButton(R.string.okay) { _, _ ->
                    binding.historyScrollView.isGone = true
                    binding.historyEmpty.isVisible = true
                    lifecycleScope.launch(Dispatchers.IO) {
                        Database.watchHistoryDao().deleteAll()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        binding.filterTypeTV.setOnClickListener {
            val filterOptions = resources.getStringArray(R.array.filterOptions)

            BaseBottomSheet().apply {
                setSimpleItems(filterOptions.toList()) { index ->
                    binding.filterTypeTV.text = filterOptions[index]
                    selectedTypeFilter = index
                    showWatchHistory(allHistory)
                }
            }.show(childFragmentManager)
        }

        binding.filterStatusTV.setOnClickListener {
            val filterOptions = resources.getStringArray(R.array.filterStatusOptions)

            BaseBottomSheet().apply {
                setSimpleItems(filterOptions.toList()) { index ->
                    binding.filterStatusTV.text = filterOptions[index]
                    selectedStatusFilter = index
                    showWatchHistory(allHistory)
                }
            }.show(childFragmentManager)
        }
        showWatchHistory(allHistory)
    }

    private fun showWatchHistory( allHistory: List<WatchHistoryItem> ) {

        val watchHistory = allHistory.filterByStatusAndWatchPosition()

        watchHistory.forEach {
            it.thumbnailUrl = ProxyHelper.rewriteUrl(it.thumbnailUrl)
            it.uploaderAvatar = ProxyHelper.rewriteUrl(it.uploaderAvatar)
        }

        val watchHistoryAdapter = WatchHistoryAdapter(
            watchHistory.toMutableList()
        )

        binding.playAll.setOnClickListener {
            PlayingQueue.resetToDefaults()
            PlayingQueue.add(
                *watchHistory.reversed().map {
                    StreamItem(
                        url = "/watch?v=${it.videoId}",
                        title = it.title,
                        thumbnail = it.thumbnailUrl,
                        uploaderName = it.uploader,
                        uploaderUrl = it.uploaderUrl,
                        uploaderAvatar = it.uploaderAvatar,
                        uploadedDate = it.uploadDate?.toString(),
                        duration = it.duration
                    )
                }.toTypedArray()
            )
            NavigationHelper.navigateVideo(
                requireContext(),
                watchHistory.last().videoId,
                keepQueue = true
            )
        }

        binding.watchHistoryRecView.layoutManager = LinearLayoutManager(context)
        binding.watchHistoryRecView.adapter = watchHistoryAdapter
        binding.historyEmpty.isGone = true
        binding.historyScrollView.isVisible = true

        val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                watchHistoryAdapter.removeFromWatchHistory(position)
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchHelper.attachToRecyclerView(binding.watchHistoryRecView)

        // observe changes to indicate if the history is empty
        watchHistoryAdapter.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                if (watchHistoryAdapter.itemCount == 0) {
                    binding.historyScrollView.isGone = true
                    binding.historyEmpty.isVisible = true
                }
            }
        })

        // add a listener for scroll end, delay needed to prevent loading new ones the first time
        handler.postDelayed(200) {
            if (_binding == null) return@postDelayed
            binding.historyScrollView.viewTreeObserver.addOnScrollChangedListener {
                if (_binding?.historyScrollView?.canScrollVertically(1) == false &&
                    !isLoading
                ) {
                    isLoading = true
                    watchHistoryAdapter.showMoreItems()
                    isLoading = false
                }
            }
        }
    }

    private fun List<WatchHistoryItem>.filterByStatusAndWatchPosition(): List<WatchHistoryItem> {
        val watchHistoryItem = this.filter {
            val isLive = (it.duration ?: -1L) < 0L
            when (selectedTypeFilter) {
                0 -> true
                1 -> !it.isShort && !isLive
                2 -> it.isShort // where is the StreamItem converted to watchHistoryItem?
                3 -> isLive
                else -> throw IllegalArgumentException()
            }
        }

        if (selectedStatusFilter == 0) {
            return watchHistoryItem
        }

        return runBlocking {
            when (selectedStatusFilter) {
                1 -> DatabaseHelper.filterUnwatchedHistory(watchHistoryItem)
                2 -> DatabaseHelper.filterWatchedHistory(watchHistoryItem)
                else -> throw IllegalArgumentException()
            } }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
