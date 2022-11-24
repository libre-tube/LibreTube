package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.FragmentWatchHistoryBinding
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.extensions.awaitQuery
import com.github.libretube.extensions.toDp
import com.github.libretube.ui.adapters.WatchHistoryAdapter
import com.github.libretube.ui.base.BaseFragment
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.util.ProxyHelper

class WatchHistoryFragment : BaseFragment() {
    private lateinit var binding: FragmentWatchHistoryBinding

    private val playerViewModel: PlayerViewModel by activityViewModels()

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
                bottom = if (it) (64).toDp(resources).toInt() else 0
            )
        }

        val watchHistory = awaitQuery {
            Database.watchHistoryDao().getAll()
        }

        if (watchHistory.isEmpty()) return

        watchHistory.forEach {
            it.thumbnailUrl = ProxyHelper.rewriteUrl(it.thumbnailUrl)
            it.uploaderAvatar = ProxyHelper.rewriteUrl(it.uploaderAvatar)
        }

        // reversed order
        binding.watchHistoryRecView.layoutManager = LinearLayoutManager(requireContext()).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        val watchHistoryAdapter = WatchHistoryAdapter(
            watchHistory.toMutableList()
        )

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

        // observe changes
        watchHistoryAdapter.registerAdapterDataObserver(object :
                RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    if (watchHistoryAdapter.itemCount == 0) {
                        binding.watchHistoryRecView.visibility = View.GONE
                        binding.historyEmpty.visibility = View.VISIBLE
                    }
                }
            })

        binding.watchHistoryRecView.adapter = watchHistoryAdapter
        binding.historyEmpty.visibility = View.GONE
        binding.watchHistoryRecView.visibility = View.VISIBLE
    }
}
