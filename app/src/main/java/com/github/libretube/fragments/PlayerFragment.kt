package com.github.libretube.fragments

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.text.Html
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.Globals
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.activities.hideKeyboard
import com.github.libretube.adapters.ChaptersAdapter
import com.github.libretube.adapters.CommentsAdapter
import com.github.libretube.adapters.TrendingAdapter
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.databinding.FragmentPlayerBinding
import com.github.libretube.dialogs.AddtoPlaylistDialog
import com.github.libretube.dialogs.DownloadDialog
import com.github.libretube.dialogs.ShareDialog
import com.github.libretube.obj.ChapterSegment
import com.github.libretube.obj.Playlist
import com.github.libretube.obj.Segment
import com.github.libretube.obj.Segments
import com.github.libretube.obj.SponsorBlockPrefs
import com.github.libretube.obj.StreamItem
import com.github.libretube.obj.Streams
import com.github.libretube.obj.Subscribe
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.services.IS_DOWNLOAD_RUNNING
import com.github.libretube.util.BackgroundMode
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.CronetHelper
import com.github.libretube.util.DescriptionAdapter
import com.github.libretube.util.PlayerHelper
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.formatShort
import com.github.libretube.views.DoubleClickListener
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import com.google.android.exoplayer2.MediaItem.fromUri
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.cronet.CronetDataSource
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.RepeatModeUtil
import com.google.android.exoplayer2.video.VideoSize
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.chromium.net.CronetEngine
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.math.abs

class PlayerFragment : Fragment() {

    private val TAG = "PlayerFragment"
    private lateinit var binding: FragmentPlayerBinding
    private lateinit var playerBinding: ExoStyledPlayerControlViewBinding

    private var videoId: String? = null
    private var playlistId: String? = null
    private var sId: Int = 0
    private var eId: Int = 0
    private var paused = false
    private var whichQuality = 0
    private var transitioning = false
    private var autoplay = false
    private var isZoomed: Boolean = false

    private var isSubscribed: Boolean = false

    private var commentsAdapter: CommentsAdapter? = null
    private var commentsLoaded: Boolean? = false
    private var nextPage: String? = null
    private var isLoading = true
    private lateinit var exoPlayerView: StyledPlayerView
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var segmentData: Segments
    private var relatedStreamsEnabled = true

    private var relatedStreams: List<StreamItem>? = arrayListOf()
    private var nextStreamId: String? = null
    private var playlistStreamIds: MutableList<String> = arrayListOf()
    private var playlistNextPage: String? = null

    private var isPlayerLocked: Boolean = false

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var playerNotification: PlayerNotificationManager

    private lateinit var title: String
    private lateinit var uploader: String
    private lateinit var thumbnailUrl: String
    private lateinit var chapters: List<ChapterSegment>
    private val sponsorBlockPrefs = SponsorBlockPrefs()
    private lateinit var subtitle: MutableList<SubtitleConfiguration>

    private var autoRotationEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoId = it.getString("videoId")
            playlistId = it.getString("playlistId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerBinding.inflate(layoutInflater, container, false)
        playerBinding = binding.player.binding
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideKeyboard()

        // save whether auto rotation is enabled
        autoRotationEnabled = PreferenceHelper.getBoolean(
            requireContext(),
            "auto_fullscreen",
            false
        )
        val mainActivity = activity as MainActivity
        if (autoRotationEnabled) {
            // enable auto rotation
            mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            onConfigurationChanged(resources.configuration)
        } else {
            // go to portrait mode
            mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }

        // save whether related streams and autoplay are enabled
        autoplay = PreferenceHelper.getBoolean(
            requireContext(),
            "autoplay",
            false
        )
        relatedStreamsEnabled = PreferenceHelper.getBoolean(
            requireContext(),
            "related_streams_toggle",
            true
        )

        setSponsorBlockPrefs()
        createExoPlayer(view)
        initializeTransitionLayout(view)
        playVideo(view)
    }

