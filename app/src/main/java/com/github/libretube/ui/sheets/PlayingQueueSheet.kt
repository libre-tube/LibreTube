package com.github.libretube.ui.sheets

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.QueueBottomSheetBinding
import com.github.libretube.ui.adapters.PlayingQueueAdapter
import com.github.libretube.ui.dialogs.AddToPlaylistDialog
import com.github.libretube.util.PlayingQueue

class PlayingQueueSheet : ExpandedBottomSheet() {
    private lateinit var binding: QueueBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = QueueBottomSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionsRecycler.layoutManager = LinearLayoutManager(context)
        val adapter = PlayingQueueAdapter()
        binding.optionsRecycler.adapter = adapter

        // scroll to the currently playing video in the queue
        val currentPlayingIndex = PlayingQueue.currentIndex()
        if (currentPlayingIndex != -1) binding.optionsRecycler.scrollToPosition(currentPlayingIndex)

        binding.shuffle.setOnClickListener {
            val streams = PlayingQueue.getStreams().toMutableList()
            val currentIndex = PlayingQueue.currentIndex()

            // save all streams that need to be shuffled to a copy of the list
            val toShuffle = streams.filterIndexed { index, _ ->
                index > currentIndex
            }

            // re-add all streams in the new, shuffled order after removing them
            streams.removeAll(toShuffle)
            streams.addAll(toShuffle.shuffled())

            PlayingQueue.setStreams(streams)

            adapter.notifyDataSetChanged()
        }

        binding.addToPlaylist.setOnClickListener {
            AddToPlaylistDialog().show(childFragmentManager, null)
        }

        binding.reverse.setOnClickListener {
            PlayingQueue.setStreams(PlayingQueue.getStreams().reversed())
            adapter.notifyDataSetChanged()
        }

        binding.repeat.setOnClickListener {
            PlayingQueue.repeatQueue = !PlayingQueue.repeatQueue
            it.alpha = if (PlayingQueue.repeatQueue) 1f else 0.5f
        }

        binding.repeat.alpha = if (PlayingQueue.repeatQueue) 1f else 0.5f

        binding.bottomControls.setOnClickListener {
            dialog?.dismiss()
        }

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.absoluteAdapterPosition
                val to = target.absoluteAdapterPosition

                PlayingQueue.move(from, to)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                if (position == PlayingQueue.currentIndex()) {
                    adapter.notifyItemChanged(position)
                    return
                }
                PlayingQueue.remove(position)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, adapter.itemCount)
            }
        }

        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.optionsRecycler)
    }
}
