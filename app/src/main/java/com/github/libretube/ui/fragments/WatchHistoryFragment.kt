package com.github.libretube.ui.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.withTransaction
import com.github.libretube.R
import com.github.libretube.databinding.FragmentWatchHistoryBinding
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.extensions.ceilHalf
import com.github.libretube.extensions.dpToPx
import com.github.libretube.extensions.setOnDismissListener
import com.github.libretube.helpers.NavBarHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.adapters.WatchHistoryAdapter
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.ui.extensions.addOnBottomReachedListener
import com.github.libretube.ui.extensions.setupFragmentAnimation
import com.github.libretube.ui.models.CommonPlayerViewModel
import com.github.libretube.ui.models.WatchHistoryModel
import com.github.libretube.util.PlayingQueue
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchHistoryFragment : DynamicLayoutManagerFragment(R.layout.fragment_watch_history) {
    private var _binding: FragmentWatchHistoryBinding? = null
    private val binding get() = _binding!!

    private val commonPlayerViewModel: CommonPlayerViewModel by activityViewModels()
    private var recyclerViewState: Parcelable? = null

    private val viewModel: WatchHistoryModel by viewModels()
    private val watchHistoryAdapter = WatchHistoryAdapter()

    override fun setLayoutManagers(gridItems: Int) {
        _binding?.watchHistoryRecView?.layoutManager =
            GridLayoutManager(context, gridItems.ceilHalf())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentWatchHistoryBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        commonPlayerViewModel.isMiniPlayerVisible.observe(viewLifecycleOwner) { isMiniPlayerVisible ->
            _binding?.watchHistoryRecView?.updatePadding(bottom = if (isMiniPlayerVisible) 64f.dpToPx() else 0)
            _binding?.playAll?.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = (if (isMiniPlayerVisible) 64f else 16f).dpToPx()
            }
        }

        binding.watchHistoryRecView.setOnDismissListener { position ->
            val item = viewModel.filteredWatchHistory.value?.getOrNull(position)
                ?: return@setOnDismissListener
            viewModel.removeFromHistory(item)
        }

        // observe changes to indicate if the history is empty
        watchHistoryAdapter.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                if (watchHistoryAdapter.itemCount == 0) {
                    binding.watchHistoryRecView.isGone = true
                    binding.historyEmpty.isVisible = true
                }
            }
        })

        binding.watchHistoryRecView.adapter = watchHistoryAdapter

        // manually restore the recyclerview state due to https://github.com/material-components/material-components-android/issues/3473
        binding.watchHistoryRecView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                recyclerViewState = binding.watchHistoryRecView.layoutManager?.onSaveInstanceState()
            }
        })

        binding.chipContinue.isChecked = viewModel.selectedStatusFilter in arrayOf(0, 1)
        binding.chipFinished.isChecked = viewModel.selectedStatusFilter in arrayOf(0, 2)

        val watchPositionItem = arrayOf(getString(R.string.also_clear_watch_positions))
        val selected = booleanArrayOf(false)

        binding.clear.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_history)
                .setMultiChoiceItems(watchPositionItem, selected) { _, index, newValue ->
                    selected[index] = newValue
                }
                .setPositiveButton(R.string.okay) { _, _ ->
                    binding.watchHistoryRecView.isGone = true
                    binding.historyEmpty.isVisible = true
                    binding.clear.isVisible = true
                    binding.playAll.isGone = true
                    binding.statusFilterChips.isGone = true

                    lifecycleScope.launch(Dispatchers.IO) {
                        Database.withTransaction {
                            Database.watchHistoryDao().deleteAll()
                            if (selected[0]) Database.watchPositionDao().deleteAll()
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        binding.statusFilterChips.setOnCheckedStateChangeListener { _, checkedIds ->
            val continueWatchingEnabled = checkedIds.contains(binding.chipContinue.id)
            val finishedEnabled = checkedIds.contains(binding.chipFinished.id)
            viewModel.selectedStatusFilter = when {
                continueWatchingEnabled && finishedEnabled -> 0
                continueWatchingEnabled -> 1
                finishedEnabled -> 2
                else -> 0
            }
        }

        binding.playAll.setOnClickListener {
            val history = viewModel.filteredWatchHistory.value.orEmpty()
            if (history.isEmpty()) return@setOnClickListener

            PlayingQueue.add(
                *history.reversed().map(WatchHistoryItem::toStreamItem).toTypedArray()
            )
            NavigationHelper.navigateVideo(
                requireContext(),
                history.last().videoId,
                keepQueue = true
            )
        }

        viewModel.filteredWatchHistory.observe(viewLifecycleOwner) { history ->
            binding.historyEmpty.isGone = history.isNotEmpty()
            binding.watchHistoryRecView.isVisible = history.isNotEmpty()
            binding.clear.isVisible = history.isNotEmpty()
            binding.playAll.isVisible = history.isNotEmpty()

            watchHistoryAdapter.submitList(history)
        }

        viewModel.fetchNextPage()

        binding.watchHistoryRecView.addOnBottomReachedListener {
            viewModel.fetchNextPage()
        }

        if (NavBarHelper.getStartFragmentId(requireContext()) != R.id.watchHistoryFragment) {
            setupFragmentAnimation(binding.root)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val hasItems = Database.watchHistoryDao().getSize() != 0

            withContext(Dispatchers.Main) {
                binding.clear.isEnabled = hasItems
            }
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // manually restore the recyclerview state due to https://github.com/material-components/material-components-android/issues/3473
        binding.watchHistoryRecView.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
