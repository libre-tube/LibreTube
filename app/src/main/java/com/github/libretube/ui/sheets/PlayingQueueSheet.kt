package com.github.libretube.ui.sheets

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.QueueBottomSheetBinding
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.WatchPosition
import com.github.libretube.extensions.toID
import com.github.libretube.ui.adapters.PlayingQueueAdapter
import com.github.libretube.ui.dialogs.AddToPlaylistDialog
import com.github.libretube.util.PlayingQueue
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.IllegalArgumentException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayingQueueSheet : ExpandedBottomSheet() {
    private var _binding: QueueBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = QueueBottomSheetBinding.inflate(layoutInflater)
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

        binding.clearQueue.setOnClickListener {
            val currentIndex = PlayingQueue.currentIndex()
            PlayingQueue.setStreams(
                PlayingQueue.getStreams()
                    .filterIndexed { index, _ -> index == currentIndex }
            )
            adapter.notifyDataSetChanged()
        }
        binding.sort.setOnClickListener {
            showSortDialog()
        }

        binding.dismiss.setOnClickListener {
            dialog?.dismiss()
        }

        binding.watchPositionsOptions.setOnClickListener {
            showWatchPositionsOptions()
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

    @SuppressLint("NotifyDataSetChanged")
    private fun showSortDialog() {
        val sortOptions = listOf(
            R.string.creation_date,
            R.string.most_views,
            R.string.uploader_name
        )
            .map { requireContext().getString(it) }
            .toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sort_by)
            .setItems(sortOptions) { _, index ->
                val newQueue = when (index) {
                    0 -> PlayingQueue.getStreams().sortedBy { it.uploaded }
                    1 -> PlayingQueue.getStreams().sortedBy { it.views }.reversed()
                    2 -> PlayingQueue.getStreams().sortedBy { it.uploaderName }
                    else -> throw IllegalArgumentException()
                }
                PlayingQueue.setStreams(newQueue)
                _binding?.optionsRecycler?.adapter?.notifyDataSetChanged()
            }
            .show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showWatchPositionsOptions() {
        val options = arrayOf(
            getString(R.string.mark_as_watched),
            getString(R.string.mark_as_unwatched),
            getString(R.string.remove_watched_videos)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.watch_positions)
            .setItems(options) { _, index ->
                when (index) {
                    0 -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            PlayingQueue.getStreams().forEach {
                                val videoId = it.url.orEmpty().toID()
                                val duration = it.duration ?: 0
                                val watchPosition = WatchPosition(videoId, duration * 1000)
                                DatabaseHolder.Database.watchPositionDao().insert(watchPosition)
                            }
                        }
                    }

                    1 -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            PlayingQueue.getStreams().forEach {
                                DatabaseHolder.Database.watchPositionDao()
                                    .deleteByVideoId(it.url.orEmpty().toID())
                            }
                        }
                    }

                    2 -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            val currentStream = PlayingQueue.getCurrent()
                            val streams = DatabaseHelper
                                .filterUnwatched(PlayingQueue.getStreams())
                                .toMutableList()
                            if (currentStream != null &&
                                streams.none { it.url?.toID() == currentStream.url?.toID() }
                            ) {
                                streams.add(0, currentStream)
                            }
                            PlayingQueue.setStreams(streams)
                            withContext(Dispatchers.Main) {
                                _binding?.optionsRecycler?.adapter?.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
            .show()
    }
}