    private fun initializeTransitionLayout(view: View) {
        videoId = videoId!!.replace("/watch?v=", "")

        val mainActivity = activity as MainActivity
        mainActivity.binding.container.visibility = View.VISIBLE

        exoPlayerView = binding.player

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
                val mainActivity = activity as MainActivity
                val mainMotionLayout =
                    mainActivity.binding.mainMotionLayout
                mainMotionLayout.progress = abs(progress)
                exoPlayerView.hideController()
                eId = endId
                sId = startId
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                println(currentId)
                val mainActivity = activity as MainActivity
                val mainMotionLayout =
                    mainActivity.binding.mainMotionLayout
                if (currentId == eId) {
                    Globals.isMiniPlayerVisible = true
                    exoPlayerView.useController = false
                    mainMotionLayout.progress = 1F
                } else if (currentId == sId) {
                    Globals.isMiniPlayerVisible = false
                    exoPlayerView.useController = true
                    mainMotionLayout.progress = 0F
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

        binding.closeImageView.setOnClickListener {
            Globals.isMiniPlayerVisible = false
            binding.playerMotionLayout.transitionToEnd()
            val mainActivity = activity as MainActivity
            mainActivity.supportFragmentManager.beginTransaction()
                .remove(this)
                .commit()
        }
        playerBinding.closeImageButton.setOnClickListener {
            Globals.isMiniPlayerVisible = false
            binding.playerMotionLayout.transitionToEnd()
            val mainActivity = activity as MainActivity
            mainActivity.supportFragmentManager.beginTransaction()
                .remove(this)
                .commit()
        }
        // show the advanced player options
        playerBinding.toggleOptions.setOnClickListener {
            if (playerBinding.advancedOptions.isVisible) {
                playerBinding.toggleOptions.animate().rotationX(0F).setDuration(200).start()
                playerBinding.advancedOptions.visibility = View.GONE
            } else {
                playerBinding.toggleOptions.animate().rotationX(180F).setDuration(200).start()
                playerBinding.advancedOptions.visibility = View.VISIBLE
            }
        }
        binding.playImageView.setOnClickListener {
            paused = if (paused) {
                binding.playImageView.setImageResource(R.drawable.ic_pause)
                exoPlayer.play()
                false
            } else {
                binding.playImageView.setImageResource(R.drawable.ic_play)
                exoPlayer.pause()
                true
            }
        }

        // video description and chapters toggle
        binding.playerTitleLayout.setOnClickListener {
            toggleDescription()
        }

        binding.commentsToggle.setOnClickListener {
            toggleComments()
        }

        // FullScreen button trigger
        // hide fullscreen button if auto rotation enabled
        playerBinding.fullscreen.visibility = if (autoRotationEnabled) View.GONE else View.VISIBLE
        playerBinding.fullscreen.setOnClickListener {
            // hide player controller
            exoPlayerView.hideController()
            if (!Globals.isFullScreen) {
                // go to fullscreen mode
                setFullscreen()
            } else {
                // exit fullscreen mode
                unsetFullscreen()
            }
        }

        // switching between original aspect ratio (black bars) and zoomed to fill device screen
        playerBinding.aspectRatioButton.setOnClickListener {
            if (isZoomed) {
                exoPlayerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                isZoomed = false
            } else {
                exoPlayerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                isZoomed = true
            }
        }

        // lock and unlock the player
        playerBinding.lockPlayer.setOnClickListener {
            // change the locked/unlocked icon
            if (!isPlayerLocked) {
                playerBinding.lockPlayer.setImageResource(R.drawable.ic_locked)
            } else {
                playerBinding.lockPlayer.setImageResource(R.drawable.ic_unlocked)
            }

            // show/hide all the controls
            lockPlayer(isPlayerLocked)

            // change locked status
            isPlayerLocked = !isPlayerLocked
        }

        // set default playback speed
        val playbackSpeed =
            PreferenceHelper.getString(requireContext(), "playback_speed", "1F")!!
        val playbackSpeeds = context?.resources?.getStringArray(R.array.playbackSpeed)!!
        val playbackSpeedValues =
            context?.resources?.getStringArray(R.array.playbackSpeedValues)!!
        exoPlayer.setPlaybackSpeed(playbackSpeed.toFloat())
        val speedIndex = playbackSpeedValues.indexOf(playbackSpeed)
        playerBinding.speedText.text = playbackSpeeds[speedIndex]

        // change playback speed button
        playerBinding.speedText.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.change_playback_speed)
                .setItems(playbackSpeeds) { _, index ->
                    // set the new playback speed
                    val newPlaybackSpeed = playbackSpeedValues[index].toFloat()
                    exoPlayer.setPlaybackSpeed(newPlaybackSpeed)
                    playerBinding.speedText.text = playbackSpeeds[index]
                }
                .show()
        }

        // repeat toggle button
        playerBinding.repeatToggle.setOnClickListener {
            if (exoPlayer.repeatMode == RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL) {
                // turn off repeat mode
                exoPlayer.repeatMode = RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE
                playerBinding.repeatToggle.setColorFilter(Color.GRAY)
            } else {
                exoPlayer.repeatMode = RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
                playerBinding.repeatToggle.setColorFilter(Color.WHITE)
            }
        }

        // share button
        binding.relPlayerShare.setOnClickListener {
            val shareDialog = ShareDialog(videoId!!, false)
            shareDialog.show(childFragmentManager, "ShareDialog")
        }

        binding.relPlayerBackground.setOnClickListener {
            // pause the current player
            exoPlayer.pause()

            // start the background mode
            BackgroundMode
                .getInstance()
                .playOnBackgroundMode(
                    requireContext(),
                    videoId!!
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

        binding.commentsRecView.layoutManager = LinearLayoutManager(view.context)
        binding.commentsRecView.setItemViewCacheSize(20)

        binding.relatedRecView.layoutManager =
            GridLayoutManager(view.context, resources.getInteger(R.integer.grid_items))
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
        playerBinding.closeImageButton.visibility = View.GONE

        val mainActivity = activity as MainActivity
        val fullscreenOrientationPref = PreferenceHelper
            .getString(requireContext(), "fullscreen_orientation", "ratio")

        scaleControls(1.3F)

        if (!autoRotationEnabled) {
            // different orientations of the video are only available when auto rotation is disabled
            val orientation = when (fullscreenOrientationPref) {
                "ratio" -> {
                    val videoSize = exoPlayer.videoSize
                    // probably a youtube shorts video
                    if (videoSize.height > videoSize.width) ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                    // a video with normal aspect ratio
                    else ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                }
                "auto" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                "landscape" -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                "portrait" -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            }
            mainActivity.requestedOrientation = orientation
        }

        Globals.isFullScreen = true
    }

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
        playerBinding.closeImageButton.visibility = View.VISIBLE

        scaleControls(1F)

        if (!autoRotationEnabled) {
            // switch back to portrait mode if auto rotation disabled
            val mainActivity = activity as MainActivity
            mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }

        Globals.isFullScreen = false
    }

