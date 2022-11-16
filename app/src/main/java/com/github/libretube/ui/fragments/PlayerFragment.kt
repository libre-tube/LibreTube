package com.github.libretube.ui.fragments

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.text.Html
import android.text.format.DateUtils
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.R
import com.github.libretube.api.CronetHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.Segment
import com.github.libretube.api.obj.SegmentData
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.DoubleTapOverlayBinding
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.databinding.FragmentPlayerBinding
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.awaitQuery
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.hideKeyboard
import com.github.libretube.extensions.query
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toStreamItem
import com.github.libretube.obj.ShareData
import com.github.libretube.obj.VideoResolution
import com.github.libretube.services.BackgroundMode
import com.github.libretube.services.DownloadService
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.adapters.ChaptersAdapter
import com.github.libretube.ui.adapters.CommentsAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.base.BaseFragment
import com.github.libretube.ui.dialogs.AddToPlaylistDialog
import com.github.libretube.ui.dialogs.DownloadDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.extensions.setupSubscriptionButton
import com.github.libretube.ui.interfaces.OnlinePlayerOptions
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.ui.sheets.BaseBottomSheet
import com.github.libretube.ui.sheets.PlayingQueueSheet
import com.github.libretube.util.BackgroundHelper
import com.github.libretube.util.DashHelper
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NowPlayingNotification
import com.github.libretube.util.PlayerHelper
import com.github.libretube.util.PlayingQueue
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.util.TextUtils
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.cronet.CronetDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.text.Cue.TEXT_SIZE_TYPE_ABSOLUTE
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.chromium.net.CronetEngine
import retrofit2.HttpException
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.abs

class PlayerFragment : BaseFragment(), OnlinePlayerOptions {

    lateinit var binding: FragmentPlayerBinding
    private lateinit var playerBinding: ExoStyledPlayerControlViewBinding
    private lateinit var doubleTapOverlayBinding: DoubleTapOverlayBinding
    private val viewModel: PlayerViewModel by activityViewModels()

    /**
     * video information
     */
    private var videoId: String? = null
    private var playlistId: String? = null
    private var isLive = false
    private lateinit var streams: Streams

    /**
     * for the transition
     */
    private var sId: Int = 0
    private var eId: Int = 0
    private var transitioning = false

    /**
     * for the comments
     */
    private var commentsAdapter: CommentsAdapter? = null
    private var nextPage: String? = null
    private var isLoading = true

    /**
     * for the player
     */
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var segmentData: SegmentData
    private lateinit var chapters: List<ChapterSegment>

    /**
     * for the player view
     */
    private lateinit var exoPlayerView: StyledPlayerView
    private var subtitles = mutableListOf<SubtitleConfiguration>()

    /**
     * user preferences
     */
    private val token = PreferenceHelper.getToken()
    private var videoShownInExternalPlayer = false

    /**
     * for the player notification
     */
    private lateinit var nowPlayingNotification: NowPlayingNotification

