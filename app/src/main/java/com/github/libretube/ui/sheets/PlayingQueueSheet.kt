package com.github.libretube.ui.sheets

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.QueueBottomSheetBinding
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.WatchPosition
import com.github.libretube.extensions.setActionListener
import com.github.libretube.extensions.toID
import com.github.libretube.ui.adapters.PlayingQueueAdapter
import com.github.libretube.ui.dialogs.AddToPlaylistDialog
import com.github.libretube.util.PlayingQueue
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        val adapter = PlayingQueueAdapter { videoId ->
            setFragmentResult(PLAYING_QUEUE_REQUEST_KEY, bundleOf(IntentData.videoId to videoId))
        }
        binding.optionsRecycler.adapter = adapter

        // scroll to the currently playing video in the queue
        val currentPlayingIndex = PlayingQueue.currentIndex()
        if (currentPlayingIndex != -1) binding.optionsRecycler.scrollToPosition(currentPlayingIndex)

        binding.addToPlaylist.setOnClickListener {
            AddToPlaylistDialog().show(childFragmentManager, null)
        }

        binding.repeat.setOnClickListener {
            // select the next available repeat mode
            PlayingQueue.repeatMode = when (PlayingQueue.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            updateRepeatButton()
        }
        updateRepeatButton()

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

        binding.optionsRecycler.setActionListener(
            allowSwipe = true,
            allowDrag = true,
            onDismissedListener = { position ->
                if (position == PlayingQueue.currentIndex()) {
                    adapter.notifyItemChanged(position)
                    return@setActionListener
                }
                PlayingQueue.remove(position)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, adapter.itemCount)
            },
            onDragListener = { from, to ->
                PlayingQueue.move(from, to)
                adapter.notifyItemMoved(from, to)
            }
        )
    }

    private fun updateRepeatButton() {
        binding.repeat.alpha = if (PlayingQueue.repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f
        val drawableResource = if (PlayingQueue.repeatMode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat
        binding.repeat.setImageResource(drawableResource)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showSortDialog() {
        val sortOptions = listOf(
            R.string.creation_date,
            R.string.most_views,
            R.string.uploader_name,
            R.string.shuffle,
            R.string.tooltip_reverse
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
                    3 -> {
                        val streams = PlayingQueue.getStreams()
                        val currentIndex = PlayingQueue.currentIndex()

                        // save all streams that need to be shuffled to a copy of the list
                        val toShuffle = streams.filterIndexed { queueIndex, _ ->
                            queueIndex > currentIndex
                        }

                        // create a new list by replacing the old queue-end with the new, shuffled one
                        streams
                            .filter { it !in toShuffle }
                            .plus(toShuffle.shuffled())
                    }
                    4 -> PlayingQueue.getStreams().reversed()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val PLAYING_QUEUE_REQUEST_KEY = "playing_queue_request_key"
    }
}