    private fun scaleControls(scaleFactor: Float) {
        playerBinding.exoPlayPause.scaleX = scaleFactor
        playerBinding.exoPlayPause.scaleY = scaleFactor
    }

    private fun toggleDescription() {
        binding.playerDescriptionArrow.animate().rotationBy(180F).setDuration(250).start()
        binding.descLinLayout.visibility =
            if (binding.descLinLayout.isVisible) View.GONE else View.VISIBLE
    }

    private fun toggleComments() {
        binding.commentsRecView.visibility =
            if (binding.commentsRecView.isVisible) View.GONE else View.VISIBLE
        binding.relatedRecView.visibility =
            if (binding.relatedRecView.isVisible) View.GONE else View.VISIBLE
        if (!commentsLoaded!!) fetchComments()
    }

    override fun onPause() {
        // pause the player if the screen is turned off
        val pausePlayerOnScreenOffEnabled = PreferenceHelper.getBoolean(
            requireContext(),
            "pause_screen_off",
            false
        )

        // check whether the screen is on
        val pm = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = pm.isInteractive

        // pause player if screen off and setting enabled
        if (
            this::exoPlayer.isInitialized && !isScreenOn && pausePlayerOnScreenOffEnabled
        ) {
            exoPlayer.pause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            saveWatchPosition()
            mediaSession.isActive = false
            mediaSession.release()
            mediaSessionConnector.setPlayer(null)
            playerNotification.setPlayer(null)
            val notificationManager = context?.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.cancel(1)
            exoPlayer.release()
        } catch (e: Exception) {
        }
    }

    // save the watch position if video isn't finished and option enabled
    private fun saveWatchPosition() {
        val watchPositionsEnabled = PreferenceHelper.getBoolean(
            requireContext(),
            "watch_positions_toggle",
            true
        )
        if (watchPositionsEnabled && exoPlayer.currentPosition != exoPlayer.duration) {
            PreferenceHelper.saveWatchPosition(
                requireContext(),
                videoId!!,
                exoPlayer.currentPosition
            )
        } else if (watchPositionsEnabled) {
            // delete watch position if video has ended
            PreferenceHelper.removeWatchPosition(requireContext(), videoId!!)
        }
    }

    private fun checkForSegments() {
        if (!exoPlayer.isPlaying || !sponsorBlockPrefs.sponsorBlockEnabled) return

        exoPlayerView.postDelayed(this::checkForSegments, 100)

        if (!::segmentData.isInitialized || segmentData.segments.isEmpty()) {
            return
        }

        segmentData.segments.forEach { segment: Segment ->
            val segmentStart = (segment.segment!![0] * 1000.0f).toLong()
            val segmentEnd = (segment.segment[1] * 1000.0f).toLong()
            val currentPosition = exoPlayer.currentPosition
            if (currentPosition in segmentStart until segmentEnd) {
                if (sponsorBlockPrefs.sponsorNotificationsEnabled) {
                    Toast.makeText(context, R.string.segment_skipped, Toast.LENGTH_SHORT).show()
                }
                exoPlayer.seekTo(segmentEnd)
            }
        }
    }