    private lateinit var shareData: ShareData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoId = it.getString(IntentData.videoId)!!.toID()
            playlistId = it.getString(IntentData.playlistId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerBinding.inflate(layoutInflater, container, false)
        exoPlayerView = binding.player
        playerBinding = binding.player.binding
        doubleTapOverlayBinding = binding.doubleTapOverlay.binding

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.hideKeyboard(view)

        // clear the playing queue
        PlayingQueue.clear()

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
        if (this::playerBinding.isInitialized && !binding.player.isPlayerLocked) {
            playerBinding.exoBottomBar.visibility = View.VISIBLE
        }
        Handler(Looper.getMainLooper()).postDelayed(this::showBottomBar, 100)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeTransitionLayout() {
        val mainActivity = activity as MainActivity
        mainActivity.binding.container.visibility = View.VISIBLE

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
                val mainMotionLayout =
                    mainActivity.binding.mainMotionLayout
                mainMotionLayout.progress = abs(progress)
                exoPlayerView.hideController()
                exoPlayerView.useController = false
                eId = endId
                sId = startId
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                println(currentId)
                val mainMotionLayout =
                    mainActivity.binding.mainMotionLayout
                if (currentId == eId) {
                    viewModel.isMiniPlayerVisible.value = true
                    exoPlayerView.useController = false
                    mainMotionLayout.progress = 1F
                    (activity as MainActivity).requestOrientationChange()
                } else if (currentId == sId) {
                    viewModel.isMiniPlayerVisible.value = false
                    exoPlayerView.useController = true
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

        binding.playerMotionLayout.progress = 1.toFloat()
        binding.playerMotionLayout.transitionToStart()

        // quitting miniPlayer on single click

        binding.titleTextView.setOnTouchListener { view, motionEvent ->
            view.onTouchEvent(motionEvent)
            if (motionEvent.action == MotionEvent.ACTION_UP) view.performClick()
            binding.root.onTouchEvent(motionEvent)
        }
        binding.titleTextView.setOnClickListener {
            binding.playerMotionLayout.setTransitionDuration(300)
            binding.playerMotionLayout.transitionToStart()
        }
    }

    // actions that don't depend on video information
    private fun initializeOnClickActions() {
        binding.closeImageView.setOnClickListener {
            viewModel.isMiniPlayerVisible.value = false
            binding.playerMotionLayout.transitionToEnd()
            val mainActivity = activity as MainActivity
            mainActivity.supportFragmentManager.beginTransaction()
                .remove(this)
                .commit()
        }
        playerBinding.closeImageButton.setOnClickListener {
            viewModel.isFullscreen.value = false
            binding.playerMotionLayout.transitionToEnd()
            val mainActivity = activity as MainActivity
            mainActivity.supportFragmentManager.beginTransaction()
                .remove(this)
                .commit()
        }

        binding.playImageView.setOnClickListener {
            if (!exoPlayer.isPlaying) {
                // start or go on playing
                binding.playImageView.setImageResource(R.drawable.ic_pause)
                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                    // restart video if finished
                    exoPlayer.seekTo(0)
                }
                exoPlayer.play()
            } else {
                // pause the video
                binding.playImageView.setImageResource(R.drawable.ic_play)
                exoPlayer.pause()
            }
        }

        // video description and chapters toggle
        binding.playerTitleLayout.setOnClickListener {
            toggleDescription()
        }

        binding.commentsToggle.setOnClickListener {
            toggleComments()
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
            exoPlayerView.hideController()
            if (viewModel.isFullscreen.value == false) {
                // go to fullscreen mode
                setFullscreen()
            } else {
                // exit fullscreen mode
                unsetFullscreen()
            }
        }

        // share button
        binding.relPlayerShare.setOnClickListener {
            shareData.currentPosition = exoPlayer.currentPosition / 1000
            val shareDialog =
                ShareDialog(
                    videoId!!,
                    ShareObjectType.VIDEO,
                    shareData
                )
            shareDialog.show(childFragmentManager, ShareDialog::class.java.name)
        }

        binding.relPlayerBackground.setOnClickListener {
            // pause the current player
            exoPlayer.pause()

            // start the background mode
            BackgroundHelper.playOnBackground(
                requireContext(),
                videoId!!,
                exoPlayer.currentPosition,
                playlistId
            )
        }

        binding.playerScrollView.viewTreeObserver
            .addOnScrollChangedListener {
                if (binding.playerScrollView.getChildAt(0).bottom
                    == (binding.playerScrollView.height + binding.playerScrollView.scrollY) &&
                    nextPage != null
                ) {
                    fetchNextComments()
                }
            }

        binding.commentsRecView.layoutManager = LinearLayoutManager(view?.context)
        binding.commentsRecView.setItemViewCacheSize(20)

        binding.relatedRecView.layoutManager = VideosAdapter.getLayout(requireContext())

        binding.alternativeTrendingRec.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
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

        val mainActivity = activity as MainActivity
        if (!PlayerHelper.autoRotationEnabled) {
            // different orientations of the video are only available when auto rotation is disabled
            val orientation = PlayerHelper.getOrientation(exoPlayer.videoSize)
            mainActivity.requestedOrientation = orientation
        }

        viewModel.isFullscreen.value = true
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun unsetFullscreen() {
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
            val mainActivity = activity as MainActivity
            mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }

        viewModel.isFullscreen.value = false
    }

    private fun toggleDescription() {
        if (binding.descLinLayout.isVisible) {
            // hide the description and chapters
            binding.playerDescriptionArrow.animate().rotation(0F).setDuration(250).start()
            binding.descLinLayout.visibility = View.GONE
        } else {
            // show the description and chapters
            binding.playerDescriptionArrow.animate().rotation(180F).setDuration(250).start()
            binding.descLinLayout.visibility = View.VISIBLE
        }
        if (this::chapters.isInitialized && chapters.isNotEmpty()) {
            val chapterIndex = getCurrentChapterIndex()
            // scroll to the current chapter in the chapterRecView in the description
            val layoutManager = binding.chaptersRecView.layoutManager as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(chapterIndex, 0)
            // set selected
            val chaptersAdapter = binding.chaptersRecView.adapter as ChaptersAdapter
            chaptersAdapter.updateSelectedPosition(chapterIndex)
        }
    }

    private fun toggleComments() {
        if (binding.commentsRecView.isVisible) {
            binding.commentsRecView.visibility = View.GONE
            binding.relatedRecView.visibility = View.VISIBLE
        } else {
            binding.commentsRecView.visibility = View.VISIBLE
            binding.relatedRecView.visibility = View.GONE
            if (binding.commentsRecView.isEmpty()) fetchComments()
        }
    }

    override fun onStart() {
        super.onStart()

        // Assuming the video is not playing in external player when returning to app
        videoShownInExternalPlayer = false
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
        try {
            // clear the playing queue
            PlayingQueue.clear()
            PlayingQueue.removeOnQueueTapListener()

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
    }

    // save the watch position if video isn't finished and option enabled
    private fun saveWatchPosition() {
        if (PlayerHelper.watchPositionsEnabled && exoPlayer.currentPosition != exoPlayer.duration) {
            DatabaseHelper.saveWatchPosition(
                videoId!!,
                exoPlayer.currentPosition
            )
        } else if (PlayerHelper.watchPositionsEnabled) {
            // delete watch position if video has ended
            DatabaseHelper.removeWatchPosition(videoId!!)
        }
    }

    private fun checkForSegments() {
        if (!exoPlayer.isPlaying || !PlayerHelper.sponsorBlockEnabled) return

        Handler(Looper.getMainLooper()).postDelayed(this::checkForSegments, 100)

        if (!::segmentData.isInitialized || segmentData.segments.isEmpty()) return

        val currentPosition = exoPlayer.currentPosition
        segmentData.segments.forEach { segment: Segment ->
            val segmentStart = (segment.segment[0] * 1000f).toLong()
            val segmentEnd = (segment.segment[1] * 1000f).toLong()

            // show the button to manually skip the segment
            if (currentPosition in segmentStart until segmentEnd) {
                if (PlayerHelper.skipSegmentsManually) {
                    binding.sbSkipBtn.visibility = View.VISIBLE
                    binding.sbSkipBtn.setOnClickListener {
                        exoPlayer.seekTo(segmentEnd)
                    }
                    return
                }

                if (PlayerHelper.sponsorBlockNotifications) {
                    Toast
                        .makeText(
                            context,
                            R.string.segment_skipped,
                            Toast.LENGTH_SHORT
                        ).show()
                }

                // skip the segment automatically
                exoPlayer.seekTo(segmentEnd)
                return
            }
        }

        if (PlayerHelper.skipSegmentsManually) binding.sbSkipBtn.visibility = View.GONE
    }

    private fun playVideo() {
        playerBinding.exoProgress.clearSegments()

        lifecycleScope.launchWhenCreated {
            streams = try {
                RetrofitInstance.api.getStreams(videoId!!)
            } catch (e: IOException) {
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection")
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response")
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            }

            if (PlayingQueue.isEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    if (playlistId != null) {
                        PlayingQueue.insertPlaylist(playlistId!!, streams.toStreamItem(videoId!!))
                    } else {
                        PlayingQueue.updateCurrent(streams.toStreamItem(videoId!!))
                        PlayingQueue.add(
                            *streams.relatedStreams.orEmpty().toTypedArray()
                        )
                    }
                }
            } else {
                PlayingQueue.updateCurrent(streams.toStreamItem(videoId!!))
            }

            PlayingQueue.setOnQueueTapListener { streamItem ->
                streamItem.url?.toID()?.let { playNextVideo(it) }
            }

            runOnUiThread {
                // hide the button to skip SponsorBlock segments manually
                binding.sbSkipBtn.visibility = View.GONE

                // set media sources for the player
                setResolutionAndSubtitles()
                prepareExoPlayerView()
                initializePlayerView(streams)
                if (!isLive) seekToWatchPosition()
                exoPlayer.prepare()
                exoPlayer.play()

                if (binding.playerMotionLayout.progress != 1.0f) {
                    // show controllers when not in picture in picture mode
                    if (!(SDK_INT >= Build.VERSION_CODES.O && activity?.isInPictureInPictureMode!!)) {
                        exoPlayerView.useController = true
                    }
                }
                // show the player notification
                initializePlayerNotification()
                if (PlayerHelper.sponsorBlockEnabled) fetchSponsorBlockSegments()

                // show comments if related streams are disabled or the alternative related streams layout is chosen
                if (!PlayerHelper.relatedStreamsEnabled || PlayerHelper.alternativeVideoLayout) toggleComments()

                // add the video to the watch history
                if (PlayerHelper.watchHistoryEnabled) {
                    DatabaseHelper.addToWatchHistory(videoId!!, streams)
                }
            }
        }
    }

    /**
     * fetch the segments for SponsorBlock
     */
    private fun fetchSponsorBlockSegments() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val categories = PlayerHelper.getSponsorBlockCategories()
                if (categories.isEmpty()) return@runCatching
                segmentData =
                    RetrofitInstance.api.getSegments(
                        videoId!!,
                        ObjectMapper().writeValueAsString(categories)
                    )
                playerBinding.exoProgress.setSegments(segmentData.segments)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshLiveStatus() {
        // switch back to normal speed when on the end of live stream
        if (exoPlayer.duration - exoPlayer.currentPosition < 7000) {
            exoPlayer.setPlaybackSpeed(1F)
            playerBinding.liveSeparator.visibility = View.GONE
            playerBinding.liveDiff.text = ""
        } else {
            Log.e(TAG(), "changing the time")
            // live stream but not watching at the end/live position
            playerBinding.liveSeparator.visibility = View.VISIBLE
            val diffText = DateUtils.formatElapsedTime(
                (exoPlayer.duration - exoPlayer.currentPosition) / 1000
            )
            playerBinding.liveDiff.text = "-$diffText"
        }
        // call it again
        Handler(Looper.getMainLooper())
            .postDelayed(this@PlayerFragment::refreshLiveStatus, 100)
    }

    // seek to saved watch position if available
    private fun seekToWatchPosition() {
        // support for time stamped links
        val timeStamp: Long? = arguments?.getLong(IntentData.timeStamp)
        if (timeStamp != null && timeStamp != 0L) {
            exoPlayer.seekTo(timeStamp * 1000)
            return
        }
        // browse the watch positions
        val position = try {
            awaitQuery {
                Database.watchPositionDao().findById(videoId!!)?.position
            }
        } catch (e: Exception) {
            return
        }
        // position is almost the end of the video => don't seek, start from beginning
        if (position != null && position < streams.duration!! * 1000 * 0.9) {
            exoPlayer.seekTo(
                position
            )
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

            // forces the comments to reload for the new video
            binding.commentsRecView.adapter = null
            playVideo()
        }
    }

    private fun prepareExoPlayerView() {
        exoPlayerView.apply {
            setShowSubtitleButton(true)
            setShowNextButton(false)
            setShowPreviousButton(false)
            // controllerShowTimeoutMs = 1500
            controllerHideOnTouch = true
            useController = false
            player = exoPlayer
        }

        playerBinding.exoProgress.setPlayer(exoPlayer)

        applyCaptionStyle()
    }

    private fun applyCaptionStyle() {
        val captionStyle = PlayerHelper.getCaptionStyle(requireContext())
        exoPlayerView.subtitleView?.apply {
            setApplyEmbeddedFontSizes(false)
            setFixedTextSize(TEXT_SIZE_TYPE_ABSOLUTE, 18F)
            if (!PlayerHelper.useSystemCaptionStyle) return
            setApplyEmbeddedStyles(captionStyle == CaptionStyleCompat.DEFAULT)
            setStyle(captionStyle)
        }
    }

    private fun handleLiveVideo() {
        playerBinding.exoTime.visibility = View.GONE
        playerBinding.liveLL.visibility = View.VISIBLE
        playerBinding.liveIndicator.setOnClickListener {
            exoPlayer.seekTo(exoPlayer.duration - 1000)
        }
        refreshLiveStatus()
    }

    @SuppressLint("SetTextI18n")
    private fun initializePlayerView(response: Streams) {
        // initialize the player view actions
        binding.player.initialize(
            childFragmentManager,
            this,
            doubleTapOverlayBinding,
            trackSelector
        )

        binding.apply {
            playerViewsInfo.text =
                context?.getString(R.string.views, response.views.formatShort()) +
                if (!isLive) TextUtils.SEPARATOR + response.uploadDate else ""

            textLike.text = response.likes.formatShort()
            textDislike.text = response.dislikes.formatShort()
            ImageHelper.loadImage(response.uploaderAvatar, binding.playerChannelImage)
            playerChannelName.text = response.uploader

            titleTextView.text = response.title

            playerTitle.text = response.title
            shareData = ShareData(currentVideo = response.title)
            playerDescription.text = response.description

            playerChannelSubCount.text = context?.getString(
                R.string.subscribers,
                response.uploaderSubscriberCount?.formatShort()
            )
        }

        // duration that's not greater than 0 indicates that the video is live
        if (response.duration!! <= 0) {
            isLive = true
            handleLiveVideo()
        }

        playerBinding.exoTitle.text = response.title

        // init the chapters recyclerview
        if (response.chapters != null) {
            chapters = response.chapters
            initializeChapters()
        }

        // Listener for play and pause icon change
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && PlayerHelper.sponsorBlockEnabled) {
                    Handler(Looper.getMainLooper()).postDelayed(
                        this@PlayerFragment::checkForSegments,
                        100
                    )
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                exoPlayerView.keepScreenOn = !(
                    playbackState == Player.STATE_IDLE ||
                        playbackState == Player.STATE_ENDED
                    )

                // check if video has ended, next video is available and autoplay is enabled.
                @Suppress("DEPRECATION")
                if (
                    playbackState == Player.STATE_ENDED &&
                    !transitioning &&
                    binding.player.autoplayEnabled
                ) {
                    transitioning = true
                    // check whether autoplay is enabled
                    if (binding.player.autoplayEnabled) playNextVideo()
                }

                when (playbackState) {
                    Player.STATE_READY -> {
                        // media actually playing
                        transitioning = false
                        binding.playImageView.setImageResource(R.drawable.ic_pause)
                    }
                    Player.STATE_ENDED -> {
                        // video has finished
                        binding.playImageView.setImageResource(R.drawable.ic_restart)
                    }
                    else -> {
                        // player in any other state
                        binding.playImageView.setImageResource(R.drawable.ic_play)
                    }
                }

                // save the watch position when paused
                if (playbackState == PlaybackState.STATE_PAUSED) {
                    query {
                        DatabaseHelper.saveWatchPosition(
                            videoId!!,
                            exoPlayer.currentPosition
                        )
                    }
                }

                // listen for the stop button in the notification
                if (playbackState == PlaybackState.STATE_STOPPED && SDK_INT >= Build.VERSION_CODES.O) {
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
            if (response.duration <= 0) {
                Toast.makeText(context, R.string.cannotDownload, Toast.LENGTH_SHORT).show()
            } else if (!DownloadService.IS_DOWNLOAD_RUNNING) {
                val newFragment = DownloadDialog(videoId!!)
                newFragment.show(childFragmentManager, DownloadDialog::class.java.name)
            } else {
                Toast.makeText(context, R.string.dlisinprogress, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        if (response.hls != null) {
            binding.relPlayerOpen.setOnClickListener {
                // Do not start picture in picture when playing in external player
                videoShownInExternalPlayer = true

                // start an intent with video as mimetype using the hls stream
                val uri: Uri = Uri.parse(response.hls)
                val intent = Intent()

                intent.action = Intent.ACTION_VIEW
                intent.setDataAndType(uri, "video/*")
                intent.putExtra(Intent.EXTRA_TITLE, streams.title)
                intent.putExtra("title", streams.title)
                intent.putExtra("artist", streams.uploader)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, R.string.no_player_found, Toast.LENGTH_SHORT).show()
                }
            }
        }
        initializeRelatedVideos(response.relatedStreams)
        // set video description
        val description = response.description!!
        binding.playerDescription.text =
            // detect whether the description is html formatted
            if (description.contains("<") && description.contains(">")) {
                if (SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(description, Html.FROM_HTML_MODE_COMPACT)
                        .trim()
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(description).trim()
                }
            } else {
                description
            }

        binding.playerChannel.setOnClickListener {
            val activity = view?.context as MainActivity
            val bundle = bundleOf(IntentData.channelId to response.uploaderUrl)
            activity.navController.navigate(R.id.channelFragment, bundle)
            activity.binding.mainMotionLayout.transitionToEnd()
            binding.playerMotionLayout.transitionToEnd()
        }

        // update the subscribed state
        binding.playerSubscribe.setupSubscriptionButton(
            streams.uploaderUrl?.toID(),
            streams.uploader
        )

        if (token != "") {
            binding.relPlayerSave.setOnClickListener {
                val newFragment = AddToPlaylistDialog()
                val bundle = Bundle()
                bundle.putString(IntentData.videoId, videoId)
                newFragment.arguments = bundle
                newFragment.show(childFragmentManager, AddToPlaylistDialog::class.java.name)
            }
        } else {
            binding.relPlayerSave.setOnClickListener {
                Toast.makeText(context, R.string.login_first, Toast.LENGTH_SHORT).show()
            }
        }

        // next and previous buttons
        playerBinding.skipPrev.visibility = if (
            PlayerHelper.skipButtonsEnabled && PlayingQueue.hasPrev()
        ) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        playerBinding.skipNext.visibility =
            if (PlayerHelper.skipButtonsEnabled) View.VISIBLE else View.INVISIBLE

        playerBinding.skipPrev.setOnClickListener {
            videoId = PlayingQueue.getPrev()
            playVideo()
        }

        playerBinding.skipNext.setOnClickListener {
            playNextVideo()
        }
    }

    private fun initializeRelatedVideos(relatedStreams: List<StreamItem>?) {
        if (!PlayerHelper.relatedStreamsEnabled) return

        if (PlayerHelper.alternativeVideoLayout) {
            binding.alternativeTrendingRec.adapter = VideosAdapter(
                relatedStreams.orEmpty().toMutableList(),
                childFragmentManager,
                forceMode = VideosAdapter.Companion.ForceMode.RELATED
            )
        } else {
            binding.relatedRecView.adapter = VideosAdapter(
                relatedStreams.orEmpty().toMutableList(),
                childFragmentManager
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
            "${chapter.title} (${chapter.start?.let { DateUtils.formatElapsedTime(it) }})"
        }
        playerBinding.chapterLL.setOnClickListener {
            if (viewModel.isFullscreen.value!!) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.chapters)
                    .setItems(titles.toTypedArray()) { _, index ->
                        exoPlayer.seekTo(
                            chapters[index].start!! * 1000
                        )
                    }
                    .show()
            } else {
                toggleDescription()
            }
        }
        setCurrentChapterName()
    }

    // set the name of the video chapter in the exoPlayerView
    private fun setCurrentChapterName() {
        // return if chapters are empty to avoid crashes
        if (chapters.isEmpty()) return

        // call the function again in 100ms
        exoPlayerView.postDelayed(this::setCurrentChapterName, 100)

        val chapterIndex = getCurrentChapterIndex()
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
    private fun getCurrentChapterIndex(): Int {
        val currentPosition = exoPlayer.currentPosition
        var chapterIndex = 0

        chapters.forEachIndexed { index, chapter ->
            // check whether the chapter start is greater than the current player position
            if (currentPosition >= chapter.start!! * 1000) {
                // save chapter title if found
                chapterIndex = index
            }
        }
        return chapterIndex
    }

    private fun setMediaSource(uri: Uri, mimeType: String) {
        val mediaItem: MediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(mimeType)
            .setSubtitleConfigurations(subtitles)
            .build()
        exoPlayer.setMediaItem(mediaItem)
    }

    private fun getAvailableResolutions(): List<VideoResolution> {
        if (!this::streams.isInitialized) return listOf()

        val resolutions = mutableListOf<VideoResolution>()

        // append hls to list if available
        if (streams.hls != null) {
            resolutions.add(
                VideoResolution(
                    name = getString(R.string.hls),
                    adaptiveSourceUrl = streams.hls!!
                )
            )
        }

        val videoStreams = try {
            // attempt to sort the qualities, catch if there was an error ih parsing
            streams.videoStreams?.sortedBy {
                it.quality
                    .toString()
                    .split("p")
                    .first()
                    .replace("p", "")
                    .toLong()
            }?.reversed()
                .orEmpty()
        } catch (_: Exception) {
            streams.videoStreams.orEmpty()
        }

        for (vid in videoStreams) {
            // append quality to list if it has the preferred format (e.g. MPEG)
            val preferredMimeType = "video/${PlayerHelper.videoFormatPreference}"
            if (vid.url != null && vid.mimeType == preferredMimeType) { // preferred format
                resolutions.add(
                    VideoResolution(
                        name = vid.quality!!,
                        resolution = vid.quality.toString().split("p").first().toInt()
                    )
                )
            } else if (vid.quality.equals("LBRY") && vid.format.equals("MP4")) { // LBRY MP4 format
                resolutions.add(
                    VideoResolution(
                        name = "LBRY MP4",
                        adaptiveSourceUrl = vid.url,
                        resolution = Int.MAX_VALUE
                    )
                )
            }
        }
        return resolutions
    }

    private fun setResolutionAndSubtitles() {
        // create a list of subtitles
        subtitles = mutableListOf()
        val subtitlesNamesList = mutableListOf(context?.getString(R.string.none)!!)
        val subtitleCodesList = mutableListOf("")
        streams.subtitles.orEmpty().forEach {
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
        val newParams = trackSelector.buildUponParameters()
        if (PlayerHelper.defaultSubtitleCode != "" && subtitleCodesList.contains(PlayerHelper.defaultSubtitleCode)) {
            newParams
                .setPreferredTextLanguage(PlayerHelper.defaultSubtitleCode)
                .setPreferredTextRoleFlags(C.ROLE_FLAG_CAPTION)
        } else {
            newParams.setPreferredTextLanguage(null)
        }

        trackSelector.setParameters(newParams)

        // set media source and resolution in the beginning
        setStreamSource(
            streams
        )
    }

    private fun setStreamSource(streams: Streams) {
        val defaultResolution = PlayerHelper.getDefaultResolution(requireContext())
        if (defaultResolution != "") {
            val params = trackSelector.buildUponParameters()
                .setMaxVideoSize(Int.MAX_VALUE, defaultResolution.toInt())
                .setMinVideoSize(Int.MAX_VALUE, defaultResolution.toInt())
            trackSelector.setParameters(params)
        }

        val manifest = DashHelper.createManifest(streams)

        // encode to base64
        val encoded = Base64.encodeToString(manifest.toByteArray(), Base64.DEFAULT)
        val mediaItem = "data:application/dash+xml;charset=utf-8;base64,$encoded"

        this.setMediaSource(mediaItem.toUri(), MimeTypes.APPLICATION_MPD)
    }

    private fun createExoPlayer() {
        val cronetEngine: CronetEngine = CronetHelper.getCronetEngine()
        val cronetDataSourceFactory: CronetDataSource.Factory =
            CronetDataSource.Factory(cronetEngine, Executors.newCachedThreadPool())

        val dataSourceFactory = DefaultDataSource.Factory(
            requireContext(),
            cronetDataSourceFactory
        )

        // handles the audio focus
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        // handles the duration of media to retain in the buffer prior to the current playback position (for fast backward seeking)
        val loadControl = DefaultLoadControl.Builder()
            // cache the last three minutes
            .setBackBuffer(1000 * 60 * 3, true)
            .setBufferDurationsMs(
                1000 * 10, // exo default is 50s
                PlayerHelper.bufferingGoal,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()

        // control for the track sources like subtitles and audio source
        trackSelector = DefaultTrackSelector(requireContext())

        val params = trackSelector.buildUponParameters().setPreferredAudioLanguage(
            Locale.getDefault().language.lowercase().substring(0, 2)
        )
        trackSelector.setParameters(params)

        // limit hls to full hd
        if (
            PreferenceHelper.getBoolean(
                PreferenceKeys.LIMIT_HLS,
                false
            )
        ) {
            val newParams = trackSelector.buildUponParameters()
                .setMaxVideoSize(1920, 1080)
            trackSelector.setParameters(newParams)
        }

        exoPlayer = ExoPlayer.Builder(requireContext())
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer.setAudioAttributes(audioAttributes, true)
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

    private fun fetchComments() {
        lifecycleScope.launchWhenCreated {
            val commentsResponse = try {
                RetrofitInstance.api.getComments(videoId!!)
            } catch (e: IOException) {
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection")
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response")
                return@launchWhenCreated
            }
            commentsAdapter = CommentsAdapter(videoId!!, commentsResponse.comments)
            binding.commentsRecView.adapter = commentsAdapter
            nextPage = commentsResponse.nextpage
            isLoading = false
        }
    }

    private fun fetchNextComments() {
        lifecycleScope.launchWhenCreated {
            if (!isLoading) {
                isLoading = true
                val response = try {
                    RetrofitInstance.api.getCommentsNextPage(videoId!!, nextPage!!)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG(), "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG(), "HttpException, unexpected response," + e.response())
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                commentsAdapter?.updateItems(response.comments)
                isLoading = false
            }
        }
    }

    /**
     * Use the sensor mode if auto fullscreen is enabled
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private fun changeOrientationMode() {
        val mainActivity = activity as MainActivity
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
        if (!this@PlayerFragment::streams.isInitialized ||
            streams.subtitles == null ||
            streams.subtitles!!.isEmpty()
        ) {
            Toast.makeText(context, R.string.no_subtitles_available, Toast.LENGTH_SHORT).show()
            return
        }

        val subtitlesNamesList = mutableListOf(context?.getString(R.string.none)!!)
        val subtitleCodesList = mutableListOf("")
        streams.subtitles!!.forEach {
            subtitlesNamesList += it.name!!
            subtitleCodesList += it.code!!
        }

        BaseBottomSheet()
            .setSimpleItems(subtitlesNamesList) { index ->
                val newParams = if (index != 0) {
                    // caption selected

                    // get the caption language code
                    val captionLanguageCode = subtitleCodesList[index]

                    // select the new caption preference
                    trackSelector.buildUponParameters()
                        .setPreferredTextLanguage(captionLanguageCode)
                        .setPreferredTextRoleFlags(C.ROLE_FLAG_CAPTION)
                } else {
                    // none selected
                    // disable captions
                    trackSelector.buildUponParameters()
                        .setPreferredTextLanguage(null)
                }

                // set the new caption language
                trackSelector.setParameters(newParams)
            }
            .show(childFragmentManager)
    }

    override fun onQualityClicked() {
        // get the available resolutions
        val resolutions = getAvailableResolutions()

        // Dialog for quality selection
        val lastPosition = exoPlayer.currentPosition
        BaseBottomSheet()
            .setSimpleItems(
                resolutions.map { it.name }
            ) { which ->
                if (
                    resolutions[which].adaptiveSourceUrl != null
                ) {
                    // set the progressive media source
                    setMediaSource(resolutions[which].adaptiveSourceUrl!!.toUri(), MimeTypes.APPLICATION_M3U8)
                } else {
                    val resolution = resolutions[which].resolution!!
                    val params = trackSelector.buildUponParameters()
                        .setMaxVideoSize(Int.MAX_VALUE, resolution)
                        .setMinVideoSize(Int.MAX_VALUE, resolution)
                    trackSelector.setParameters(params)
                }
                exoPlayer.seekTo(lastPosition)
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
                val lang = audioStreams.first().audioTrackId!!.substring(0, 2)
                val newParams = trackSelector.buildUponParameters()
                    .setPreferredAudioLanguage(lang)
                trackSelector.setParameters(newParams)
            }
            .show(childFragmentManager)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            // set portrait mode
            unsetFullscreen()

            // hide and disable exoPlayer controls
            exoPlayerView.hideController()
            exoPlayerView.useController = false

            with(binding.playerMotionLayout) {
                getConstraintSet(R.id.start).constrainHeight(R.id.player, -1)
                enableTransition(R.id.yt_transition, false)
            }
            binding.linLayout.visibility = View.GONE

            viewModel.isFullscreen.value = false
        } else if (lifecycle.currentState == Lifecycle.State.CREATED) {
            // close button got clicked in PiP mode
            // destroying the fragment, player and notification
            onDestroy()
            // finish the activity
            activity?.finishAndRemoveTask()
        } else {
            // enable exoPlayer controls again
            exoPlayerView.useController = true

            with(binding.playerMotionLayout) {
                getConstraintSet(R.id.start).constrainHeight(R.id.player, 0)
                enableTransition(R.id.yt_transition, true)
            }
            binding.linLayout.visibility = View.VISIBLE
        }
    }

    fun onUserLeaveHint() {
        if (SDK_INT >= Build.VERSION_CODES.O && shouldStartPiP()) {
            activity?.enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setActions(emptyList())
                    .build()
            )
        }
    }

    private fun shouldStartPiP(): Boolean {
        if (!PlayerHelper.pipEnabled ||
            exoPlayer.playbackState == PlaybackState.STATE_PAUSED ||
            videoShownInExternalPlayer
        ) {
            return false
        }

        val bounds = Rect()
        binding.playerScrollView.getHitRect(bounds)

        val backgroundModeRunning = isServiceRunning(requireContext(), BackgroundMode::class.java)

        return (
            binding.playerScrollView.getLocalVisibleRect(bounds) ||
                viewModel.isFullscreen.value == true
            ) && (exoPlayer.isPlaying || !backgroundModeRunning)
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (!PlayerHelper.autoRotationEnabled) return
        when (newConfig.orientation) {
            // go to fullscreen mode
            Configuration.ORIENTATION_LANDSCAPE -> setFullscreen()
            // exit fullscreen if not landscape
            else -> unsetFullscreen()
        }
    }
}
