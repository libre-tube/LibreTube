package com.github.libretube.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.postDelayed
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.databinding.FragmentWatchHistoryBinding
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.extensions.dpToPx
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.ProxyHelper
import com.github.libretube.ui.adapters.WatchHistoryAdapter
import com.github.libretube.ui.base.BaseFragment
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.util.PlayingQueue
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class WatchHistoryFragment : BaseFragment() {
    private lateinit var binding: FragmentWatchHistoryBinding

    private val playerViewModel: PlayerViewModel by activityViewModels()
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWatchHistoryBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerViewModel.isMiniPlayerVisible.observe(viewLifecycleOwner) {
            binding.watchHistoryRecView.updatePadding(
                bottom = if (it) (64).dpToPx().toInt() else 0
            )
        }

        val watchHistory = runBlocking(Dispatchers.IO) {
            Database.watchHistoryDao().getAll().reversed()
        }

        if (watchHistory.isEmpty()) return

        watchHistory.forEach {
            it.thumbnailUrl = ProxyHelper.rewriteUrl(it.thumbnailUrl)
            it.uploaderAvatar = ProxyHelper.rewriteUrl(it.uploaderAvatar)
        }

        binding.clear.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_history)
                .setMessage(R.string.irreversible)
                .setPositiveButton(R.string.okay) { _, _ ->
                    binding.historyScrollView.visibility = View.GONE
                    binding.historyEmpty.visibility = View.VISIBLE
                    lifecycleScope.launch(Dispatchers.IO) {
                        Database.watchHistoryDao().deleteAll()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

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
                        uploadedDate = it.uploadDate,
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

        val watchHistoryAdapter = WatchHistoryAdapter(
            watchHistory.toMutableList()
        )

        binding.watchHistoryRecView.layoutManager = LinearLayoutManager(context)
        binding.watchHistoryRecView.adapter = watchHistoryAdapter
        binding.historyEmpty.visibility = View.GONE
        binding.historyScrollView.visibility = View.VISIBLE

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

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
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
                    binding.historyScrollView.visibility = View.GONE
                    binding.historyEmpty.visibility = View.VISIBLE
                }
            }
        })

        // add a listener for scroll end, delay needed to prevent loading new ones the first time
        Handler(Looper.getMainLooper()).postDelayed(200) {
            binding.historyScrollView.viewTreeObserver.addOnScrollChangedListener {
                if (!binding.historyScrollView.canScrollVertically(1) && !isLoading) {
                    isLoading = true
                    watchHistoryAdapter.showMoreItems()
                    isLoading = false
                }
            }
        }
    }
}
