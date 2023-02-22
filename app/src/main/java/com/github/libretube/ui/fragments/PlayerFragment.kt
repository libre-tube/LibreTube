package com.github.libretube.ui.fragments

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Base64
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.net.toUri
import androidx.core.os.ConfigurationCompat
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.github.libretube.R
import com.github.libretube.api.CronetHelper
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.api.obj.Message
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.Segment
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentPlayerBinding
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.WatchPosition
import com.github.libretube.enums.PlayerEvent
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.hideKeyboard
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.updateParameters
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.DashHelper
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PlayerHelper.checkForSegments
import com.github.libretube.helpers.PlayerHelper.loadPlaybackParams
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.obj.VideoResolution
import com.github.libretube.services.BackgroundMode
import com.github.libretube.services.DownloadService
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.adapters.ChaptersAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.dialogs.AddToPlaylistDialog
import com.github.libretube.ui.dialogs.DownloadDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.dialogs.StatsDialog
import com.github.libretube.ui.extensions.setAspectRatio
import com.github.libretube.ui.extensions.setupSubscriptionButton
import com.github.libretube.ui.interfaces.OnlinePlayerOptions
import com.github.libretube.ui.listeners.SeekbarPreviewListener
import com.github.libretube.ui.models.CommentsViewModel
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.ui.sheets.BaseBottomSheet
import com.github.libretube.ui.sheets.CommentsSheet
import com.github.libretube.ui.sheets.PlayingQueueSheet
import com.github.libretube.util.DataSaverMode
import com.github.libretube.util.HtmlParser
import com.github.libretube.util.LinkHandler
import com.github.libretube.util.NowPlayingNotification
import com.github.libretube.util.PlayingQueue
import com.github.libretube.util.TextUtils
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.cronet.CronetDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import retrofit2.HttpException

class PlayerFragment : Fragment(R.layout.fragment_player), OnlinePlayerOptions {
    val binding by viewBinding(FragmentPlayerBinding::bind)
    private val playerBinding by viewBinding { it.binding.player.binding }
    private val doubleTapOverlayBinding by viewBinding {
        it.binding.doubleTapOverlay.binding
    }
    private val playerGestureControlsViewBinding by viewBinding {
        it.binding.playerGestureControlsView.binding
    }

    private val viewModel: PlayerViewModel by activityViewModels()
    private val commentsViewModel: CommentsViewModel by activityViewModels()

    /**
     * Video information passed by the intent
     */
    private var videoId: String? = null
    private var playlistId: String? = null
    private var channelId: String? = null
    private var keepQueue: Boolean = false
    private var timeStamp: Long? = null

    /**
     * Video information fetched at runtime
     */
    private lateinit var streams: Streams

    /**
     * for the transition
     */
    private var sId: Int = 0
    private var eId: Int = 0
    private var transitioning = false

    /**
     * for the player
     */
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector
    private var captionLanguage: String? = PlayerHelper.defaultSubtitleCode

    /**
     * Chapters and comments
     */
    private lateinit var chapters: List<ChapterSegment>

    /**
     * for the player view
     */
    private var subtitles = mutableListOf<SubtitleConfiguration>()

    /**
     * for the player notification
     */
    private lateinit var nowPlayingNotification: NowPlayingNotification

    /**
     * SponsorBlock
     */
    private var segments = listOf<Segment>()
    private var sponsorBlockEnabled = PlayerHelper.sponsorBlockEnabled

    private val handler = Handler(Looper.getMainLooper())
    private val mainActivity get() = activity as MainActivity

    /**
     * Receiver for all actions in the PiP mode
     */
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.getIntExtra(PlayerHelper.CONTROL_TYPE, 0) ?: return
            when (PlayerEvent.fromInt(action)) {
                PlayerEvent.Play -> {
                    exoPlayer.play()
                }
                PlayerEvent.Pause -> {
                    exoPlayer.pause()
                }
                PlayerEvent.Forward -> {
                    exoPlayer.seekTo(exoPlayer.currentPosition + PlayerHelper.seekIncrement)
                }
                PlayerEvent.Rewind -> {
                    exoPlayer.seekTo(exoPlayer.currentPosition - PlayerHelper.seekIncrement)
                }
                PlayerEvent.Next -> {
                    playNextVideo()
                }
                PlayerEvent.Background -> {
                    playOnBackground()
                    // wait some time in order for the service to get started properly
                    handler.postDelayed(500) {
                        activity?.finish()
                    }
                }
                else -> {
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoId = it.getString(IntentData.videoId)!!.toID()
            playlistId = it.getString(IntentData.playlistId)
            channelId = it.getString(IntentData.channelId)
            keepQueue = it.getBoolean(IntentData.keepQueue, false)
            timeStamp = it.getLong(IntentData.timeStamp, 0L)
        }

        // broadcast receiver for PiP actions
        context?.registerReceiver(
            broadcastReceiver,
            IntentFilter(PlayerHelper.getIntentActon(requireContext()))
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.hideKeyboard(view)

        // reset the callbacks of the playing queue
        PlayingQueue.resetToDefaults()

        // clear the playing queue
        if (!keepQueue) PlayingQueue.clear()

        changeOrientationMode()

        createExoPlayer()
        initializeTransitionLayout()
        initializeOnClickActions()
        playVideo()

        showBottomBar()
    }

    /**
     * somehow the bottom bar is invisible on low screen resolutions, this fixes it
     */
    private fun showBottomBar() {
        if (isAdded && !binding.player.isPlayerLocked) {
            playerBinding.bottomBar.isVisible = true
        }
        handler.postDelayed(this::showBottomBar, 100)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeTransitionLayout() {
        mainActivity.binding.container.visibility = View.VISIBLE
        val mainMotionLayout = mainActivity.binding.mainMotionLayout

        binding.playerMotionLayout.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
                mainMotionLayout.progress = abs(progress)
                binding.player.hideController()
                binding.player.useController = false
                eId = endId
                sId = startId
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (currentId == eId) {
                    viewModel.isMiniPlayerVisible.value = true
                    // disable captions
                    updateCaptionsLanguage(null)
                    binding.player.useController = false
                    mainMotionLayout.progress = 1F
                    (activity as MainActivity).requestOrientationChange()
                } else if (currentId == sId) {
                    viewModel.isMiniPlayerVisible.value = false
                    // re-enable captions
                    updateCaptionsLanguage(captionLanguage)
                    binding.player.useController = true
                    mainMotionLayout.progress = 0F
                    changeOrientationMode()
                }
            }

            override fun onTransitionTrigger(
                MotionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
            }
        })