    private fun playVideo(view: View) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getStreams(videoId!!)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                }
                // for the notification description adapter
                title = response.title!!
                uploader = response.uploader!!
                thumbnailUrl = response.thumbnailUrl!!

                // save related streams for autoplay
                relatedStreams = response.relatedStreams

                runOnUiThread {
                    // set media sources for the player
                    setResolutionAndSubtitles(response)
                    prepareExoPlayerView()
                    initializePlayerView(view, response)
                    seekToWatchPosition()
                    exoPlayer.prepare()
                    exoPlayer.play()
                    exoPlayerView.useController = true
                    initializePlayerNotification(requireContext())
                    fetchSponsorBlockSegments()
                    // show comments if related streams disabled
                    if (!relatedStreamsEnabled) toggleComments()
                    // prepare for autoplay
                    initAutoPlay()
                    val watchHistoryEnabled =
                        PreferenceHelper.getBoolean(requireContext(), "Watch_history_toggle", true)
                    if (watchHistoryEnabled) {
                        PreferenceHelper.addToWatchHistory(requireContext(), videoId!!, response)
                    }
                }
            }
        }
        run()
    }

    private fun seekToWatchPosition() {
        // seek to saved watch position if available
        val watchPositions = PreferenceHelper.getWatchPositions(requireContext())
        var position: Long? = null
        watchPositions.forEach {
            if (it.videoId == videoId) position = it.position
        }
        // support for time stamped links
        val timeStamp: Long? = arguments?.getLong("timeStamp")
        if (timeStamp != null && timeStamp != 0L) {
            position = timeStamp * 1000
        }
        if (position != null) exoPlayer.seekTo(position!!)
    }

    // the function is working recursively
    private fun initAutoPlay() {
        // save related streams for autoplay
        if (autoplay) {
            // if it's a playlist use the next video
            if (playlistId != null) {
                lateinit var playlist: Playlist // var for saving the list in
                // runs only the first time when starting a video from a playlist
                if (playlistStreamIds.isEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        // fetch the playlists videos
                        playlist = RetrofitInstance.api.getPlaylist(playlistId!!)
                        // save the playlist urls in the array
                        playlist.relatedStreams?.forEach { video ->
                            playlistStreamIds += video.url?.replace("/watch?v=", "")!!
                        }
                        // save playlistNextPage for usage if video is not contained
                        playlistNextPage = playlist.nextpage
                        // restart the function after videos are loaded
                        initAutoPlay()
                    }
                }
                // if the playlists contain the video, then save the next video as next stream
                else if (playlistStreamIds.contains(videoId)) {
                    val index = playlistStreamIds.indexOf(videoId)
                    // check whether there's a next video
                    if (index + 1 <= playlistStreamIds.size) {
                        nextStreamId = playlistStreamIds[index + 1]
                    }
                    // fetch the next page of the playlist if the video isn't contained
                } else if (playlistNextPage != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        RetrofitInstance.api.getPlaylistNextPage(playlistId!!, playlistNextPage!!)
                        // append all the playlist item urls to the array
                        playlist.relatedStreams?.forEach { video ->
                            playlistStreamIds += video.url?.replace("/watch?v=", "")!!
                        }
                        // save playlistNextPage for usage if video is not contained
                        playlistNextPage = playlist.nextpage
                        // restart the function after videos are loaded
                        initAutoPlay()
                    }
                }
                // else: the video must be the last video of the playlist so nothing happens

                // if it's not a playlist then use the next related video
            } else if (relatedStreams != null && relatedStreams!!.isNotEmpty()) {
                // save next video from related streams for autoplay
                nextStreamId = relatedStreams!![0].url!!.replace("/watch?v=", "")
            }
        }
    }

    // used for autoplay and skipping to next video
    private fun playNextVideo() {
        // check whether there is a new video in the queue
        // by making sure that the next and the current video aren't the same
        if (videoId != nextStreamId) {
            // save the id of the next stream as videoId and load the next video
            videoId = nextStreamId
            playVideo(view!!)
        }
    }

    private fun setSponsorBlockPrefs() {
        sponsorBlockPrefs.sponsorBlockEnabled =
            PreferenceHelper.getBoolean(requireContext(), "sb_enabled_key", true)
        sponsorBlockPrefs.sponsorNotificationsEnabled =
            PreferenceHelper.getBoolean(requireContext(), "sb_notifications_key", true)
        sponsorBlockPrefs.introEnabled =
            PreferenceHelper.getBoolean(requireContext(), "intro_category_key", false)
        sponsorBlockPrefs.selfPromoEnabled =
            PreferenceHelper.getBoolean(requireContext(), "selfpromo_category_key", false)
        sponsorBlockPrefs.interactionEnabled =
            PreferenceHelper.getBoolean(requireContext(), "interaction_category_key", false)
        sponsorBlockPrefs.sponsorsEnabled =
            PreferenceHelper.getBoolean(requireContext(), "sponsors_category_key", true)
        sponsorBlockPrefs.outroEnabled =
            PreferenceHelper.getBoolean(requireContext(), "outro_category_key", false)
        sponsorBlockPrefs.fillerEnabled =
            PreferenceHelper.getBoolean(requireContext(), "filler_category_key", false)
        sponsorBlockPrefs.musicOffTopicEnabled =
            PreferenceHelper.getBoolean(requireContext(), "music_offtopic_category_key", false)
        sponsorBlockPrefs.previewEnabled =
            PreferenceHelper.getBoolean(requireContext(), "preview_category_key", false)
    }

    private fun fetchSponsorBlockSegments() {
        fun run() {
            lifecycleScope.launch(Dispatchers.IO) {
                if (sponsorBlockPrefs.sponsorBlockEnabled) {
                    val categories: ArrayList<String> = arrayListOf()
                    if (sponsorBlockPrefs.introEnabled) {
                        categories.add("intro")
                    }
                    if (sponsorBlockPrefs.selfPromoEnabled) {
                        categories.add("selfpromo")
                    }
                    if (sponsorBlockPrefs.interactionEnabled) {
                        categories.add("interaction")
                    }
                    if (sponsorBlockPrefs.sponsorsEnabled) {
                        categories.add("sponsor")
                    }
                    if (sponsorBlockPrefs.outroEnabled) {
                        categories.add("outro")
                    }
                    if (sponsorBlockPrefs.fillerEnabled) {
                        categories.add("filler")
                    }
                    if (sponsorBlockPrefs.musicOffTopicEnabled) {
                        categories.add("music_offtopic")
                    }
                    if (sponsorBlockPrefs.previewEnabled) {
                        categories.add("preview")
                    }
                    if (categories.size > 0) {
                        segmentData = try {
                            RetrofitInstance.api.getSegments(
                                videoId!!,
                                "[\"" + TextUtils.join("\",\"", categories) + "\"]"
                            )
                        } catch (e: IOException) {
                            println(e)
                            Log.e(TAG, "IOException, you might not have internet connection")
                            return@launch
                        } catch (e: HttpException) {
                            Log.e(TAG, "HttpException, unexpected response")
                            return@launch
                        }
                    }
                }
            }
        }
        run()
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

        val useSystemCaptionStyle = PreferenceHelper.getBoolean(requireContext(), "system_caption_style", true)
        if (useSystemCaptionStyle) {
            // set the subtitle style
            val captionStyle = PlayerHelper.getCaptionStyle(requireContext())
            exoPlayerView.subtitleView?.setApplyEmbeddedStyles(captionStyle == CaptionStyleCompat.DEFAULT)
            exoPlayerView.subtitleView?.setStyle(captionStyle)
        }
    }

    private fun initializePlayerView(view: View, response: Streams) {
        binding.apply {
            playerViewsInfo.text =
                context?.getString(R.string.views, response.views.formatShort()) +
                " • " + response.uploadDate
            textLike.text = response.likes.formatShort()
            textDislike.text = response.dislikes.formatShort()
            ConnectionHelper.loadImage(response.uploaderAvatar, binding.playerChannelImage)
            playerChannelName.text = response.uploader

            titleTextView.text = response.title
            playerTitle.text = response.title
            playerDescription.text = response.description
        }

        playerBinding.exoTitle.text = response.title

        enableSeekbarPreview()
        enableDoubleTapToSeek()

        // init the chapters recyclerview
        if (response.chapters != null) {
            chapters = response.chapters
            initializeChapters()
        }

        // Listener for play and pause icon change
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && sponsorBlockPrefs.sponsorBlockEnabled) {
                    exoPlayerView.postDelayed(
                        this@PlayerFragment::checkForSegments,
                        100
                    )
                }
            }

            override fun onVideoSizeChanged(
                videoSize: VideoSize
            ) {
                // Set new width/height of view
                // height or width must be cast to float as int/int will give 0

                // Redraw the player container with the new layout height
                val params = binding.player.layoutParams
                params.height = videoSize.height / videoSize.width * params.width
                binding.player.layoutParams = params
                binding.player.requestLayout()
                (binding.mainContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
                    matchConstraintPercentHeight = (videoSize.height / videoSize.width).toFloat()
                }
            }

            @Deprecated(message = "Deprecated", level = DeprecationLevel.HIDDEN)
            override fun onPlayerStateChanged(
                playWhenReady: Boolean,
                playbackState: Int
            ) {
                exoPlayerView.keepScreenOn = !(
                    playbackState == Player.STATE_IDLE ||
                        playbackState == Player.STATE_ENDED ||
                        !playWhenReady
                    )

                // check if video has ended, next video is available and autoplay is enabled.
                if (
                    playbackState == Player.STATE_ENDED &&
                    nextStreamId != null &&
                    !transitioning &&
                    autoplay
                ) {
                    transitioning = true
                    // check whether autoplay is enabled
                    if (autoplay) playNextVideo()
                }

                if (playWhenReady && playbackState == Player.STATE_READY) {
                    // media actually playing
                    transitioning = false
                    binding.playImageView.setImageResource(R.drawable.ic_pause)
                } else if (playWhenReady) {
                    // might be idle (plays after prepare()),
                    // buffering (plays when data available)
                    // or ended (plays when seek away from end)
                    binding.playImageView.setImageResource(R.drawable.ic_play)
                } else {
                    // player paused in any state
                    binding.playImageView.setImageResource(R.drawable.ic_play)
                }
            }
        })

        // check if livestream
        if (response.duration!! > 0) {
            // download clicked
            binding.relPlayerDownload.setOnClickListener {
                if (!IS_DOWNLOAD_RUNNING) {
                    val newFragment = DownloadDialog()
                    val bundle = Bundle()
                    bundle.putString("video_id", videoId)
                    newFragment.arguments = bundle
                    newFragment.show(childFragmentManager, "DownloadDialog")
                } else {
                    Toast.makeText(context, R.string.dlisinprogress, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } else {
            Toast.makeText(context, R.string.cannotDownload, Toast.LENGTH_SHORT).show()
        }

        if (response.hls != null) {
            binding.relPlayerOpen.setOnClickListener {
                // start an intent with video as mimetype using the hls stream
                val uri: Uri = Uri.parse(response.hls)
                val intent = Intent()

                intent.action = Intent.ACTION_VIEW
                intent.setDataAndType(uri, "video/*")
                intent.putExtra(Intent.EXTRA_TITLE, title)
                intent.putExtra("title", title)
                intent.putExtra("artist", uploader)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, R.string.no_player_found, Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (relatedStreamsEnabled) {
            // only show related streams if enabled
            binding.relatedRecView.adapter = TrendingAdapter(
                response.relatedStreams!!,
                childFragmentManager
            )
        }
        // set video description
        val description = response.description!!
        binding.playerDescription.text =
            // detect whether the description is html formatted
            if (description.contains("<") && description.contains(">")) {
                if (SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(description, Html.FROM_HTML_MODE_COMPACT)
                        .trim()
                } else {
                    Html.fromHtml(description).trim()
                }
            } else {
                description
            }

        binding.playerChannel.setOnClickListener {
            val activity = view.context as MainActivity
            val bundle = bundleOf("channel_id" to response.uploaderUrl)
            activity.navController.navigate(R.id.channelFragment, bundle)
            activity.binding.mainMotionLayout.transitionToEnd()
            binding.playerMotionLayout.transitionToEnd()
        }
        val token = PreferenceHelper.getToken(requireContext())
        if (token != "") {
            val channelId = response.uploaderUrl?.replace("/channel/", "")
            isSubscribed(binding.playerSubscribe, channelId!!)
            binding.relPlayerSave.setOnClickListener {
                val newFragment = AddtoPlaylistDialog()
                val bundle = Bundle()
                bundle.putString("videoId", videoId)
                newFragment.arguments = bundle
                newFragment.show(childFragmentManager, "AddToPlaylist")
            }
        }
    }

    private fun enableDoubleTapToSeek() {
        val seekIncrement =
            PreferenceHelper.getString(requireContext(), "seek_increment", "5")?.toLong()!! * 1000

        // enable rewind button
        binding.rewindFL.setOnClickListener(
            DoubleClickListener(
                callback = object : DoubleClickListener.Callback {
                    override fun doubleClicked() {
                        binding.rewindBTN.visibility = View.VISIBLE
                        exoPlayer.seekTo(exoPlayer.currentPosition - seekIncrement)
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.rewindBTN.visibility = View.INVISIBLE
                        }, 700)
                    }

                    override fun singleClicked() {
                        toggleController()
                    }
                }
            )
        )

        // enable fast forward button
        binding.forwardFL.setOnClickListener(
            DoubleClickListener(
                callback = object : DoubleClickListener.Callback {
                    override fun doubleClicked() {
                        binding.forwardBTN.visibility = View.VISIBLE
                        exoPlayer.seekTo(exoPlayer.currentPosition + seekIncrement)
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.forwardBTN.visibility = View.INVISIBLE
                        }, 700)
                    }

                    override fun singleClicked() {
                        toggleController()
                    }
                }
            )
        )
    }

    private fun disableDoubleTapToSeek() {
        // disable fast forward and rewind by double tapping
        binding.forwardFL.visibility = View.GONE
        binding.rewindFL.visibility = View.GONE
    }

    // toggle the visibility of the player controller
    private fun toggleController() {
        if (exoPlayerView.isControllerFullyVisible) exoPlayerView.hideController()
        else exoPlayerView.showController()
    }

    // enable seek bar preview
    private fun enableSeekbarPreview() {
        playerBinding.exoProgress.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                exoPlayer.pause()
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                val minTimeDiff = 10 * 1000 // 10s
                // get the difference between the new and the old position
                val diff = abs(exoPlayer.currentPosition - position)
                // seek only when the difference is greater than 10 seconds
                if (diff >= minTimeDiff) exoPlayer.seekTo(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                exoPlayer.seekTo(position)
                exoPlayer.play()
                Handler(Looper.getMainLooper()).postDelayed({
                    exoPlayerView.hideController()
                }, 200)
            }
        })
    }

    private fun initializeChapters() {
        if (chapters.isNotEmpty()) {
            // enable chapters in the video description
            binding.chaptersRecView.layoutManager =
                LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
            binding.chaptersRecView.adapter = ChaptersAdapter(chapters, exoPlayer)
            binding.chaptersRecView.visibility = View.VISIBLE

            // enable the chapters dialog in the player
            val titles = mutableListOf<String>()
            chapters.forEach {
                titles += it.title!!
            }
            playerBinding.chapterLL.visibility = View.VISIBLE
            playerBinding.chapterLL.setOnClickListener {
                if (Globals.isFullScreen) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.chapters)
                        .setItems(titles.toTypedArray()) { _, index ->
                            val position = chapters[index].start!! * 1000
                            exoPlayer.seekTo(position)
                        }
                        .show()
                } else {
                    toggleDescription()
                }
            }
            setCurrentChapterName()
        }
    }

    // set the name of the video chapter in the exoPlayerView
    private fun setCurrentChapterName() {
        // call the function again in 100ms
        exoPlayerView.postDelayed(this::setCurrentChapterName, 100)

        val chapterName = getCurrentChapterName()

        // change the chapter name textView text to the chapterName
        if (chapterName != null && chapterName != playerBinding.chapterName.text) {
            playerBinding.chapterName.text = chapterName
        }
    }

    // get the name of the currently played chapter
    private fun getCurrentChapterName(): String? {
        val currentPosition = exoPlayer.currentPosition
        var chapterName: String? = null

        chapters.forEach {
            // check whether the chapter start is greater than the current player position
            if (currentPosition >= it.start!! * 1000) {
                // save chapter title if found
                chapterName = it.title
            }
        }
        return chapterName
    }

    private fun setMediaSource(
        videoUri: Uri,
        audioUrl: String
    ) {
        val dataSourceFactory: DataSource.Factory =
            DefaultHttpDataSource.Factory()
        val videoItem: MediaItem = MediaItem.Builder()
            .setUri(videoUri)
            .setSubtitleConfigurations(subtitle)
            .build()
        val videoSource: MediaSource =
            DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(videoItem)
        val audioSource: MediaSource =
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(fromUri(audioUrl))
        val mergeSource: MediaSource =
            MergingMediaSource(videoSource, audioSource)
        exoPlayer.setMediaSource(mergeSource)
    }

    private fun setResolutionAndSubtitles(response: Streams) {
        val videoFormatPreference =
            PreferenceHelper.getString(requireContext(), "player_video_format", "WEBM")

        var videosNameArray: Array<CharSequence> = arrayOf()
        var videosUrlArray: Array<Uri> = arrayOf()

        // append hls to list if available
        if (response.hls != null) {
            videosNameArray += getString(R.string.hls)
            videosUrlArray += response.hls.toUri()
        }

        for (vid in response.videoStreams!!) {
            // append quality to list if it has the preferred format (e.g. MPEG)
            if (vid.format.equals(videoFormatPreference) && vid.url != null) { // preferred format
                videosNameArray += vid.quality.toString()
                videosUrlArray += vid.url!!.toUri()
            } else if (vid.quality.equals("LBRY") && vid.format.equals("MP4")) { // LBRY MP4 format
                videosNameArray += "LBRY MP4"
                videosUrlArray += vid.url!!.toUri()
            }
        }
        // create a list of subtitles
        subtitle = mutableListOf<SubtitleConfiguration>()
        response.subtitles!!.forEach {
            subtitle.add(
                SubtitleConfiguration.Builder(it.url!!.toUri())
                    .setMimeType(it.mimeType!!) // The correct MIME type (required).
                    .setLanguage(it.code) // The subtitle language (optional).
                    .build()
            )
        }
        // set media source and resolution in the beginning
        setStreamSource(
            response,
            videosNameArray,
            videosUrlArray
        )

        playerBinding.qualityText.setOnClickListener {
            // Dialog for quality selection
            val builder: MaterialAlertDialogBuilder? = activity?.let {
                MaterialAlertDialogBuilder(it)
            }
            val lastPosition = exoPlayer.currentPosition
            builder!!.setTitle(R.string.choose_quality_dialog)
                .setItems(
                    videosNameArray
                ) { _, which ->
                    whichQuality = which
                    if (
                        videosNameArray[which] == getString(R.string.hls) ||
                        videosNameArray[which] == "LBRY HLS"
                    ) {
                        // no need to merge sources if using hls
                        val mediaItem: MediaItem = MediaItem.Builder()
                            .setUri(videosUrlArray[which])
                            .setSubtitleConfigurations(subtitle)
                            .build()
                        exoPlayer.setMediaItem(mediaItem)
                    } else {
                        val videoUri = videosUrlArray[which]
                        val audioUrl = PlayerHelper.getMostBitRate(response.audioStreams!!)
                        setMediaSource(videoUri, audioUrl)
                    }
                    exoPlayer.seekTo(lastPosition)
                    playerBinding.qualityText.text = videosNameArray[which]
                }
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun setStreamSource(
        streams: Streams,
        videosNameArray: Array<CharSequence>,
        videosUrlArray: Array<Uri>
    ) {
        val defRes = PreferenceHelper.getString(
            requireContext(),
            "default_resolution",
            "hls"
        )!!

        if (defRes != "hls") {
            videosNameArray.forEachIndexed { index, pipedStream ->
                // search for quality preference in the available stream sources
                if (pipedStream.contains(defRes)) {
                    val videoUri = videosUrlArray[index]
                    val audioUrl = PlayerHelper.getMostBitRate(streams.audioStreams!!)
                    setMediaSource(videoUri, audioUrl)
                    playerBinding.qualityText.text = videosNameArray[index]
                    return
                }
            }
        }

        // if default resolution isn't set or available, use hls if available
        if (streams.hls != null) {
            val mediaItem: MediaItem = MediaItem.Builder()
                .setUri(streams.hls)
                .setSubtitleConfigurations(subtitle)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            playerBinding.qualityText.text = context?.getString(R.string.hls)
            return
        }

        // if nothing found, use the first list entry
        if (videosUrlArray.isNotEmpty()) {
            val videoUri = videosUrlArray[0]
            val audioUrl = PlayerHelper.getMostBitRate(streams.audioStreams!!)
            setMediaSource(videoUri, audioUrl)
            playerBinding.qualityText.text = videosNameArray[0]
        }
    }

    private fun createExoPlayer(view: View) {
        val bufferingGoal =
            PreferenceHelper.getString(requireContext(), "buffering_goal", "50")?.toInt()!! * 1000

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
            .setContentType(C.CONTENT_TYPE_MOVIE)
            .build()

        // handles the duration of media to retain in the buffer prior to the current playback position (for fast backward seeking)
        val loadControl = DefaultLoadControl.Builder()
            // cache the last three minutes
            .setBackBuffer(1000 * 60 * 3, true)
            .setBufferDurationsMs(
                1000 * 10, // exo default is 50s
                bufferingGoal,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()

        exoPlayer = ExoPlayer.Builder(view.context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .build()

        exoPlayer.setAudioAttributes(audioAttributes, true)
    }

    private fun initializePlayerNotification(c: Context) {
        mediaSession = MediaSessionCompat(c, this.javaClass.name)
        mediaSession.apply {
            isActive = true
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(exoPlayer)

        playerNotification = PlayerNotificationManager
            .Builder(c, 1, "background_mode")
            .setMediaDescriptionAdapter(
                DescriptionAdapter(title, uploader, thumbnailUrl, requireContext())
            )
            .build()

        playerNotification.apply {
            setPlayer(exoPlayer)
            setUsePreviousAction(false)
            setUseStopAction(true)
            setMediaSessionToken(mediaSession.sessionToken)
        }
    }

    // lock the player
    private fun lockPlayer(isLocked: Boolean) {
        val visibility = if (isLocked) View.VISIBLE else View.GONE

        playerBinding.exoTopBarRight.visibility = visibility
        playerBinding.exoPlayPause.visibility = visibility
        playerBinding.exoBottomBar.visibility = visibility
        playerBinding.closeImageButton.visibility = visibility
        playerBinding.exoTitle.visibility =
            if (isLocked &&
                Globals.isFullScreen
            ) View.VISIBLE else View.INVISIBLE

        // hide the close image button
        playerBinding.closeImageButton.visibility =
            if (isLocked &&
                !Globals.isFullScreen &&
                autoRotationEnabled
            ) View.VISIBLE else View.GONE

        // disable double tap to seek when the player is locked
        if (isLocked) enableDoubleTapToSeek() else disableDoubleTapToSeek()
    }

    private fun isSubscribed(button: MaterialButton, channel_id: String) {
        @SuppressLint("ResourceAsColor")
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val token = PreferenceHelper.getToken(requireContext())
                    RetrofitInstance.authApi.isSubscribed(
                        channel_id,
                        token
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }

                runOnUiThread {
                    if (response.subscribed == true) {
                        isSubscribed = true
                        button.text = getString(R.string.unsubscribe)
                    }
                    if (response.subscribed != null) {
                        button.setOnClickListener {
                            if (isSubscribed) {
                                unsubscribe(channel_id)
                                button.text = getString(R.string.subscribe)
                            } else {
                                subscribe(channel_id)
                                button.text = getString(R.string.unsubscribe)
                            }
                        }
                    }
                }
            }
        }
        run()
    }

    private fun subscribe(channel_id: String) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                try {
                    val token = PreferenceHelper.getToken(requireContext())
                    RetrofitInstance.authApi.subscribe(
                        token,
                        Subscribe(channel_id)
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response$e")
                    return@launchWhenCreated
                }
                isSubscribed = true
            }
        }
        run()
    }

    private fun unsubscribe(channel_id: String) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                try {
                    val token = PreferenceHelper.getToken(requireContext())
                    RetrofitInstance.authApi.unsubscribe(
                        token,
                        Subscribe(channel_id)
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                isSubscribed = false
            }
        }
        run()
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }

    private fun fetchComments() {
        lifecycleScope.launchWhenCreated {
            val commentsResponse = try {
                RetrofitInstance.api.getComments(videoId!!)
            } catch (e: IOException) {
                println(e)
                Log.e(TAG, "IOException, you might not have internet connection")
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG, "HttpException, unexpected response")
                return@launchWhenCreated
            }
            commentsAdapter = CommentsAdapter(videoId!!, commentsResponse.comments)
            binding.commentsRecView.adapter = commentsAdapter
            nextPage = commentsResponse.nextpage
            commentsLoaded = true
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
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response," + e.response())
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                commentsAdapter?.updateItems(response.comments)
                isLoading = false
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            // hide and disable exoPlayer controls
            exoPlayerView.hideController()
            exoPlayerView.useController = false

            // set portrait mode
            unsetFullscreen()

            Globals.isFullScreen = false
        } else {
            // enable exoPlayer controls again
            exoPlayerView.useController = true
        }
    }

    fun onUserLeaveHint() {
        val bounds = Rect()
        binding.playerScrollView.getHitRect(bounds)

        if (SDK_INT >= Build.VERSION_CODES.O &&
            exoPlayer.isPlaying && (binding.playerScrollView.getLocalVisibleRect(bounds) || Globals.isFullScreen)
        ) {
            activity?.enterPictureInPictureMode(updatePipParams())
        }
    }

    private fun updatePipParams() = PictureInPictureParams.Builder()
        .setActions(emptyList())
        .build()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (autoRotationEnabled) {
            val orientation = newConfig.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // go to fullscreen mode
                setFullscreen()
            } else {
                // exit fullscreen if not landscape
                unsetFullscreen()
            }
        }
    }
}
