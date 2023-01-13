package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.databinding.FragmentAudioPlayerBinding
import com.github.libretube.extensions.toID
import com.github.libretube.ui.base.BaseFragment
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper
import com.github.libretube.util.PlayingQueue

class AudioPlayerFragment : BaseFragment() {
    private lateinit var binding: FragmentAudioPlayerBinding
    private val onTrackChangeListener: (StreamItem) -> Unit = {
        updateStreamInfo()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAudioPlayerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.prev.setOnClickListener {
            val currentIndex = PlayingQueue.currentIndex()
            PlayingQueue.onQueueItemSelected(currentIndex - 1)
        }

        binding.next.setOnClickListener {
            val currentIndex = PlayingQueue.currentIndex()
            PlayingQueue.onQueueItemSelected(currentIndex + 1)
        }

        PlayingQueue.addOnTrackChangedListener(onTrackChangeListener)

        updateStreamInfo()
    }

    private fun updateStreamInfo() {
        val current = PlayingQueue.getCurrent()
        current ?: return

        binding.title.text = current.title
        binding.uploader.text = current.uploaderName
        binding.uploader.setOnClickListener {
            NavigationHelper.navigateChannel(requireContext(), current.uploaderUrl?.toID())
        }

        ImageHelper.loadImage(current.thumbnail, binding.thumbnail)
    }

    override fun onDestroy() {
        super.onDestroy()

        // unregister the listener
        PlayingQueue.removeOnTrackChangedListener(onTrackChangeListener)
    }
}