        if (PlayerHelper.swipeGestureEnabled) {
            binding.playerMotionLayout.addSwipeUpListener {
                binding.player.hideController()
                setFullscreen()
            }
        }

        binding.playerMotionLayout.progress = 1.toFloat()
        binding.playerMotionLayout.transitionToStart()

        if (usePiP()) activity?.setPictureInPictureParams(getPipParams())

        if (SDK_INT < Build.VERSION_CODES.O) {
            binding.relPlayerPip.visibility = View.GONE
        }
    }

    // actions that don't depend on video information
    private fun initializeOnClickActions() {
        binding.closeImageView.setOnClickListener {
            PlayingQueue.clear()
            BackgroundHelper.stopBackgroundPlay(requireContext())
            killPlayerFragment()
        }
        playerBinding.closeImageButton.setOnClickListener {
            PlayingQueue.clear()
            BackgroundHelper.stopBackgroundPlay(requireContext())
            killPlayerFragment()
        }
        playerBinding.autoPlay.visibility = View.VISIBLE

        binding.playImageView.setOnClickListener {
            when {
                !exoPlayer.isPlaying && exoPlayer.playbackState == Player.STATE_ENDED -> {
                    exoPlayer.seekTo(0)
                }
                !exoPlayer.isPlaying -> exoPlayer.play()
                else -> exoPlayer.pause()
            }
        }

        // video description and chapters toggle
        binding.playerTitleLayout.setOnClickListener {
            if (this::streams.isInitialized) toggleDescription()
        }

        binding.commentsToggle.setOnClickListener {
            videoId ?: return@setOnClickListener
            // set the max height to not cover the currently playing video
            commentsViewModel.maxHeight = binding.root.height - binding.player.height
            commentsViewModel.videoId = videoId
            CommentsSheet().show(childFragmentManager)
        }

        playerBinding.queueToggle.visibility = View.VISIBLE
        playerBinding.queueToggle.setOnClickListener {
            PlayingQueueSheet().show(childFragmentManager, null)
        }

        // FullScreen button trigger
        // hide fullscreen button if auto rotation enabled
        playerBinding.fullscreen.visibility =
            if (PlayerHelper.autoRotationEnabled) View.INVISIBLE else View.VISIBLE
        playerBinding.fullscreen.setOnClickListener {
            // hide player controller
            binding.player.hideController()
            if (viewModel.isFullscreen.value == false) {
                // go to fullscreen mode
                setFullscreen()
            } else {
                // exit fullscreen mode
                unsetFullscreen()
            }
        }

        val updateSbImageResource = {
            playerBinding.sbToggle.setImageResource(
                if (sponsorBlockEnabled) R.drawable.ic_sb_enabled else R.drawable.ic_sb_disabled
            )
        }
        updateSbImageResource()
        playerBinding.sbToggle.setOnClickListener {
            sponsorBlockEnabled = !sponsorBlockEnabled
            updateSbImageResource()
        }

        // share button
        binding.relPlayerShare.setOnClickListener {
            if (!this::streams.isInitialized) return@setOnClickListener
            val shareDialog =
                ShareDialog(
                    videoId!!,
                    ShareObjectType.VIDEO,
                    ShareData(
                        currentVideo = streams.title,
                        currentPosition = exoPlayer.currentPosition / 1000
                    )
                )
            shareDialog.show(childFragmentManager, ShareDialog::class.java.name)
        }

        binding.relPlayerShare.setOnLongClickListener {
            streams.hls ?: return@setOnLongClickListener true

            // start an intent with video as mimetype using the hls stream
            val uri: Uri = Uri.parse(streams.hls)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                putExtra(Intent.EXTRA_TITLE, streams.title)
                putExtra("title", streams.title)
                putExtra("artist", streams.uploader)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, R.string.no_player_found, Toast.LENGTH_SHORT).show()
            }
            true
        }

        binding.relPlayerBackground.setOnClickListener {
            // pause the current player
            exoPlayer.pause()

            // start the background mode
            playOnBackground()
        }

        binding.relatedRecView.layoutManager = VideosAdapter.getLayout(requireContext())

        binding.alternativeTrendingRec.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }

    private fun playOnBackground() {
        BackgroundHelper.stopBackgroundPlay(requireContext())
        BackgroundHelper.playOnBackground(
            requireContext(),
            videoId!!,
            exoPlayer.currentPosition,
            playlistId,
            channelId,
            true
        )
        handler.postDelayed(500) {
            NavigationHelper.startAudioPlayer(requireContext())
            killPlayerFragment()
        }
    }

    private fun setFullscreen() {
        with(binding.playerMotionLayout) {
            getConstraintSet(R.id.start).constrainHeight(R.id.player, -1)
            enableTransition(R.id.yt_transition, false)
        }

        binding.mainContainer.isClickable = true
        binding.linLayout.visibility = View.GONE
        playerBinding.fullscreen.setImageResource(R.drawable.ic_fullscreen_exit)
        playerBinding.exoTitle.visibility = View.VISIBLE

        if (!PlayerHelper.autoRotationEnabled) {
            // different orientations of the video are only available when auto rotation is disabled
            val orientation = PlayerHelper.getOrientation(exoPlayer.videoSize)
            mainActivity.requestedOrientation = orientation
        }

        viewModel.isFullscreen.value = true
    }

    @SuppressLint("SourceLockedOrientationActivity")
    fun unsetFullscreen() {
        // leave fullscreen mode
        with(binding.playerMotionLayout) {
            getConstraintSet(R.id.start).constrainHeight(R.id.player, 0)
            enableTransition(R.id.yt_transition, true)
        }

        binding.mainContainer.isClickable = false
        binding.linLayout.visibility = View.VISIBLE
        playerBinding.fullscreen.setImageResource(R.drawable.ic_fullscreen)
        playerBinding.exoTitle.visibility = View.INVISIBLE

        if (!PlayerHelper.autoRotationEnabled) {
            // switch back to portrait mode if auto rotation disabled
            mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }

        viewModel.isFullscreen.value = false
    }

    private fun toggleDescription() {
        val views = if (binding.descLinLayout.isVisible) {
            // show formatted short view count
            streams.views.formatShort()
        } else {
            // show exact view count
            String.format("%,d", streams.views)
        }
        val viewInfo = getString(R.string.normal_views, views, localizeDate(streams))
        if (binding.descLinLayout.isVisible) {
            // hide the description and chapters
            binding.playerDescriptionArrow.animate().rotation(0F).setDuration(250).start()
            binding.descLinLayout.visibility = View.GONE

            // limit the title height to two lines
            binding.playerTitle.maxLines = 2
        } else {
            // show the description and chapters
            binding.playerDescriptionArrow.animate().rotation(180F).setDuration(250).start()
            binding.descLinLayout.visibility = View.VISIBLE

            // show the whole title
            binding.playerTitle.maxLines = Int.MAX_VALUE
        }
        binding.playerViewsInfo.text = viewInfo

        if (this::chapters.isInitialized && chapters.isNotEmpty()) {
            val chapterIndex = getCurrentChapterIndex() ?: return
            // scroll to the current chapter in the chapterRecView in the description
            val layoutManager = binding.chaptersRecView.layoutManager as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(chapterIndex, 0)
            // set selected
            val chaptersAdapter = binding.chaptersRecView.adapter as ChaptersAdapter
            chaptersAdapter.updateSelectedPosition(chapterIndex)
        }
    }

    override fun onPause() {
        // pauses the player if the screen is turned off

        // check whether the screen is on
        val pm = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = pm.isInteractive

        // pause player if screen off and setting enabled
        if (
            this::exoPlayer.isInitialized && !isScreenOn && PlayerHelper.pausePlayerOnScreenOffEnabled
        ) {
            exoPlayer.pause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()

        handler.removeCallbacksAndMessages(null)

        try {
            // disable the auto PiP mode for SDK >= 32
            disableAutoPiP()

            // unregister the receiver for player actions
            context?.unregisterReceiver(broadcastReceiver)

            saveWatchPosition()

            // release the player
            nowPlayingNotification.destroySelfAndPlayer()

            activity?.requestedOrientation =
                if ((activity as MainActivity).autoRotationEnabled) {
                    ActivityInfo.SCREEN_ORIENTATION_USER
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun disableAutoPiP() {
        if (SDK_INT < Build.VERSION_CODES.S) {
            return
        }
        activity?.setPictureInPictureParams(
            PictureInPictureParams.Builder().setAutoEnterEnabled(false).build()
        )
    }

    // save the watch position if video isn't finished and option enabled
    private fun saveWatchPosition() {
        if (!PlayerHelper.watchPositionsVideo) return
        val watchPosition = WatchPosition(videoId!!, exoPlayer.currentPosition)
        CoroutineScope(Dispatchers.IO).launch {
            Database.watchPositionDao().insertAll(listOf(watchPosition))
        }
    }

    private fun checkForSegments() {
        if (!exoPlayer.isPlaying || !PlayerHelper.sponsorBlockEnabled) return

        handler.postDelayed(this::checkForSegments, 100)

        if (!sponsorBlockEnabled) return

        if (segments.isEmpty()) return

        exoPlayer.checkForSegments(requireContext(), segments, PlayerHelper.skipSegmentsManually)?.let { segmentEnd ->
            binding.sbSkipBtn.visibility = View.VISIBLE
            binding.sbSkipBtn.setOnClickListener {
                exoPlayer.seekTo(segmentEnd)
            }
            return
        }

        if (PlayerHelper.skipSegmentsManually) binding.sbSkipBtn.visibility = View.GONE
    }

    private fun playVideo() {
        // reset the player view
        playerBinding.exoProgress.clearSegments()
        playerBinding.sbToggle.visibility = View.GONE

        // reset the comments to become reloaded later
        commentsViewModel.reset()

        lifecycleScope.launchWhenCreated {
            streams = try {
                RetrofitInstance.api.getStreams(videoId!!)
            } catch (e: IOException) {
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG).show()
                return@launchWhenCreated
            } catch (e: HttpException) {
                val errorMessage = e.response()?.errorBody()?.string()?.let {
                    JsonHelper.json.decodeFromString<Message>(it).message
                } ?: context?.getString(R.string.server_error) ?: ""
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                return@launchWhenCreated
            }

            if (PlayingQueue.isEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (playlistId != null) {
                        PlayingQueue.insertPlaylist(playlistId!!, streams.toStreamItem(videoId!!))
                    } else if (channelId != null) {
                        PlayingQueue.insertChannel(channelId!!, streams.toStreamItem(videoId!!))
                    } else {
                        PlayingQueue.updateCurrent(streams.toStreamItem(videoId!!))
                        if (PlayerHelper.autoInsertRelatedVideos) {
                            PlayingQueue.add(*streams.relatedStreams.toTypedArray())
                        }
                    }
                }
            } else {
                PlayingQueue.updateCurrent(streams.toStreamItem(videoId!!))
            }

            PlayingQueue.setOnQueueTapListener { streamItem ->
                streamItem.url?.toID()?.let { playNextVideo(it) }
            }

            withContext(Dispatchers.Main) {
                // hide the button to skip SponsorBlock segments manually
                binding.sbSkipBtn.visibility = View.GONE

                // set media sources for the player
                setResolutionAndSubtitles()
                prepareExoPlayerView()
                initializePlayerView()
                setupSeekbarPreview()

                if (!streams.livestream) seekToWatchPosition()
                trySeekToTimeStamp()

                exoPlayer.prepare()
                if (!DataSaverMode.isEnabled(requireContext())) exoPlayer.play()

                if (binding.playerMotionLayout.progress != 1.0f) {
                    // show controllers when not in picture in picture mode
                    if (!(usePiP() && activity?.isInPictureInPictureMode!!)) {
                        binding.player.useController = true
                    }
                }
                // show the player notification
                initializePlayerNotification()
                if (PlayerHelper.sponsorBlockEnabled) fetchSponsorBlockSegments()

                // add the video to the watch history
                if (PlayerHelper.watchHistoryEnabled) {
                    DatabaseHelper.addToWatchHistory(videoId!!, streams)
                }
            }
        }
    }

    /**
     * Detect whether PiP is supported and enabled
     */
    private fun usePiP(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.O && PlayerHelper.pipEnabled
    }

    /**
     * fetch the segments for SponsorBlock
     */
    private fun fetchSponsorBlockSegments() {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val categories = PlayerHelper.getSponsorBlockCategories()
                if (categories.isEmpty()) return@runCatching
                segments =
                    RetrofitInstance.api.getSegments(
                        videoId!!,
                        JsonHelper.json.encodeToString(categories)
                    ).segments
                if (segments.isEmpty()) return@runCatching
                playerBinding.exoProgress.setSegments(segments)
                withContext(Dispatchers.Main) {
                    playerBinding.sbToggle.visibility = View.VISIBLE
                    updateDisplayedDuration()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshLiveStatus() {
        // switch back to normal speed when on the end of live stream
        if (exoPlayer.duration - exoPlayer.currentPosition < 7000) {
            exoPlayer.setPlaybackSpeed(1F)
            playerBinding.timeSeparator.visibility = View.GONE
            playerBinding.liveDiff.text = ""
        } else {
            // live stream but not watching at the end/live position
            playerBinding.timeSeparator.visibility = View.VISIBLE
            val diffText = DateUtils.formatElapsedTime(
                (exoPlayer.duration - exoPlayer.currentPosition) / 1000
            )
            playerBinding.liveDiff.text = "-$diffText"
        }
        // call the function again after 100ms
        handler.postDelayed(this@PlayerFragment::refreshLiveStatus, 100)
    }

    /**
     *  Seek to saved watch position if available */
    private fun seekToWatchPosition() {
        // browse the watch positions
        val position = try {
            runBlocking {
                Database.watchPositionDao().findById(videoId!!)?.position
            }
        } catch (e: Exception) {
            return
        }
        // position is almost the end of the video => don't seek, start from beginning
        if (position != null && position < streams.duration * 1000 * 0.9) {
            exoPlayer.seekTo(position)
        }
    }

    /**
     * Seek to the time stamp passed by the intent arguments if available
     */
    private fun trySeekToTimeStamp() {
        // support for time stamped links
        timeStamp?.let {
            if (it != 0L) exoPlayer.seekTo(it * 1000)
        }
        // delete the time stamp because it already got consumed
        timeStamp = null
    }

    // used for autoplay and skipping to next video
    private fun playNextVideo(nextId: String? = null) {
        val nextVideoId = nextId ?: PlayingQueue.getNext()
        // by making sure that the next and the current video aren't the same
        saveWatchPosition()

        // save the id of the next stream as videoId and load the next video
        if (nextVideoId != null) {
            videoId = nextVideoId

            // play the next video
            playVideo()

            // close comment bottom-sheet for next video
            commentsViewModel.commentsSheetDismiss?.invoke()
        }
    }

    private fun prepareExoPlayerView() {
        binding.player.apply {
            useController = false
            player = exoPlayer
        }

        playerBinding.exoProgress.setPlayer(exoPlayer)
    }

    private fun localizeDate(streams: Streams): String {
        return if (!streams.livestream) {
            val locale = ConfigurationCompat.getLocales(resources.configuration)[0]!!
            TextUtils.SEPARATOR + TextUtils.localizeDate(streams.uploadDate, locale)
        } else {
            ""
        }
    }

    private fun handleLiveVideo() {
        playerBinding.exoPosition.visibility = View.GONE
        playerBinding.liveDiff.visibility = View.VISIBLE
        playerBinding.duration.text = getString(R.string.live)
        playerBinding.exoTime.setOnClickListener {
            exoPlayer.seekTo(exoPlayer.duration)
        }
        refreshLiveStatus()
    }

    @SuppressLint("SetTextI18n")
    private fun initializePlayerView() {
        // initialize the player view actions
        binding.player.initialize(
            this,
            doubleTapOverlayBinding,
            playerGestureControlsViewBinding,
            trackSelector,
            viewModel,
            viewLifecycleOwner
        )

        binding.apply {
            val views = streams.views.formatShort()
            playerViewsInfo.text = getString(R.string.normal_views, views, localizeDate(streams))

            textLike.text = streams.likes.formatShort()
            textDislike.text = streams.dislikes.formatShort()
            ImageHelper.loadImage(streams.uploaderAvatar, binding.playerChannelImage)
            playerChannelName.text = streams.uploader

            titleTextView.text = streams.title

            playerTitle.text = streams.title
            playerDescription.text = streams.description

            playerChannelSubCount.text = context?.getString(
                R.string.subscribers,
                streams.uploaderSubscriberCount.formatShort()
            )
        }

        // duration that's not greater than 0 indicates that the video is live
        if (streams.livestream) handleLiveVideo()

        playerBinding.exoTitle.text = streams.title

        // init the chapters recyclerview
        chapters = streams.chapters
        initializeChapters()

        // Listener for play and pause icon change
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (usePiP()) activity?.setPictureInPictureParams(getPipParams())

                if (isPlaying) {
                    // Stop [BackgroundMode] service if it is running.
                    BackgroundHelper.stopBackgroundPlay(requireContext())
                } else {
                    disableAutoPiP()
                }

                if (isPlaying && PlayerHelper.sponsorBlockEnabled) {
                    handler.postDelayed(
                        this@PlayerFragment::checkForSegments,
                        100
                    )
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                updateDisplayedDuration()
                super.onEvents(player, events)
                if (events.containsAny(
                        Player.EVENT_PLAYBACK_STATE_CHANGED,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_PLAY_WHEN_READY_CHANGED
                    )
                ) {
                    updatePlayPauseButton()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                // save the watch position to the database
                // only called when the position is unequal to 0, otherwise it would become reset
                // before the player can seek to the saved position from videos of the queue
                if (exoPlayer.currentPosition != 0L) saveWatchPosition()

                // check if video has ended, next video is available and autoplay is enabled.
                if (
                    playbackState == Player.STATE_ENDED &&
                    !transitioning &&
                    binding.player.autoplayEnabled
                ) {
                    transitioning = true
                    playNextVideo()
                }

                if (playbackState == Player.STATE_READY) {
                    // media actually playing
                    transitioning = false
                    // update the PiP params to use the correct aspect ratio
                    if (usePiP()) activity?.setPictureInPictureParams(getPipParams())
                }

                // listen for the stop button in the notification
                if (playbackState == PlaybackState.STATE_STOPPED && usePiP()) {
                    // finish PiP by finishing the activity
                    if (activity?.isInPictureInPictureMode!!) activity?.finish()
                }
                super.onPlaybackStateChanged(playbackState)
            }

            /**
             * Catch player errors to prevent the app from stopping
             */
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                try {
                    exoPlayer.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })

        binding.relPlayerDownload.setOnClickListener {
            if (streams.duration <= 0) {
                Toast.makeText(context, R.string.cannotDownload, Toast.LENGTH_SHORT).show()
            } else if (!DownloadService.IS_DOWNLOAD_RUNNING) {
                val newFragment = DownloadDialog(videoId!!)
                newFragment.show(childFragmentManager, DownloadDialog::class.java.name)
            } else {
                Toast.makeText(context, R.string.dlisinprogress, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        if (streams.hls != null) {
            binding.relPlayerPip.setOnClickListener {
                if (SDK_INT < Build.VERSION_CODES.O) return@setOnClickListener
                try {
                    activity?.enterPictureInPictureMode(getPipParams())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        initializeRelatedVideos(streams.relatedStreams)
        // set video description
        val description = streams.description

        setupDescription(binding.playerDescription, description)
        binding.videoCategory.text = "${context?.getString(R.string.category)}: ${streams.category}"

        binding.playerChannel.setOnClickListener {
            val activity = view?.context as MainActivity
            val bundle = bundleOf(IntentData.channelId to streams.uploaderUrl)
            activity.navController.navigate(R.id.channelFragment, bundle)
            activity.binding.mainMotionLayout.transitionToEnd()
            binding.playerMotionLayout.transitionToEnd()
        }

        // update the subscribed state
        binding.playerSubscribe.setupSubscriptionButton(
            this.streams.uploaderUrl.toID(),
            this.streams.uploader
        )

        binding.relPlayerSave.setOnClickListener {
            AddToPlaylistDialog(videoId!!).show(
                childFragmentManager,
                AddToPlaylistDialog::class.java.name
            )
        }

        syncQueueButtons()

        playerBinding.skipPrev.setOnClickListener {
            playNextVideo(PlayingQueue.getPrev())
        }

        playerBinding.skipNext.setOnClickListener {
            playNextVideo()
        }
    }

    /**
     * Set up the description text with video links and timestamps
     */
    private fun setupDescription(
        descTextView: TextView,
        description: String
    ) {
        // detect whether the description is html formatted
        if (description.contains("<") && description.contains(">")) {
            descTextView.movementMethod = LinkMovementMethod.getInstance()
            descTextView.text = description
                .parseAsHtml(tagHandler = HtmlParser(LinkHandler(this::handleLink)))
        } else {
            // Links can be present as plain text
            descTextView.autoLinkMask = Linkify.WEB_URLS
            descTextView.text = description
        }
    }

    /**
     * Handle a link clicked in the description
     */
    private fun handleLink(link: String) {
        val uri = Uri.parse(link)
        // get video id if the link is a valid youtube video link
        val videoId = TextUtils.getVideoIdFromUri(link)
        if (videoId.isNullOrEmpty()) {
            // not a youtube video link, thus handle normally
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
            return
        }

        // check if the video is the current video and has a valid time
        if (videoId == this.videoId) {
            // try finding the time stamp of the url and seek to it if found
            TextUtils.parseTimestamp(uri.getQueryParameter("t") ?: return)?.let {
                exoPlayer.seekTo(it * 1000)
            }
        } else {
            // youtube video link without time or not the current video, thus load in player
            playNextVideo(videoId)
        }
    }

    /**
     * Update the displayed duration of the video
     */
    @SuppressLint("SetTextI18n")
    private fun updateDisplayedDuration() {
        if (exoPlayer.duration < 0 || streams.livestream) return

        playerBinding.duration.text = DateUtils.formatElapsedTime(
            exoPlayer.duration.div(1000)
        )
        if (segments.isEmpty()) return

        val durationWithSb = DateUtils.formatElapsedTime(
            exoPlayer.duration.div(1000) - segments.sumOf {
                it.segment[1] - it.segment[0]
            }.toInt()
        )
        playerBinding.duration.text = playerBinding.duration.text.toString() + " ($durationWithSb)"
    }

    private fun syncQueueButtons() {
        if (!PlayerHelper.skipButtonsEnabled) return

        // toggle the visibility of next and prev buttons based on queue and whether the player view is locked
        playerBinding.skipPrev.visibility = if (
            PlayingQueue.hasPrev() && !binding.player.isPlayerLocked
        ) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        playerBinding.skipNext.visibility = if (
            PlayingQueue.hasNext() && !binding.player.isPlayerLocked
        ) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }

        handler.postDelayed(this::syncQueueButtons, 100)
    }

    private fun updatePlayPauseButton() {
        binding.playImageView.setImageResource(
            when {
                exoPlayer.isPlaying -> R.drawable.ic_pause
                exoPlayer.playbackState == Player.STATE_ENDED -> R.drawable.ic_restart
                else -> R.drawable.ic_play
            }
        )
    }

    private fun initializeRelatedVideos(relatedStreams: List<StreamItem>?) {
        if (!PlayerHelper.relatedStreamsEnabled) return

        if (PlayerHelper.alternativeVideoLayout) {
            binding.alternativeTrendingRec.adapter = VideosAdapter(
                relatedStreams.orEmpty().toMutableList(),
                forceMode = VideosAdapter.Companion.ForceMode.RELATED
            )
        } else {
            binding.relatedRecView.adapter = VideosAdapter(
                relatedStreams.orEmpty().toMutableList()
            )
        }
    }

    private fun initializeChapters() {
        if (chapters.isEmpty()) {
            binding.chaptersRecView.visibility = View.GONE
            playerBinding.chapterLL.visibility = View.INVISIBLE
            return
        }
        // show the chapter layouts
        binding.chaptersRecView.visibility = View.VISIBLE
        playerBinding.chapterLL.visibility = View.VISIBLE

        // enable chapters in the video description
        binding.chaptersRecView.layoutManager =
            LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        binding.chaptersRecView.adapter = ChaptersAdapter(chapters, exoPlayer)

        // enable the chapters dialog in the player
        val titles = chapters.map { chapter ->
            "(${chapter.start?.let { DateUtils.formatElapsedTime(it) }}) ${chapter.title}"
        }
        playerBinding.chapterLL.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.chapters)
                .setItems(titles.toTypedArray()) { _, index ->
                    exoPlayer.seekTo(chapters[index].start!! * 1000)
                }
                .show()
        }

        setCurrentChapterName()
    }

    // set the name of the video chapter in the exoPlayerView
    private fun setCurrentChapterName() {
        // return if chapters are empty to avoid crashes
        if (chapters.isEmpty()) return

        // call the function again in 100ms
        binding.player.postDelayed(this::setCurrentChapterName, 100)

        val chapterIndex = getCurrentChapterIndex() ?: return
        val chapterName = chapters[chapterIndex].title?.trim()

        // change the chapter name textView text to the chapterName
        if (chapterName != playerBinding.chapterName.text) {
            playerBinding.chapterName.text = chapterName
            // update the selected item
            val chaptersAdapter = binding.chaptersRecView.adapter as ChaptersAdapter
            chaptersAdapter.updateSelectedPosition(chapterIndex)
        }
    }

    /**
     * Get the name of the currently played chapter
     */
    private fun getCurrentChapterIndex(): Int? {
        val currentPosition = exoPlayer.currentPosition / 1000
        return chapters.indexOfLast { currentPosition >= it.start!! }.takeIf { it >= 0 }
    }

    private fun setMediaSource(uri: Uri, mimeType: String) {
        val mediaItem: MediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(mimeType)
            .setSubtitleConfigurations(subtitles)
            .build()
        exoPlayer.setMediaItem(mediaItem)
    }

    /**
     * Get all available player resolutions
     */
    private fun getAvailableResolutions(): List<VideoResolution> {
        val resolutions = exoPlayer.currentTracks.groups.asSequence()
            .flatMap { group ->
                (0 until group.length).map {
                    group.getTrackFormat(it).height
                }
            }
            .filter { it > 0 }
            .map { VideoResolution("${it}p", it) }
            .toSortedSet(compareByDescending { it.resolution })

        resolutions.add(VideoResolution(getString(R.string.auto_quality), Int.MAX_VALUE))
        return resolutions.toList()
    }

    private fun setResolutionAndSubtitles() {
        // create a list of subtitles
        subtitles = mutableListOf()
        val subtitlesNamesList = mutableListOf(getString(R.string.none))
        val subtitleCodesList = mutableListOf("")
        streams.subtitles.forEach {
            subtitles.add(
                SubtitleConfiguration.Builder(it.url!!.toUri())
                    .setMimeType(it.mimeType!!) // The correct MIME type (required).
                    .setLanguage(it.code) // The subtitle language (optional).
                    .build()
            )
            subtitlesNamesList += it.name!!
            subtitleCodesList += it.code!!
        }

        // set the default subtitle if available
        updateCaptionsLanguage(captionLanguage)

        // set media source and resolution in the beginning
        setStreamSource()
    }

    private fun setPlayerResolution(resolution: Int) {
        trackSelector.updateParameters {
            setMaxVideoSize(Int.MAX_VALUE, resolution)
            setMinVideoSize(Int.MIN_VALUE, resolution)
        }
    }

    private fun setStreamSource() {
        val defaultResolution = PlayerHelper.getDefaultResolution(requireContext()).replace("p", "")
        if (defaultResolution != "") setPlayerResolution(defaultResolution.toInt())

        if (!PreferenceHelper.getBoolean(PreferenceKeys.USE_HLS_OVER_DASH, false) &&
            streams.videoStreams.isNotEmpty()
        ) {
            val uri = streams.dash?.toUri() ?: let {
                val manifest = DashHelper.createManifest(streams)

                // encode to base64
                val encoded = Base64.encodeToString(manifest.toByteArray(), Base64.DEFAULT)

                "data:application/dash+xml;charset=utf-8;base64,$encoded".toUri()
            }

            this.setMediaSource(uri, MimeTypes.APPLICATION_MPD)
        } else if (streams.hls != null) {
            setMediaSource(streams.hls!!.toUri(), MimeTypes.APPLICATION_M3U8)
        } else {
            Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createExoPlayer() {
        val cronetDataSourceFactory = CronetDataSource.Factory(
            CronetHelper.cronetEngine,
            Executors.newCachedThreadPool()
        )
        val dataSourceFactory = DefaultDataSource.Factory(requireContext(), cronetDataSourceFactory)

        // control for the track sources like subtitles and audio source
        trackSelector = DefaultTrackSelector(requireContext())

        trackSelector.updateParameters {
            setPreferredAudioLanguage(
                Locale.getDefault().language.lowercase().substring(0, 2)
            )
        }

        exoPlayer = ExoPlayer.Builder(requireContext())
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(PlayerHelper.getLoadControl())
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(PlayerHelper.getAudioAttributes(), true)
            .build()
            .loadPlaybackParams()
    }

    /**
     * show the [NowPlayingNotification] for the current video
     */
    private fun initializePlayerNotification() {
        if (!this::nowPlayingNotification.isInitialized) {
            nowPlayingNotification = NowPlayingNotification(requireContext(), exoPlayer, false)
        }
        nowPlayingNotification.updatePlayerNotification(videoId!!, streams)
    }

    /**
     * Use the sensor mode if auto fullscreen is enabled
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private fun changeOrientationMode() {
        if (PlayerHelper.autoRotationEnabled) {
            // enable auto rotation
            mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            onConfigurationChanged(resources.configuration)
        } else {
            // go to portrait mode
            mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }
    }

    override fun onCaptionsClicked() {
        if (!this@PlayerFragment::streams.isInitialized || streams.subtitles.isEmpty()) {
            Toast.makeText(context, R.string.no_subtitles_available, Toast.LENGTH_SHORT).show()
            return
        }

        val subtitlesNamesList = mutableListOf(getString(R.string.none))
        val subtitleCodesList = mutableListOf("")
        streams.subtitles.forEach {
            subtitlesNamesList += it.name!!
            subtitleCodesList += it.code!!
        }

        BaseBottomSheet()
            .setSimpleItems(subtitlesNamesList) { index ->
                val language = subtitleCodesList.getOrNull(index)
                updateCaptionsLanguage(language)
                this.captionLanguage = language
            }
            .show(childFragmentManager)
    }

    override fun onQualityClicked() {
        // get the available resolutions
        val resolutions = getAvailableResolutions()
        val currentQuality = trackSelector.parameters.maxVideoHeight

        // Dialog for quality selection
        BaseBottomSheet()
            .setSimpleItems(
                resolutions.map {
                    if (currentQuality == it.resolution) "${it.name} ✓" else it.name
                }
            ) { which ->
                setPlayerResolution(resolutions[which].resolution)
            }
            .show(childFragmentManager)
    }

    private fun getAudioStreamGroups(audioStreams: List<PipedStream>?): Map<String?, List<PipedStream>> {
        return audioStreams.orEmpty()
            .groupBy { it.audioTrackName }
    }

    override fun onAudioStreamClicked() {
        val audioGroups = getAudioStreamGroups(streams.audioStreams)
        val audioLanguages = audioGroups.map { it.key ?: getString(R.string.default_audio_track) }

        BaseBottomSheet()
            .setSimpleItems(audioLanguages) { index ->
                val audioStreams = audioGroups.values.elementAt(index)
                val lang = audioStreams.firstOrNull()?.audioTrackId?.substring(0, 2)
                trackSelector.updateParameters {
                    setPreferredAudioLanguage(lang)
                }
            }
            .show(childFragmentManager)
    }

    override fun onStatsClicked() {
        if (!this::streams.isInitialized) return
        StatsDialog(exoPlayer, videoId ?: return)
            .show(childFragmentManager, null)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            // hide and disable exoPlayer controls
            binding.player.hideController()
            binding.player.useController = false

            if (viewModel.isMiniPlayerVisible.value == true) {
                binding.playerMotionLayout.transitionToStart()
                viewModel.isMiniPlayerVisible.value = false
            }

            with(binding.playerMotionLayout) {
                getConstraintSet(R.id.start).constrainHeight(R.id.player, -1)
                enableTransition(R.id.yt_transition, false)
            }
            binding.linLayout.visibility = View.GONE

            updateCaptionsLanguage(null)
        } else {
            // close button got clicked in PiP mode
            // pause the video and keep the app alive
            if (lifecycle.currentState == Lifecycle.State.CREATED) exoPlayer.pause()

            // enable exoPlayer controls again
            binding.player.useController = true

            // set back to portrait mode
            if (viewModel.isFullscreen.value != true) {
                with(binding.playerMotionLayout) {
                    getConstraintSet(R.id.start).constrainHeight(R.id.player, 0)
                    enableTransition(R.id.yt_transition, true)
                }
                binding.linLayout.visibility = View.VISIBLE
            }

            updateCaptionsLanguage(captionLanguage)

            binding.optionsLL.post {
                binding.optionsLL.requestLayout()
            }
        }
    }

    private fun updateCaptionsLanguage(language: String?) {
        trackSelector.updateParameters {
            setPreferredTextRoleFlags(C.ROLE_FLAG_CAPTION)
            setPreferredTextLanguage(language)
        }
    }

    fun onUserLeaveHint() {
        if (usePiP() && shouldStartPiP()) {
            activity?.enterPictureInPictureMode(getPipParams())
            return
        }
        if (PlayerHelper.pauseOnQuit) exoPlayer.pause()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getPipParams(): PictureInPictureParams = PictureInPictureParams.Builder()
        .setActions(PlayerHelper.getPiPModeActions(requireActivity(), exoPlayer.isPlaying))
        .apply {
            if (SDK_INT >= Build.VERSION_CODES.S && PlayerHelper.pipEnabled) {
                setAutoEnterEnabled(true)
            }
            if (exoPlayer.isPlaying) {
                setAspectRatio(exoPlayer.videoSize.width, exoPlayer.videoSize.height)
            }
        }
        .build()

    private fun setupSeekbarPreview() {
        playerBinding.seekbarPreview.visibility = View.GONE
        playerBinding.exoProgress.addListener(
            SeekbarPreviewListener(
                streams.previewFrames,
                playerBinding.seekbarPreview,
                streams.duration * 1000
            )
        )
    }

    private fun shouldStartPiP(): Boolean {
        if (!PlayerHelper.pipEnabled || SDK_INT >= Build.VERSION_CODES.S) {
            return false
        }

        val backgroundModeRunning = BackgroundHelper.isServiceRunning(
            requireContext(),
            BackgroundMode::class.java
        )

        return exoPlayer.isPlaying && !backgroundModeRunning
    }

    private fun killPlayerFragment() {
        viewModel.isFullscreen.value = false
        binding.playerMotionLayout.transitionToEnd()
        mainActivity.supportFragmentManager.commit {
            remove(this@PlayerFragment)
        }

        onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (!PlayerHelper.autoRotationEnabled) return

        // If in PiP mode, orientation is given as landscape.
        if (SDK_INT >= Build.VERSION_CODES.N && activity?.isInPictureInPictureMode == true) return

        when (newConfig.orientation) {
            // go to fullscreen mode
            Configuration.ORIENTATION_LANDSCAPE -> setFullscreen()
            // exit fullscreen if not landscape
            else -> unsetFullscreen()
        }
    }
}
