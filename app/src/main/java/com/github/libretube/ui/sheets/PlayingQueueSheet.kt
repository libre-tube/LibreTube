package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.QueueBottomSheetBinding
import com.github.libretube.ui.adapters.PlayingQueueAdapter
import com.github.libretube.util.PlayingQueue
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlayingQueueSheet : BottomSheetDialogFragment() {
    private lateinit var binding: QueueBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = QueueBottomSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionsRecycler.layoutManager = LinearLayoutManager(context)
        val adapter = PlayingQueueAdapter()
        binding.optionsRecycler.adapter = adapter

        binding.shuffle.setOnClickListener {
            val streams = PlayingQueue.getStreams()
            val size =  PlayingQueue.size()
            streams.subList(PlayingQueue.currentIndex(), size).shuffle()
            adapter.notifyItemRangeChanged(0, size)
        }

        binding.clear.setOnClickListener {
            val streams = PlayingQueue.getStreams()
            val currentIndex = PlayingQueue.currentIndex()
            val size = PlayingQueue.size()
            streams.subList(currentIndex, size).clear()
            adapter.notifyItemRangeRemoved(currentIndex + 1, size)
        }

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

                adapter.notifyItemMoved(from, to)
                PlayingQueue.move(from, to)
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
