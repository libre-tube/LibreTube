package com.github.libretube.ui.fragments

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.TransitionAdapter
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import androidx.core.text.parseAsHtml
import androidx.core.view.WindowCompat
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.github.libretube.compat.PictureInPictureCompat
import com.github.libretube.compat.PictureInPictureParamsCompat
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
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.setMetadata
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.extensions.updateParameters
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PlayerHelper.SPONSOR_HIGHLIGHT_CATEGORY
import com.github.libretube.helpers.PlayerHelper.checkForSegments
import com.github.libretube.helpers.PlayerHelper.isInSegment
import com.github.libretube.helpers.PlayerHelper.loadPlaybackParams
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ProxyHelper
import com.github.libretube.obj.PlayerNotificationData
import com.github.libretube.obj.ShareData
import com.github.libretube.obj.VideoResolution
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.services.DownloadService
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.adapters.ChaptersAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.dialogs.AddToPlaylistDialog
import com.github.libretube.ui.dialogs.DownloadDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.dialogs.StatsDialog
import com.github.libretube.ui.extensions.setupSubscriptionButton
import com.github.libretube.ui.interfaces.OnlinePlayerOptions
import com.github.libretube.ui.listeners.SeekbarPreviewListener
import com.github.libretube.ui.models.CommentsViewModel
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.ui.sheets.BaseBottomSheet
import com.github.libretube.ui.sheets.CommentsSheet
import com.github.libretube.ui.sheets.PlayingQueueSheet
import com.github.libretube.util.HtmlParser
import com.github.libretube.util.LinkHandler
import com.github.libretube.util.NowPlayingNotification
import com.github.libretube.util.PlayingQueue
import com.github.libretube.util.TextUtils
import com.github.libretube.util.TextUtils.toTimeInSeconds
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import retrofit2.HttpException
import kotlin.math.abs

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerFragment : Fragment(), OnlinePlayerOptions {
    private var _binding: FragmentPlayerBinding? = null
    val binding get() = _binding!!

    private val playerBinding get() = binding.player.binding
    private val doubleTapOverlayBinding get() = binding.doubleTapOverlay.binding
    private val playerGestureControlsViewBinding get() = binding.playerGestureControlsView.binding

    private val viewModel: PlayerViewModel by activityViewModels()
    private val commentsViewModel: CommentsViewModel by activityViewModels()

    /**
     * Video information passed by the intent
     */
    private lateinit var videoId: String
    private var playlistId: String? = null
    private var channelId: String? = null
    private var keepQueue: Boolean = false
    private var timeStamp: Long = 0

    /**
     * Video information fetched at runtime
     */
    private lateinit var streams: Streams

    /**
     * for the transition
     */
    private var sId: Int = 0
    private var eId: Int = 0
    private var isTransitioning = true

    /**
     * for the player
     */
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector
    private var captionLanguage: String? = PlayerHelper.defaultSubtitleCode

    /**
     * Chapters and comments
     */
    private lateinit var chapters: MutableList<ChapterSegment>

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
    private var sponsorBlockConfig = PlayerHelper.getSponsorBlockCategories()

    private val handler = Handler(Looper.getMainLooper())
    private val mainActivity get() = activity as MainActivity
    private val windowInsetsControllerCompat
        get() = WindowCompat
            .getInsetsController(mainActivity.window, mainActivity.window.decorView)

    private var scrubbingTimeBar = false

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
        val playerData = requireArguments().parcelable<PlayerData>(IntentData.playerData)!!
        videoId = playerData.videoId
        playlistId = playerData.playlistId
        channelId = playerData.channelId
        keepQueue = playerData.keepQueue
        timeStamp = playerData.timestamp

        // broadcast receiver for PiP actions
        context?.registerReceiver(
            broadcastReceiver,
            IntentFilter(PlayerHelper.getIntentActon(requireContext()))
        )

        // schedule task to save the watch position each second
        Timer().scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    handler.post(this@PlayerFragment::saveWatchPosition)
                }
            },
            1000,
            1000
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater)
        return binding.root
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
        if (_binding?.player?.isPlayerLocked == false) {
            playerBinding.bottomBar.isVisible = true
        }
        handler.postDelayed(this::showBottomBar, 100)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeTransitionLayout() {
        mainActivity.binding.container.visibility = View.VISIBLE
        val mainMotionLayout = mainActivity.binding.mainMotionLayout

        binding.playerMotionLayout.addTransitionListener(object : TransitionAdapter() {
            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
                if (_binding == null) return

                mainMotionLayout.progress = abs(progress)
                binding.player.hideController()
                binding.player.useController = false
                commentsViewModel.setCommentSheetExpand(false)
                eId = endId
                sId = startId
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (_binding == null) return

                if (currentId == eId) {
                    viewModel.isMiniPlayerVisible.value = true
                    // disable captions temporarily
                    updateCaptionsLanguage(null)
                    binding.player.useController = false
                    commentsViewModel.setCommentSheetExpand(null)
                    binding.sbSkipBtn.isGone = true
                    mainMotionLayout.progress = 1F
                    (activity as MainActivity).requestOrientationChange()
                } else if (currentId == sId) {
                    viewModel.isMiniPlayerVisible.value = false
                    // re-enable captions
                    updateCaptionsLanguage(captionLanguage)
                    binding.player.useController = true
                    commentsViewModel.setCommentSheetExpand(true)
                    mainMotionLayout.progress = 0F
                    changeOrientationMode()
                }
            }
        })

        binding.playerMotionLayout.addSwipeUpListener {
            if (this::streams.isInitialized && PlayerHelper.fullscreenGesturesEnabled) {
                binding.player.hideController()
                setFullscreen()
            }
        }

        binding.playerMotionLayout.progress = 1.toFloat()
        binding.playerMotionLayout.transitionToStart()

        val activity = requireActivity()
        if (PlayerHelper.pipEnabled) {
            PictureInPictureCompat.setPictureInPictureParams(activity, pipParams)
        }
        binding.relPlayerPip.isVisible = PictureInPictureCompat
            .isPictureInPictureAvailable(activity)
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
            // set the max height to not cover the currently playing video
            commentsViewModel.handleLink = this::handleLink
            commentsViewModel.maxHeight = binding.root.height - binding.player.height
            commentsViewModel.videoId = videoId
            CommentsSheet().show(childFragmentManager)
        }

        playerBinding.queueToggle.visibility = View.VISIBLE
        playerBinding.queueToggle.setOnClickListener {
            PlayingQueueSheet().show(childFragmentManager, null)
        }

        // FullScreen button trigger
        // hide fullscreen button if autorotation enabled
        playerBinding.fullscreen.isInvisible = PlayerHelper.autoRotationEnabled
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
                    videoId,
                    ShareObjectType.VIDEO,
                    ShareData(
                        currentVideo = streams.title,
                        currentPosition = exoPlayer.currentPosition / 1000
                    )
                )
            shareDialog.show(childFragmentManager, ShareDialog::class.java.name)
        }

        binding.relPlayerShare.setOnLongClickListener {
            if (!this::streams.isInitialized || streams.hls == null) {
                return@setOnLongClickListener true
            }

            // start an intent with video as mimetype using the hls stream
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(streams.hls.orEmpty().toUri(), "video/*")
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
            videoId,
            exoPlayer.currentPosition,
            playlistId,
            channelId,
            keepQueue = true,
            keepVideoPlayerAlive = true
        )
        killPlayerFragment()
        NavigationHelper.startAudioPlayer(requireContext())
    }

    private fun setFullscreen() {
        with(binding.playerMotionLayout) {
            getConstraintSet(R.id.start).constrainHeight(R.id.player, -1)
            enableTransition(R.id.yt_transition, false)
        }

        // set status bar icon color to white
        windowInsetsControllerCompat.isAppearanceLightStatusBars = false

        binding.mainContainer.isClickable = true
        binding.linLayout.visibility = View.GONE
        commentsViewModel.setCommentSheetExpand(null)
        playerBinding.fullscreen.setImageResource(R.drawable.ic_fullscreen_exit)
        playerBinding.exoTitle.visibility = View.VISIBLE

        if (!PlayerHelper.autoRotationEnabled) {
            val height = streams.videoStreams.firstOrNull()?.height ?: exoPlayer.videoSize.height
            val width = streams.videoStreams.firstOrNull()?.width ?: exoPlayer.videoSize.width

            // different orientations of the video are only available when autorotation is disabled
            val orientation = PlayerHelper.getOrientation(width, height)
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

        // set status bar icon color back to theme color
        windowInsetsControllerCompat.isAppearanceLightStatusBars =
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> false
                Configuration.UI_MODE_NIGHT_NO -> true
                else -> true
            }

        binding.mainContainer.isClickable = false
        binding.linLayout.visibility = View.VISIBLE
        playerBinding.fullscreen.setImageResource(R.drawable.ic_fullscreen)
        playerBinding.exoTitle.visibility = View.INVISIBLE

        if (!PlayerHelper.autoRotationEnabled) {
            // switch back to portrait mode if autorotation disabled
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
            "%,d".format(streams.views)
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
            setCurrentChapterName(forceUpdate = true, enqueueNew = false)
        }
    }

    override fun onPause() {
        // pauses the player if the screen is turned off

        // check whether the screen is on
        val isInteractive = requireContext().getSystemService<PowerManager>()!!.isInteractive

        // pause player if screen off and setting enabled
        if (this::exoPlayer.isInitialized && !isInteractive &&
            PlayerHelper.pausePlayerOnScreenOffEnabled
        ) {
            exoPlayer.pause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()

        // disable the auto PiP mode for SDK >= 32
        exoPlayer.pause()
        PictureInPictureCompat.setPictureInPictureParams(
            requireActivity(),
            pipParams
        )

        handler.removeCallbacksAndMessages(null)

        runCatching {
            // unregister the receiver for player actions
            context?.unregisterReceiver(broadcastReceiver)
        }

        try {
            saveWatchPosition()

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

        _binding = null
    }

    // save the watch position if video isn't finished and option enabled
    private fun saveWatchPosition() {
        if (!this::exoPlayer.isInitialized || !PlayerHelper.watchPositionsVideo || isTransitioning ||
            exoPlayer.duration == C.TIME_UNSET || exoPlayer.currentPosition in listOf(
                0L,
                C.TIME_UNSET
            )
        ) {
            return
        }
        val watchPosition = WatchPosition(videoId, exoPlayer.currentPosition)
        CoroutineScope(Dispatchers.IO).launch {
            Database.watchPositionDao().insert(watchPosition)
        }
    }

    private fun checkForSegments() {
        if (!exoPlayer.isPlaying || !PlayerHelper.sponsorBlockEnabled) return

        handler.postDelayed(this::checkForSegments, 100)
        if (!sponsorBlockEnabled || segments.isEmpty()) return

        exoPlayer.checkForSegments(requireContext(), segments, sponsorBlockConfig)
            ?.let { segmentEnd ->
                if (viewModel.isMiniPlayerVisible.value == true) return@let
                binding.sbSkipBtn.isVisible = true
                binding.sbSkipBtn.setOnClickListener {
                    exoPlayer.seekTo(segmentEnd)
                }
                return
            }
        if (!exoPlayer.isInSegment(segments)) binding.sbSkipBtn.isGone = true
    }

    private fun playVideo() {
        // reset the player view
        playerBinding.exoProgress.clearSegments()
        playerBinding.sbToggle.visibility = View.GONE

        // reset the comments to become reloaded later
        commentsViewModel.reset()

        lifecycleScope.launch(Dispatchers.IO) {
            streams = try {
                RetrofitInstance.api.getStreams(videoId)
            } catch (e: IOException) {
                context?.toastFromMainDispatcher(R.string.unknown_error, Toast.LENGTH_LONG)
                return@launch
            } catch (e: HttpException) {
                val errorMessage = e.response()?.errorBody()?.string()?.runCatching {
                    JsonHelper.json.decodeFromString<Message>(this).message
                }?.getOrNull() ?: context?.getString(R.string.server_error).orEmpty()
                context?.toastFromMainDispatcher(errorMessage, Toast.LENGTH_LONG)
                return@launch
            }

            if (PlayingQueue.isEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (playlistId != null) {
                        PlayingQueue.insertPlaylist(playlistId!!, streams.toStreamItem(videoId))
                    } else if (channelId != null) {
                        PlayingQueue.insertChannel(channelId!!, streams.toStreamItem(videoId))
                    } else {
                        PlayingQueue.updateCurrent(streams.toStreamItem(videoId))
                        if (PlayerHelper.autoInsertRelatedVideos) {
                            PlayingQueue.add(*streams.relatedStreams.toTypedArray())
                        }
                    }
                }
            } else {
                PlayingQueue.updateCurrent(streams.toStreamItem(videoId))
            }

            if (PreferenceHelper.getBoolean(PreferenceKeys.AUTO_FULLSCREEN_SHORTS, false)) {
                val videoStream = streams.videoStreams.firstOrNull()
                if (PlayingQueue.getCurrent()?.isShort == true ||
                    (videoStream?.height ?: 0) > (videoStream?.width ?: 0)
                ) {
                    withContext(Dispatchers.Main) {
                        if (binding.playerMotionLayout.progress == 0f) setFullscreen()
                    }
                }
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

                exoPlayer.prepare()
                if (PreferenceHelper.getBoolean(PreferenceKeys.PLAY_AUTOMATICALLY, true)) {
                    exoPlayer.play()
                }

                if (binding.playerMotionLayout.progress != 1.0f) {
                    // show controllers when not in picture in picture mode
                    val inPipMode = PlayerHelper.pipEnabled &&
                            PictureInPictureCompat.isInPictureInPictureMode(requireActivity())
                    if (!inPipMode) {
                        binding.player.useController = true
                    }
                }
                // show the player notification
                initializePlayerNotification()

                // Since the highlight is also a chapter, we need to fetch the other segments
                // first
                fetchSponsorBlockSegments()

                initializeChapters()

                // add the video to the watch history
                if (PlayerHelper.watchHistoryEnabled) {
                    DatabaseHelper.addToWatchHistory(videoId, streams)
                }
            }
        }
    }

    /**
     * fetch the segments for SponsorBlock
     */
    private fun fetchSponsorBlockSegments() {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                if (sponsorBlockConfig.isEmpty()) return@launch
                segments =
                    RetrofitInstance.api.getSegments(
                        videoId,
                        JsonHelper.json.encodeToString(sponsorBlockConfig.keys)
                    ).segments
                if (segments.isEmpty()) return@launch

                withContext(Dispatchers.Main) {
                    playerBinding.exoProgress.setSegments(segments)
                    playerBinding.sbToggle.visibility = View.VISIBLE
                    updateDisplayedDuration()
                }
                segments.firstOrNull { it.category == SPONSOR_HIGHLIGHT_CATEGORY }?.let {
                    initializeHighlight(it)
                }
            }
        }
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
            TextUtils.SEPARATOR + TextUtils.localizeDate(streams.uploadDate)
        } else {
            ""
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initializePlayerView() {
        // initialize the player view actions
        binding.player.initialize(doubleTapOverlayBinding, playerGestureControlsViewBinding)
        binding.player.initPlayerOptions(viewModel, viewLifecycleOwner, trackSelector, this)

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

            player.isLive = streams.livestream
        }
        playerBinding.exoTitle.text = streams.title

        // init the chapters recyclerview
        chapters = streams.chapters.toMutableList()

        // Listener for play and pause icon change
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (PlayerHelper.pipEnabled) {
                    PictureInPictureCompat.setPictureInPictureParams(requireActivity(), pipParams)
                }

                if (isPlaying) {
                    // Stop [BackgroundMode] service if it is running.
                    BackgroundHelper.stopBackgroundPlay(requireContext())
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
                saveWatchPosition()

                // set the playback speed to one if having reached the end of a livestream
                if (playbackState == Player.STATE_BUFFERING && binding.player.isLive &&
                    exoPlayer.duration - exoPlayer.currentPosition < 700
                ) {
                    exoPlayer.setPlaybackSpeed(1f)
                }

                // check if video has ended, next video is available and autoplay is enabled.
                if (
                    playbackState == Player.STATE_ENDED &&
                    !isTransitioning &&
                    PlayerHelper.autoPlayEnabled
                ) {
                    isTransitioning = true
                    if (PlayerHelper.autoPlayCountdown) {
                        showAutoPlayCountdown()
                    } else {
                        playNextVideo()
                    }
                }

                if (playbackState == Player.STATE_READY) {
                    // media actually playing
                    isTransitioning = false
                }

                // listen for the stop button in the notification
                if (playbackState == PlaybackState.STATE_STOPPED && PlayerHelper.pipEnabled &&
                    PictureInPictureCompat.isInPictureInPictureMode(requireActivity())
                ) {
                    // finish PiP by finishing the activity
                    activity?.finish()
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
                val newFragment = DownloadDialog(videoId)
                newFragment.show(childFragmentManager, DownloadDialog::class.java.name)
            } else {
                Toast.makeText(context, R.string.dlisinprogress, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.relPlayerPip.setOnClickListener {
            PictureInPictureCompat.enterPictureInPictureMode(requireActivity(), pipParams)
        }
        initializeRelatedVideos(streams.relatedStreams.filter { !it.title.isNullOrBlank() })
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
            AddToPlaylistDialog(videoId).show(
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

    private fun showAutoPlayCountdown() {
        if (!PlayingQueue.hasNext()) return

        binding.player.useController = false
        binding.player.hideController()
        binding.autoplayCountdown.setHideSelfListener {
            // could fail if the video already got closed before
            runCatching {
                binding.autoplayCountdown.visibility = View.GONE
                binding.player.useController = true
            }
        }
        binding.autoplayCountdown.startCountdown {
            runCatching {
                playNextVideo()
            }
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
            descTextView.text = description.replace("</a>", "</a> ")
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
        // get video id if the link is a valid youtube video link
        val videoId = TextUtils.getVideoIdFromUri(link)
        if (videoId.isNullOrEmpty()) {
            // not a YouTube video link, thus handle normally
            val intent = Intent(Intent.ACTION_VIEW, link.toUri())
            startActivity(intent)
            return
        }

        // check if the video is the current video and has a valid time
        if (videoId == this.videoId) {
            // try finding the time stamp of the url and seek to it if found
            link.toUri().getQueryParameter("t")?.toTimeInSeconds()?.let {
                exoPlayer.seekTo(it * 1000)
            }
        } else {
            // YouTube video link without time or not the current video, thus load in player
            playNextVideo(videoId)
        }
    }

    /**
     * Update the displayed duration of the video
     */
    private fun updateDisplayedDuration() {
        val duration = exoPlayer.duration / 1000
        if (duration < 0 || streams.livestream || _binding == null) return

        val durationWithoutSegments = duration - segments.sumOf {
            val (start, end) = it.segmentStartAndEnd
            end - start
        }.toLong()
        val durationString = DateUtils.formatElapsedTime(duration)

        playerBinding.duration.text = if (durationWithoutSegments < duration) {
            "$durationString (${DateUtils.formatElapsedTime(durationWithoutSegments)})"
        } else {
            durationString
        }
    }

    private fun syncQueueButtons() {
        if (!PlayerHelper.skipButtonsEnabled) return

        // toggle the visibility of next and prev buttons based on queue and whether the player view is locked
        val isPlayerLocked = binding.player.isPlayerLocked
        playerBinding.skipPrev.isInvisible = !PlayingQueue.hasPrev() || isPlayerLocked
        playerBinding.skipNext.isInvisible = !PlayingQueue.hasNext() || isPlayerLocked

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
        playerBinding.chapterLL.setOnClickListener {
            PlayerHelper.showChaptersDialog(requireContext(), chapters, exoPlayer)
        }

        setCurrentChapterName()
    }

    private suspend fun initializeHighlight(highlight: Segment) {
        val frame =
            PlayerHelper.getPreviewFrame(streams.previewFrames, exoPlayer.currentPosition) ?: return
        val drawable = withContext(Dispatchers.IO) {
            ImageHelper.getImage(requireContext(), frame.previewUrl)
        }.drawable ?: return
        val highlightChapter = ChapterSegment(
            title = getString(R.string.chapters_videoHighlight),
            start = highlight.segmentStartAndEnd.first.toLong(),
            drawable = drawable
        )
        chapters.add(highlightChapter)
        chapters.sortBy { it.start }

        withContext(Dispatchers.Main) {
            initializeChapters()
        }
    }

    // set the name of the video chapter in the exoPlayerView
    private fun setCurrentChapterName(forceUpdate: Boolean = false, enqueueNew: Boolean = true) {
        // return if chapters are empty to avoid crashes
        if (chapters.isEmpty() || _binding == null) return

        // call the function again in 100ms
        if (enqueueNew) binding.player.postDelayed(this::setCurrentChapterName, 100)

        // if the user is scrubbing the time bar, don't update
        if (scrubbingTimeBar && !forceUpdate) return

        val chapterIndex = PlayerHelper.getCurrentChapterIndex(exoPlayer, chapters) ?: return
        val chapterName = chapters[chapterIndex].title.trim()

        // change the chapter name textView text to the chapterName
        if (chapterName != playerBinding.chapterName.text) {
            playerBinding.chapterName.text = chapterName
            // update the selected item
            val chaptersAdapter = binding.chaptersRecView.adapter as ChaptersAdapter
            chaptersAdapter.updateSelectedPosition(chapterIndex)
        }
    }

    private fun setMediaSource(uri: Uri, mimeType: String) {
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(mimeType)
            .setSubtitleConfigurations(subtitles)
            .setMetadata(streams)
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
        lifecycleScope.launch(Dispatchers.IO) {
            setStreamSource()

            withContext(Dispatchers.Main) {
                // support for time stamped links
                if (timeStamp != 0L) {
                    exoPlayer.seekTo(timeStamp * 1000)
                    // delete the time stamp because it already got consumed
                    timeStamp = 0L
                } else if (!streams.livestream) {
                    // seek to the saved watch position
                    PlayerHelper.getPosition(videoId, streams.duration)?.let {
                        exoPlayer.seekTo(it)
                    }
                }
            }
        }
    }

    private fun setPlayerResolution(resolution: Int) {
        trackSelector.updateParameters {
            setMaxVideoSize(Int.MAX_VALUE, resolution)
            setMinVideoSize(Int.MIN_VALUE, resolution)
        }
    }

    private suspend fun setStreamSource() {
        val defaultResolution = PlayerHelper.getDefaultResolution(requireContext()).replace("p", "")
        if (defaultResolution.isNotEmpty()) setPlayerResolution(defaultResolution.toInt())

        val (uri, mimeType) = when {
            // LBRY HLS
            PreferenceHelper.getBoolean(
                PreferenceKeys.LBRY_HLS,
                false
            ) && streams.videoStreams.any {
                it.quality.orEmpty().contains("LBRY HLS")
            } -> {
                val lbryHlsUrl = streams.videoStreams.first {
                    it.quality!!.contains("LBRY HLS")
                }.url!!
                lbryHlsUrl.toUri() to MimeTypes.APPLICATION_M3U8
            }
            // DASH
            !PreferenceHelper.getBoolean(
                PreferenceKeys.USE_HLS_OVER_DASH,
                false
            ) && streams.videoStreams.isNotEmpty() -> {
                // only use the dash manifest generated by YT if either it's a livestream or no other source is available
                val dashUri =
                    if (streams.livestream && streams.dash != null) ProxyHelper.unwrapStreamUrl(
                        streams.dash!!
                    ).toUri() else {
                        val shouldDisableProxy =
                            ProxyHelper.shouldDisableProxy(streams.videoStreams.first().url!!)
                        PlayerHelper.createDashSource(
                            streams,
                            requireContext(),
                            disableProxy = shouldDisableProxy
                        )
                    }

                dashUri to MimeTypes.APPLICATION_MPD
            }
            // HLS
            streams.hls != null -> {
                ProxyHelper.unwrapStreamUrl(streams.hls!!).toUri() to MimeTypes.APPLICATION_M3U8
            }
            // NO STREAM FOUND
            else -> {
                context?.toastFromMainDispatcher(R.string.unknown_error)
                return
            }
        }
        withContext(Dispatchers.Main) { setMediaSource(uri, mimeType) }
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
                LocaleHelper.getAppLocale().language.lowercase().substring(0, 2)
            )
        }

        exoPlayer = ExoPlayer.Builder(requireContext())
            .setUsePlatformDiagnostics(false)
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
        val playerNotificationData = PlayerNotificationData(
            streams.title,
            streams.uploader,
            streams.thumbnailUrl
        )
        nowPlayingNotification.updatePlayerNotification(videoId, playerNotificationData)
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
        StatsDialog(exoPlayer, videoId)
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
        if (PlayerHelper.pipEnabled && shouldStartPiP()) {
            PictureInPictureCompat.enterPictureInPictureMode(requireActivity(), pipParams)
            return
        }
        if (PlayerHelper.pauseOnQuit) {
            exoPlayer.pause()
        }
    }

    private val pipParams
        get() = PictureInPictureParamsCompat.Builder()
            .setActions(PlayerHelper.getPiPModeActions(requireActivity(), exoPlayer.isPlaying))
            .setAutoEnterEnabled(PlayerHelper.pipEnabled && exoPlayer.isPlaying)
            .apply {
                if (exoPlayer.isPlaying) {
                    setAspectRatio(exoPlayer.videoSize)
                }
            }
            .build()

    private fun setupSeekbarPreview() {
        playerBinding.seekbarPreview.visibility = View.GONE
        playerBinding.exoProgress.addListener(
            SeekbarPreviewListener(
                streams.previewFrames,
                playerBinding,
                streams.duration * 1000,
                onScrub = {
                    setCurrentChapterName(forceUpdate = true, enqueueNew = false)
                    scrubbingTimeBar = true
                },
                onScrubEnd = {
                    scrubbingTimeBar = false
                    setCurrentChapterName(forceUpdate = true, enqueueNew = false)
                }
            )
        )
    }

    /**
     * Detect whether PiP is supported and enabled
     */
    private fun usePiP(): Boolean {
        return PictureInPictureCompat.isPictureInPictureAvailable(requireContext()) && PlayerHelper.pipEnabled
    }

    private fun shouldStartPiP(): Boolean {
        return usePiP() && exoPlayer.isPlaying && !BackgroundHelper.isBackgroundServiceRunning(
            requireContext()
        )
    }

    private fun killPlayerFragment() {
        viewModel.isFullscreen.value = false
        viewModel.isMiniPlayerVisible.value = false
        binding.playerMotionLayout.transitionToEnd()
        mainActivity.supportFragmentManager.commit {
            remove(this@PlayerFragment)
        }

        onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (!PlayerHelper.autoRotationEnabled ||
            // If in PiP mode, orientation is given as landscape.
            PictureInPictureCompat.isInPictureInPictureMode(requireActivity())
        ) {
            return
        }

        when (newConfig.orientation) {
            // go to fullscreen mode
            Configuration.ORIENTATION_LANDSCAPE -> setFullscreen()
            // exit fullscreen if not landscape
            else -> if (_binding != null) unsetFullscreen()
        }
    }
}
