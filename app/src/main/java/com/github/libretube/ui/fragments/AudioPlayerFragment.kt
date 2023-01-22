package com.github.libretube.ui.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.databinding.FragmentAudioPlayerBinding
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.toID
import com.github.libretube.obj.ShareData
import com.github.libretube.services.BackgroundMode
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.base.BaseFragment
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.sheets.PlaybackOptionsSheet
import com.github.libretube.ui.sheets.PlayingQueueSheet
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.util.BackgroundHelper
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper
import com.github.libretube.util.PlayingQueue

class AudioPlayerFragment : BaseFragment() {
    private lateinit var binding: FragmentAudioPlayerBinding
    private val onTrackChangeListener: (StreamItem) -> Unit = {
        updateStreamInfo()
    }
    private var handler = Handler(Looper.getMainLooper())
    private var isPaused: Boolean = false

    private var playerService: BackgroundMode? = null

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as BackgroundMode.LocalBinder
            playerService = binder.getService()
            handleServiceConnection()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            val mainActivity = activity as MainActivity
            if (mainActivity.navController.currentDestination?.id == R.id.audioPlayerFragment) {
                mainActivity.navController.popBackStack()
            } else {
                mainActivity.navController.backQueue.removeAll {
                    it.destination.id == R.id.audioPlayerFragment
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(activity, BackgroundMode::class.java).also { intent ->
            activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
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

        // select the title TV in order for it to automatically scroll
        binding.title.isSelected = true
        binding.uploader.isSelected = true

        binding.prev.setOnClickListener {
            val currentIndex = PlayingQueue.currentIndex()
            if (!PlayingQueue.hasPrev()) return@setOnClickListener
            PlayingQueue.onQueueItemSelected(currentIndex - 1)
        }

        binding.next.setOnClickListener {
            val currentIndex = PlayingQueue.currentIndex()
            if (!PlayingQueue.hasNext()) return@setOnClickListener
            PlayingQueue.onQueueItemSelected(currentIndex + 1)
        }

        binding.openQueue.setOnClickListener {
            PlayingQueueSheet().show(childFragmentManager)
        }

        binding.playbackOptions.setOnClickListener {
            playerService?.player?.let {
                PlaybackOptionsSheet(it)
                    .show(childFragmentManager)
            }
        }

        binding.openVideo.setOnClickListener {
            NavigationHelper.navigateVideo(
                context = requireContext(),
                videoId = PlayingQueue.getCurrent()?.url?.toID(),
                timeStamp = playerService?.player?.currentPosition,
                keepQueue = true,
                forceVideo = true
            )
            BackgroundHelper.stopBackgroundPlay(requireContext())
            findNavController().popBackStack()
        }

        binding.share.setOnClickListener {
            val currentVideo = PlayingQueue.getCurrent() ?: return@setOnClickListener
            ShareDialog(
                id = currentVideo.url!!.toID(),
                shareObjectType = ShareObjectType.VIDEO,
                shareData = ShareData(currentVideo = currentVideo.title)
            ).show(childFragmentManager, null)
        }

        binding.thumbnail.setOnClickListener {
            val current = PlayingQueue.getCurrent()
            current?.let {
                VideoOptionsBottomSheet(it.url!!.toID(), it.title!!)
                    .show(childFragmentManager)
            }
        }

        // Listen for track changes due to autoplay or the notification
        PlayingQueue.addOnTrackChangedListener(onTrackChangeListener)

        binding.playPause.setOnClickListener {
            if (isPaused) playerService?.play() else playerService?.pause()
        }

        // load the stream info into the UI
        updateStreamInfo()
    }

    /**
     * Load the information from a new stream into the UI
     */
    private fun updateStreamInfo() {
        val current = PlayingQueue.getCurrent()
        current ?: return

        binding.title.text = current.title
        binding.uploader.text = current.uploaderName
        binding.uploader.setOnClickListener {
            NavigationHelper.navigateChannel(requireContext(), current.uploaderUrl?.toID())
        }

        current.thumbnail?.let { updateThumbnailAsync(it) }

        initializeSeekBar()
    }

    private fun updateThumbnailAsync(thumbnailUrl: String) {
        binding.progress.visibility = View.VISIBLE
        binding.thumbnail.visibility = View.GONE

        ImageHelper.getAsync(requireContext(), thumbnailUrl) {
            binding.thumbnail.setImageBitmap(it)
            binding.thumbnail.visibility = View.VISIBLE
            binding.progress.visibility = View.GONE
        }
    }

    private fun initializeSeekBar() {
        binding.timeBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) playerService?.seekToPosition(value.toLong() * 1000)
        }
        updateSeekBar()
    }

    /**
     * Update the position, duration and text views belonging to the seek bar
     */
    private fun updateSeekBar() {
        val duration = playerService?.getDuration()?.takeIf { it > 0 } ?: let {
            // if there's no duration available, clear everything
            binding.timeBar.value = 0f
            binding.duration.text = ""
            binding.currentPosition.text = ""
            handler.postDelayed(this::updateSeekBar, 100)
            return
        }
        val currentPosition = playerService?.getCurrentPosition()?.toFloat() ?: 0f

        // set the text for the indicators
        binding.duration.text = DateUtils.formatElapsedTime(duration / 1000)
        binding.currentPosition.text = DateUtils.formatElapsedTime(
            (currentPosition / 1000).toLong()
        )

        // update the time bar current value and maximum value
        binding.timeBar.valueTo = (duration / 1000).toFloat()
        binding.timeBar.value = minOf(
            currentPosition / 1000,
            binding.timeBar.valueTo
        )

        handler.postDelayed(this::updateSeekBar, 200)
    }

    private fun handleServiceConnection() {
        playerService?.onIsPlayingChanged = { isPlaying ->
            binding.playPause.setIconResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
            isPaused = !isPlaying
        }
        initializeSeekBar()
    }

    override fun onDestroy() {
        // unregister all listeners and the connected [playerService]
        playerService?.onIsPlayingChanged = null
        activity?.unbindService(connection)
        PlayingQueue.removeOnTrackChangedListener(onTrackChangeListener)

        super.onDestroy()
    }
}
