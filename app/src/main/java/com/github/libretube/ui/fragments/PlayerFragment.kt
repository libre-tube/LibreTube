package com.github.libretube.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.KeyEvent
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.TransitionAdapter
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import androidx.core.view.WindowCompat
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.api.obj.Segment
import com.github.libretube.api.obj.Streams
import com.github.libretube.compat.PictureInPictureCompat
import com.github.libretube.compat.PictureInPictureParamsCompat
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentPlayerBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.enums.FileType
import com.github.libretube.enums.PlayerCommand
import com.github.libretube.enums.PlayerEvent
import com.github.libretube.enums.SbSkipOptions
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.serializableExtra
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.togglePlayPauseState
import com.github.libretube.extensions.updateIfChanged
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.DownloadHelper
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavBarHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PlayerHelper.getCurrentSegment
import com.github.libretube.helpers.ThemeHelper
import com.github.libretube.helpers.WindowHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.services.AbstractPlayerService
import com.github.libretube.services.OfflinePlayerService
import com.github.libretube.services.OnlinePlayerService
import com.github.libretube.ui.activities.AbstractPlayerHostActivity
import com.github.libretube.ui.activities.NoInternetActivity
import com.github.libretube.ui.adapters.VideoCardsAdapter
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.dialogs.AddToPlaylistDialog
import com.github.libretube.ui.dialogs.PlayOfflineDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.extensions.animateDown
import com.github.libretube.ui.extensions.getSystemInsets
import com.github.libretube.ui.extensions.setOnBackPressed
import com.github.libretube.ui.extensions.setupSubscriptionButton
import com.github.libretube.ui.interfaces.CustomPlayerCallback
import com.github.libretube.ui.interfaces.TimeFrameReceiver
import com.github.libretube.ui.listeners.SeekbarPreviewListener
import com.github.libretube.ui.models.ChaptersViewModel
import com.github.libretube.ui.models.CommentsViewModel
import com.github.libretube.ui.models.CommonPlayerViewModel
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.ui.sheets.CommentsSheet
import com.github.libretube.util.OfflineTimeFrameReceiver
import com.github.libretube.util.OnlineTimeFrameReceiver
import com.github.libretube.util.PlayingQueue
import com.github.libretube.util.TextUtils
import com.github.libretube.util.TextUtils.toTimeInSeconds
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.io.path.exists
import kotlin.math.abs
import kotlin.math.absoluteValue


@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerFragment : Fragment(R.layout.fragment_player), CustomPlayerCallback {
    private var _binding: FragmentPlayerBinding? = null
    val binding get() = _binding!!

    private val playerControlsBinding get() = binding.player.binding
    private val playerBackgroundBinding get() = binding.player.backgroundBinding

    private val commonPlayerViewModel: CommonPlayerViewModel by activityViewModels()
    private val viewModel: PlayerViewModel by viewModels()
    private val commentsViewModel: CommentsViewModel by activityViewModels()
    private val chaptersViewModel: ChaptersViewModel by activityViewModels()
    private lateinit var playerController: MediaController

    // Video information passed by the intent
    private lateinit var videoId: String
    private var playlistId: String? = null
    private var channelId: String? = null
    var isOffline: Boolean = false
        private set

    // data and objects stored for the player
    private lateinit var streams: Streams
    private val isShort: Boolean
        get() {
            if (PlayingQueue.getCurrent()?.isShort == true) return true
            if (!::playerController.isInitialized) return false

            val currentVideoFormat = PlayerHelper.getCurrentVideoFormat(playerController)
                ?: return false
            return currentVideoFormat.height > currentVideoFormat.width
        }

    private val handler = Handler(Looper.getMainLooper())

    private var seekBarPreviewListener: SeekbarPreviewListener? = null

    // True when the video was closed through the close button on PiP mode
    private var closedVideo = false

    private var autoPlayCountdownEnabled = PlayerHelper.autoPlayCountdown

    /**
     * The orientation of the `fragment_player.xml` that's currently used
     * This is needed in order to figure out if the current layout is the landscape one or not.
     */
    private var playerLayoutOrientation = Int.MIN_VALUE

    // Activity that's active during PiP, can be used for controlling its lifecycle.
    private var pipActivity: Activity? = null

    // check if pip is entered via the dedicated button
    private var isEnteringPiPMode = false

    private val baseActivity get() = activity as AbstractPlayerHostActivity
    private val windowInsetsControllerCompat
        get() = WindowCompat
            .getInsetsController(requireActivity().window, requireActivity().window.decorView)

    private val fullscreenDialog by lazy {
        object : Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
            @Deprecated("Deprecated in Java", ReplaceWith("onbackpressedispatcher and callback"))
            override fun onBackPressed() {
                unsetFullscreen()
            }

            override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
                if (_binding?.player?.onKeyUp(keyCode, event) == true) {
                    return true
                }

                return super.onKeyUp(keyCode, event)
            }
        }
    }

    /**
     * Receiver for all actions in the PiP mode
     */
    private val playerActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val event = intent.serializableExtra<PlayerEvent>(PlayerHelper.CONTROL_TYPE) ?: return

            if (PlayerHelper.handlePlayerAction(playerController, event)) return

            when (event) {
                PlayerEvent.Next -> {
                    PlayingQueue.getNext()?.let { playNextVideo(it) }
                }

                PlayerEvent.Prev -> {
                    PlayingQueue.getPrev()?.let { playNextVideo(it) }
                }

                PlayerEvent.Background -> {
                    switchToAudioMode()
                    // wait some time in order for the service to get started properly
                    handler.postDelayed(500) {
                        pipActivity?.moveTaskToBack(false)
                        pipActivity = null
                    }
                }

                else -> Unit
            }
        }
    }

    private var bufferingTimeoutTask: Runnable? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (PlayerHelper.pipEnabled || PictureInPictureCompat.isInPictureInPictureMode(
                    baseActivity
                )
            ) {
                PictureInPictureCompat.setPictureInPictureParams(requireActivity(), pipParams)
            }

            if (isPlaying && PlayerHelper.sponsorBlockEnabled) {
                handler.postDelayed(
                    this@PlayerFragment::checkForSegments,
                    100
                )
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)

            if (events.containsAny(
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED
                ) && _binding != null
            ) {
                updatePlayPauseButton()
            }
        }

        var fullscreenOnShortsAlreadyDone = false
        override fun onPlaybackStateChanged(playbackState: Int) {
            // set the playback speed to one if having reached the end of a livestream
            if (playbackState == Player.STATE_BUFFERING && binding.player.isLive &&
                playerController.duration - playerController.currentPosition < 700
            ) {
                playerController.setPlaybackSpeed(1f)
            }

            // check if video has ended, next video is available and autoplay is enabled/the video is part of a played playlist.
            if (playbackState == Player.STATE_ENDED) {
                playerBackgroundBinding.sbSkipBtn.isGone = true

                // if the current tracks are empty, the player is transitioning at the moment
                val isTransitioning = playerController.currentTracks.isEmpty
                if (PlayerHelper.isAutoPlayEnabled(playlistId != null) && autoPlayCountdownEnabled && !isTransitioning) {
                    showAutoPlayCountdown()
                } else {
                    binding.player.showControllerPermanently()
                }
            }

            // listen for the stop button in the notification
            if (playbackState == PlaybackState.STATE_STOPPED && PlayerHelper.pipEnabled &&
                PictureInPictureCompat.isInPictureInPictureMode(requireActivity())
            ) {
                // finish PiP by finishing the activity
                activity?.finish()
            }

            // Buffering timeout after 10 Minutes
            if (playbackState == Player.STATE_BUFFERING) {
                if (bufferingTimeoutTask == null) {
                    bufferingTimeoutTask = Runnable {
                        playerController.pause()
                    }
                }

                handler.postDelayed(bufferingTimeoutTask!!, PlayerHelper.MAX_BUFFER_DELAY)
            } else {
                bufferingTimeoutTask?.let { handler.removeCallbacks(it) }
            }

            if (playbackState == Player.STATE_READY && binding.playerMotionLayout.progress == 0f) {
                if (PlayerHelper.autoFullscreenShortsEnabled && isShort && !fullscreenOnShortsAlreadyDone) {
                    setFullscreen()
                    fullscreenOnShortsAlreadyDone = true
                }
            }

            if (playbackState == Player.STATE_ENDED) fullscreenOnShortsAlreadyDone = false

            super.onPlaybackStateChanged(playbackState)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)

            // JSON-encode as work-around for https://github.com/androidx/media/issues/564
            val maybeStreams: Streams? = mediaMetadata.extras?.getString(IntentData.streams)?.let {
                JsonHelper.json.decodeFromString(it)
            }
            maybeStreams?.let { streams ->
                this@PlayerFragment.streams = streams
                viewModel.segments.postValue(emptyList())
                updatePlayerView()
            }
        }

        override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onPlaylistMetadataChanged(mediaMetadata)

            mediaMetadata.extras?.getString(IntentData.videoId)?.let {
                videoId = it
                if (_binding != null) playerBackgroundBinding.autoplayCountdown.cancelAndHideCountdown()

                // fix: if the fragment is recreated, play the current video, and not the initial one
                arguments?.run {
                    val playerData =
                        parcelable<PlayerData>(IntentData.playerData)!!.copy(videoId = videoId)
                    putParcelable(IntentData.playerData, playerData)
                }
            }

            // JSON-encode as work-around for https://github.com/androidx/media/issues/564
            val segments: List<Segment>? =
                mediaMetadata.extras?.getString(IntentData.segments)?.let {
                    JsonHelper.json.decodeFromString(it)
                }
            viewModel.segments.postValue(segments.orEmpty())
        }

        /**
         * Catch player errors to prevent the app from stopping
         */
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            try {
                playerController.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            if (mediaItem == null) {
                toggleVideoInfoVisibility(false)
                disableController()
                binding.titleTextView.text = ""
            }
        }
    }

    private val lockedOrientations = listOf(
        ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT,
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    )

    private var screenshotBitmap: Bitmap? = null
    private val openScreenshotFile =
        registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
            if (uri == null) {
                screenshotBitmap = null
                return@registerForActivityResult
            }

            CoroutineScope(Dispatchers.IO).launch {
                context?.contentResolver?.openOutputStream(uri)?.use { outputStream ->
                    screenshotBitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                screenshotBitmap = null

                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        requireView(),
                        R.string.screenshot_saved,
                        2500
                    ).apply {
                        setAction(R.string.share) {
                            startActivity(Intent.createChooser(with(Intent()) {
                                setAction(Intent.ACTION_SEND)
                                setType("image/png")
                                putExtra(Intent.EXTRA_STREAM, uri)
                            }, null))
                        }
                        show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // broadcast receiver for PiP actions
        ContextCompat.registerReceiver(
            requireContext(),
            playerActionReceiver,
            IntentFilter(PlayerHelper.getIntentActionName(requireContext())),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPlayerBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        // manually apply additional padding for edge-to-edge compatibility
        activity?.getSystemInsets()?.let { systemBars ->
            with(binding.root) {
                setPadding(
                    paddingLeft,
                    paddingTop + systemBars.top,
                    paddingRight,
                    paddingBottom
                )
            }
        }


        val playerData = requireArguments().parcelable<PlayerData>(IntentData.playerData)!!
        videoId = playerData.videoId
        isOffline = playerData.isOffline
        playlistId = playerData.playlistId
        channelId = playerData.channelId

        // remember if playback already started once and only restart playback if that's the first run
        val createNewSession = !requireArguments().getBoolean(IntentData.alreadyStarted)
        requireArguments().putBoolean(IntentData.alreadyStarted, true)

        changeOrientationMode()

        playerLayoutOrientation = resources.configuration.orientation

        initializeTransitionLayout()
        initializeOnClickActions()

        if (PlayerHelper.autoFullscreenEnabled && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setFullscreen()
        }

        chaptersViewModel.chaptersLiveData.observe(viewLifecycleOwner) {
            binding.player.setCurrentChapterName()
            playerControlsBinding.exoProgress.setChapters(it)
        }

        viewModel.segments.observe(viewLifecycleOwner) { segments ->
            binding.descriptionLayout.setSegments(segments)
            playerControlsBinding.exoProgress.setSegments(segments)
            playerControlsBinding.sbToggle.isVisible = segments.isNotEmpty()
            segments.firstOrNull { it.category == PlayerHelper.SPONSOR_HIGHLIGHT_CATEGORY }
                ?.let {
                    lifecycleScope.launch(Dispatchers.IO) { initializeHighlight(it) }
                }
        }

        val localDownloadVersion = runBlocking(Dispatchers.IO) {
            DatabaseHolder.Database.downloadDao().findById(videoId)
        }

        if (!isOffline && localDownloadVersion != null && createNewSession) {
            // the dialog must also be visible when in fullscreen, thus we need to use the activity's
            // fragment manager and not the one from [PlayerFragment]
            val fragmentManager = requireActivity().supportFragmentManager

            fragmentManager.setFragmentResultListener(
                PlayOfflineDialog.PLAY_OFFLINE_DIALOG_REQUEST_KEY, viewLifecycleOwner
            ) { _, bundle ->
                isOffline = bundle.getBoolean(IntentData.isPlayingOffline)

                // start a new playback session - the method will read `isOffline` and decide whether
                // to play the downloaded video based on it, so it's enough to set `isOffline` here
                attachToPlayerService(playerData, true)
            }

            val downloadInfo = DownloadHelper.extractDownloadInfoText(
                requireContext(),
                localDownloadVersion
            ).toTypedArray()

            PlayOfflineDialog().apply {
                arguments = bundleOf(
                    IntentData.videoId to videoId,
                    IntentData.videoTitle to localDownloadVersion.download.title,
                    IntentData.downloadInfo to downloadInfo
                )
            }.show(fragmentManager, null)
        } else {
            attachToPlayerService(playerData, createNewSession)
        }

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (commonPlayerViewModel.isFullscreen.value == true) unsetFullscreen()
                else {
                    binding.playerMotionLayout.setTransitionDuration(250)
                    binding.playerMotionLayout.transitionToEnd()
                    baseActivity.minimizePlayerContainerLayout()
                    baseActivity.requestOrientationChange()
                }
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                binding.playerMotionLayout.progress = backEvent.progress
            }

            override fun handleOnBackCancelled() {
                binding.playerMotionLayout.transitionToStart()
            }
        }
        setOnBackPressed(onBackPressedCallback)

        commonPlayerViewModel.isMiniPlayerVisible.observe(viewLifecycleOwner) { isMiniPlayerVisible ->
            // re-add the callback on top of the back pressed dispatcher listeners stack,
            // so that it's the first one to become called while the full player is visible
            if (!isMiniPlayerVisible) {
                onBackPressedCallback.remove()
                setOnBackPressed(onBackPressedCallback)
            }

            // if the player is minimized, the fragment behind the player should handle the event
            onBackPressedCallback.isEnabled = isMiniPlayerVisible != true
        }

        toggleVideoInfoVisibility(false)
    }

    private fun attachToPlayerService(playerData: PlayerData, startNewSession: Boolean) {
        val (serviceClass, args) = if (isOffline) {
            val isNoInternet = activity is NoInternetActivity

            OfflinePlayerService::class.java to bundleOf(
                IntentData.videoId to videoId,
                IntentData.downloadTab to (playerData.downloadTab ?: DownloadTab.VIDEO),
                IntentData.playlistId to playlistId,
                IntentData.sortOptions to playerData.downloadSortingOrder,
                IntentData.shuffle to playerData.shuffle,
                IntentData.noInternet to isNoInternet
            )
        } else {
            OnlinePlayerService::class.java to bundleOf(
                IntentData.playerData to playerData,
                IntentData.audioOnly to false
            )
        }

        BackgroundHelper.startMediaService(
            requireContext(),
            serviceClass,
            if (startNewSession) args else Bundle.EMPTY,
        ) {
            if (_binding == null) {
                playerController.sendCustomCommand(
                    AbstractPlayerService.stopServiceCommand,
                    Bundle.EMPTY
                )
                playerController.release()
                return@startMediaService
            }

            playerController = it
            playerController.addListener(playerListener)
            updatePlayPauseButton()

            if (!startNewSession) {
                // JSON-encode as work-around for https://github.com/androidx/media/issues/564
                val streams: Streams? =
                    playerController.mediaMetadata.extras?.getString(IntentData.streams)
                        ?.let { json ->
                            JsonHelper.json.decodeFromString(json)
                        }

                // reload the streams data and playback, metadata apparently no longer exists
                if (streams == null) {
                    playNextVideo(videoId)
                    return@startMediaService
                }

                this.streams = streams
                updatePlayerView()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeTransitionLayout() {
        baseActivity.setPlayerContainerProgress(0f)

        var transitionStartId = 0
        var transitionEndId = 0

        binding.playerMotionLayout.addTransitionListener(object : TransitionAdapter() {
            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
                if (_binding == null) return

                baseActivity.setPlayerContainerProgress(progress.absoluteValue)
                disableController()
                commonPlayerViewModel.setSheetExpand(false)
                transitionEndId = endId
                transitionStartId = startId
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (_binding == null) return

                if (currentId == transitionStartId) {
                    commonPlayerViewModel.isMiniPlayerVisible.value = false
                    // re-enable captions
                    binding.player.updateCurrentSubtitle(viewModel.currentCaptionId)
                    binding.player.useController = true
                    commonPlayerViewModel.setSheetExpand(true)
                    baseActivity.setPlayerContainerProgress(0f)
                    changeOrientationMode()

                    // clear search bar focus to avoid keyboard popups
                    baseActivity.clearSearchViewFocus()
                } else if (currentId == transitionEndId) {
                    commonPlayerViewModel.isMiniPlayerVisible.value = true
                    // disable captions temporarily
                    binding.player.updateCurrentSubtitle(null)
                    disableController()
                    commonPlayerViewModel.setSheetExpand(null)
                    playerBackgroundBinding.sbSkipBtn.isGone = true

                    baseActivity.setPlayerContainerProgress(1f)
                    baseActivity.requestOrientationChange()
                }

                updateMaxSheetHeight()
            }
        })

        binding.playerMotionLayout
            .addSwipeDownListener {
                if (commonPlayerViewModel.isMiniPlayerVisible.value == true) {
                    closeMiniPlayer()
                }
            }

        binding.playerMotionLayout.progress = 1F
        binding.playerMotionLayout.transitionToStart()

        val activity = requireActivity()
        if (PlayerHelper.pipEnabled) {
            PictureInPictureCompat.setPictureInPictureParams(activity, pipParams)
        }
    }

    private fun closeMiniPlayer() {
        binding
            .playerMotionLayout
            .animateDown(
                duration = 300L,
                dy = 500F,
                onEnd = ::killPlayerFragment
            )
    }

    // actions that don't depend on video information
    private fun initializeOnClickActions() {
        binding.closeImageView.setOnClickListener {
            killPlayerFragment()
        }
        playerControlsBinding.closeImageButton.setOnClickListener {
            killPlayerFragment()
        }

        binding.playImageView.setOnClickListener {
            if (::playerController.isInitialized) playerController.togglePlayPauseState()
        }

        activity?.supportFragmentManager
            ?.setFragmentResultListener(
                CommentsSheet.HANDLE_LINK_REQUEST_KEY,
                viewLifecycleOwner
            ) { _, bundle ->
                bundle.getString(IntentData.url)?.let { handleLink(it) }
            }

        binding.commentsToggle.setOnClickListener {
            if (!this::streams.isInitialized) return@setOnClickListener
            // set the max height to not cover the currently playing video
            updateMaxSheetHeight()
            commentsViewModel.videoIdLiveData.updateIfChanged(videoId)
            CommentsSheet()
                .apply { arguments = bundleOf(IntentData.channelAvatar to streams.uploaderAvatar) }
                .show(childFragmentManager)
        }

        // share button
        binding.relPlayerShare.setOnClickListener {
            if (!this::streams.isInitialized) return@setOnClickListener
            val bundle = bundleOf(
                IntentData.id to videoId,
                IntentData.shareObjectType to ShareObjectType.VIDEO,
                IntentData.shareData to ShareData(
                    currentVideo = streams.title,
                    currentPosition = playerController.currentPosition / 1000
                )
            )
            val newShareDialog = ShareDialog()
            newShareDialog.arguments = bundle
            newShareDialog.show(childFragmentManager, ShareDialog::class.java.name)
        }

        binding.relPlayerBackground.setOnClickListener {
            // start the background mode
            switchToAudioMode()
        }

        binding.relPlayerPip.isVisible =
            PictureInPictureCompat.isPictureInPictureAvailable(requireContext())

        binding.relPlayerPip.setOnClickListener {
            PictureInPictureCompat.enterPictureInPictureMode(requireActivity(), pipParams)
            isEnteringPiPMode = true
        }

        binding.relatedRecView.layoutManager = LinearLayoutManager(
            context,
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                LinearLayoutManager.HORIZONTAL
            } else {
                LinearLayoutManager.VERTICAL
            },
            false
        )

        binding.relPlayerSave.setOnClickListener {
            if (!::streams.isInitialized) return@setOnClickListener

            AddToPlaylistDialog().apply {
                arguments = bundleOf(IntentData.videoInfo to streams.toStreamItem(videoId))
            }.show(childFragmentManager, AddToPlaylistDialog::class.java.name)
        }

        playerControlsBinding.skipPrev.setOnClickListener {
            PlayingQueue.getPrev()?.let { prev -> playNextVideo(prev) }
        }

        playerControlsBinding.skipNext.setOnClickListener {
            PlayingQueue.getNext()?.let { next -> playNextVideo(next) }
        }

        binding.relPlayerDownload.setOnClickListener {
            if (!this::streams.isInitialized) return@setOnClickListener

            DownloadHelper.startDownloadDialog(requireContext(), childFragmentManager, videoId)
        }

        binding.relPlayerScreenshot.setOnClickListener {
            if (!this::streams.isInitialized) return@setOnClickListener
            val surfaceView =
                binding.player.videoSurfaceView as? SurfaceView ?: return@setOnClickListener

            val bmp = Bitmap.createBitmap(
                surfaceView.width,
                surfaceView.height,
                Bitmap.Config.ARGB_8888
            )

            PixelCopy.request(surfaceView, bmp, { _ ->
                screenshotBitmap = bmp
                val currentPosition =
                    playerController.currentPosition.toFloat() / 1000
                openScreenshotFile.launch("${streams.title}-${currentPosition}.png")
            }, handler)
        }

        binding.playerChannel.setOnClickListener {
            if (!this::streams.isInitialized) return@setOnClickListener

            NavigationHelper.navigateChannel(requireContext(), streams.uploaderUrl)
        }

        binding.descriptionLayout.handleLink = this::handleLink
    }

    private fun updateMaxSheetHeight() {
        val systemBars = baseActivity.getSystemInsets() ?: return
        val maxHeight = binding.root.height - (binding.player.height + systemBars.top)
        commonPlayerViewModel.maxSheetHeightPx = maxHeight
        chaptersViewModel.maxSheetHeightPx = maxHeight
    }

    fun switchToAudioMode() {
        playerController.sendCustomCommand(
            AbstractPlayerService.runPlayerActionCommand,
            bundleOf(PlayerCommand.TOGGLE_AUDIO_ONLY_MODE.name to true)
        )
        // disable autoplay countdown while the audio player is running
        // otherwise playback of the next video wouldn't start automatically because
        // it awaits the start of the autoplay countdown
        playerController.sendCustomCommand(
            AbstractPlayerService.runPlayerActionCommand, bundleOf(
                PlayerCommand.SET_AUTOPLAY_COUNTDOWN_ENABLED.name to false
            )
        )

        binding.player.player = null

        playerController.release()
        killPlayerFragment()

        NavigationHelper.openAudioPlayerFragment(requireContext(), offlinePlayer = isOffline)
    }

    private fun updateFullscreenOrientation() {
        if (PlayerHelper.autoFullscreenEnabled || !this::streams.isInitialized) return

        val height = streams.videoStreams.firstOrNull()?.height
            ?: playerController.videoSize.height
        val width =
            streams.videoStreams.firstOrNull()?.width ?: playerController.videoSize.width

        baseActivity.requestedOrientation = PlayerHelper.getOrientation(width, height)
    }

    private fun setFullscreen() {
        // set status bar icon color to white
        windowInsetsControllerCompat.isAppearanceLightStatusBars = false

        commonPlayerViewModel.isFullscreen.value = true
        updateFullscreenOrientation()

        commonPlayerViewModel.setSheetExpand(null)

        openOrCloseFullscreenDialog(true)

        binding.player.updateMarginsByFullscreenMode()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    fun unsetFullscreen() {
        if (activity == null || _binding == null) return

        commonPlayerViewModel.isFullscreen.value = false

        if (!PlayerHelper.autoFullscreenEnabled) {
            baseActivity.requestedOrientation = baseActivity.screenOrientationPref
        }

        openOrCloseFullscreenDialog(false)

        binding.player.updateMarginsByFullscreenMode()

        // set status bar icon color back to theme color after fullscreen dialog closed!
        windowInsetsControllerCompat.isAppearanceLightStatusBars =
            !ThemeHelper.isDarkMode(requireContext())
    }

    /**
     * Enter/exit fullscreen or toggle it depending on the current state
     */
    override fun toggleFullscreen() {
        binding.player.hideController()

        val isFullscreen = commonPlayerViewModel.isFullscreen.value == true
        if (!isFullscreen) {
            // go to fullscreen mode
            setFullscreen()
        } else {
            // exit fullscreen mode
            unsetFullscreen()
        }
    }

    private fun openOrCloseFullscreenDialog(open: Boolean) {
        val playerView = binding.player
        (playerView.parent as ViewGroup).removeView(playerView)

        if (open) {
            fullscreenDialog.addContentView(
                binding.player,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            )
            fullscreenDialog.show()
            playerView.currentWindow = fullscreenDialog.window
        } else {
            binding.playerMotionLayout.addView(playerView)
            playerView.currentWindow = null
            fullscreenDialog.dismiss()
        }

        WindowHelper.toggleFullscreen(fullscreenDialog.window!!, open)
    }

    override fun onPause() {
        // check whether the screen is on
        val isInteractive = requireContext().getSystemService<PowerManager>()!!.isInteractive

        // disable video stream since it's not needed when screen off or when PiP is not
        // enabled, except when the user is intentionally entering PiP mode via the dedicated button
        if ((!isInteractive || !PlayerHelper.pipEnabled) && !isEnteringPiPMode) {
            // disable the autoplay countdown while the screen is off or when PiP is not enabled
            setAutoPlayCountdownEnabled(false)

            // disable loading the video track while screen is off or when PiP is not enabled
            setVideoTrackTypeDisabled(true)
        }

        val shouldPausePlayer =
            (isInteractive && PlayerHelper.pauseOnQuit) ||
                    (!isInteractive && PlayerHelper.pausePlayerOnScreenOffEnabled)

        // pause player if screen off or app is put the background, except when
        // the user is intentionally entering PiP mode via the dedicated button
        if (shouldPausePlayer && !isEnteringPiPMode) {
            playerController.pause()
        }

        isEnteringPiPMode = false

        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        if (closedVideo) {
            closedVideo = false
        }

        // re-enable the autoplay countdown
        setAutoPlayCountdownEnabled(PlayerHelper.autoPlayCountdown)

        // re-enable and load video stream
        setVideoTrackTypeDisabled(false)
    }

    private fun setAutoPlayCountdownEnabled(enabled: Boolean) {
        if (!::playerController.isInitialized) return

        this.autoPlayCountdownEnabled = enabled

        playerController.sendCustomCommand(
            AbstractPlayerService.runPlayerActionCommand, bundleOf(
                PlayerCommand.SET_AUTOPLAY_COUNTDOWN_ENABLED.name to enabled
            )
        )
    }

    private fun setVideoTrackTypeDisabled(disabled: Boolean) {
        if (!::playerController.isInitialized) return

        playerController.sendCustomCommand(
            AbstractPlayerService.runPlayerActionCommand, bundleOf(
                PlayerCommand.SET_VIDEO_TRACK_TYPE_DISABLED.name to disabled
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        handler.removeCallbacksAndMessages(null)

        if (::playerController.isInitialized && playerController.isConnected) {
            playerController.removeListener(playerListener)
            playerController.pause()

            playerController.sendCustomCommand(
                AbstractPlayerService.stopServiceCommand,
                Bundle.EMPTY
            )
            playerController.release()
        }

        if (PlayerHelper.pipEnabled) {
            // disable the auto PiP mode for SDK >= 32
            PictureInPictureCompat
                .setPictureInPictureParams(requireActivity(), pipParams)
        }

        runCatching {
            if (fullscreenDialog.isShowing) fullscreenDialog.dismiss()
        }

        runCatching {
            // unregister the receiver for player actions
            context?.unregisterReceiver(playerActionReceiver)
        }

        // restore the orientation that's used by the main activity
        baseActivity.requestOrientationChange()

        _binding = null
    }

    /**
     * Manually kill the player fragment - call instead of using onDestroy directly
     */
    private fun killPlayerFragment() {
        binding.playerMotionLayout.transitionToEnd()

        commonPlayerViewModel.isMiniPlayerVisible.value = false

        if (commonPlayerViewModel.isFullscreen.value == true) {
            // wait for the mini player transition to finish
            // that guarantees that the navigation bar is shown properly
            // before we kill the player fragment
            binding.playerMotionLayout.addTransitionListener(object : TransitionAdapter() {
                override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                    super.onTransitionCompleted(motionLayout, currentId)

                    baseActivity.supportFragmentManager.commit {
                        remove(this@PlayerFragment)
                    }
                }
            })

            unsetFullscreen()
        } else {
            baseActivity.supportFragmentManager.commit {
                remove(this@PlayerFragment)
            }
        }
    }

    private fun checkForSegments() {
        if (!playerController.isPlaying || !PlayerHelper.sponsorBlockEnabled) return

        handler.postDelayed(this::checkForSegments, 100)
        if (viewModel.segments.value.isNullOrEmpty()) return

        val segmentData = playerController.getCurrentSegment(
            viewModel.segments.value.orEmpty(),
            viewModel.sponsorBlockConfig
        )

        if (segmentData != null && commonPlayerViewModel.isMiniPlayerVisible.value != true) {
            val (segment, sbSkipOption) = segmentData

            val autoSkipTemporarilyDisabled =
                !binding.player.sponsorBlockAutoSkip && sbSkipOption != SbSkipOptions.OFF

            if (sbSkipOption in arrayOf(
                    SbSkipOptions.AUTOMATIC_ONCE,
                    SbSkipOptions.MANUAL
                ) || autoSkipTemporarilyDisabled
            ) {
                playerBackgroundBinding.sbSkipBtn.isVisible = true
                playerBackgroundBinding.sbSkipBtn.setOnClickListener {
                    playerController.seekTo((segment.segmentStartAndEnd.second * 1000f).toLong())
                    segment.skipped = true
                }
            }
        } else {
            playerBackgroundBinding.sbSkipBtn.isGone = true
        }
    }

    private fun setPlayerDefaults() {
        // reset the player view
        playerControlsBinding.exoProgress.clearSegments()
        playerControlsBinding.sbToggle.isGone = true

        // reset the comments to become reloaded later
        commentsViewModel.reset()

        // hide the button to skip SponsorBlock segments manually
        playerBackgroundBinding.sbSkipBtn.isGone = true

        // use the video's default audio track when starting playback
        playerController.sendCustomCommand(
            AbstractPlayerService.runPlayerActionCommand, bundleOf(
                PlayerCommand.SET_AUDIO_ROLE_FLAGS.name to C.ROLE_FLAG_MAIN
            )
        )

        setAutoPlayCountdownEnabled(PlayerHelper.autoPlayCountdown)

        // set the default subtitle if available
        binding.player.updateCurrentSubtitle(viewModel.currentCaptionId)

        if (streams.category == Streams.CATEGORY_MUSIC) {
            playerController.setPlaybackSpeed(1f)
        }
    }

    /**
     * Manually skip to another video.
     *
     * You many only call this if the video is of the same type as the type of the currently running
     * video, i.e. either both are online or both are offline.
     */
    fun playNextVideo(nextId: String) {
        playerController.sendCustomCommand(
            AbstractPlayerService.runPlayerActionCommand,
            bundleOf(PlayerCommand.PLAY_VIDEO_BY_ID.name to nextId)
        )
    }

    private fun dismissCommentsSheet() {
        // close comment bottom sheet if opened for next video
        childFragmentManager.fragments
            .filterIsInstance<CommentsSheet>()
            .firstOrNull()
            ?.dismiss()
    }

    private fun toggleVideoInfoVisibility(show: Boolean) {
        binding.descriptionLayout.collapseDescription()
        binding.descriptionLayout.isInvisible = !show
        binding.relatedRecView.isInvisible = !show
        binding.playerChannel.isInvisible = !show
        playerBackgroundBinding.videoTransitionProgress.isVisible = !show
    }

    @SuppressLint("SetTextI18n")
    private fun updatePlayerView() {
        dismissCommentsSheet()

        setPlayerDefaults()

        binding.player.apply {
            useController = false
            player = playerController
        }

        // initialize the player view actions
        binding.player.initialize(
            chaptersViewModel,
            commonPlayerViewModel,
            viewModel,
            viewLifecycleOwner,
            this
        )

        if (binding.playerMotionLayout.progress != 1.0f) {
            // show controllers when not in picture in picture mode
            val inPipMode = PlayerHelper.pipEnabled &&
                    PictureInPictureCompat.isInPictureInPictureMode(requireActivity())
            if (!inPipMode) {
                binding.player.useController = true
            }
        }

        viewModel.isOrientationChangeInProgress = false

        binding.descriptionLayout.setStreams(streams)

        toggleVideoInfoVisibility(true)

        binding.apply {
            ImageHelper.loadImage(streams.uploaderAvatar, binding.playerChannelImage, true)
            playerChannelName.text = streams.uploader
            titleTextView.text = streams.title
            playerChannelSubCount.text = context?.getString(
                R.string.subscribers,
                streams.uploaderSubscriberCount.formatShort()
            )
            player.isLive = streams.isLive
            relPlayerDownload.isVisible = !streams.isLive && !isOffline
        }
        playerControlsBinding.exoTitle.text = streams.title

        // init the chapters recyclerview
        chaptersViewModel.chaptersLiveData.postValue(streams.chapters)

        lifecycleScope.launch {
            showRelatedStreams()
        }

        // update the subscribed state
        if (streams.uploaderUrl != null) {
            binding.playerSubscribe.setupSubscriptionButton(
                streams.uploaderUrl!!.toID(),
                streams.uploader,
                streams.uploaderAvatar,
                streams.uploaderVerified
            )
        } else {
            binding.playerSubscribe.isGone = true
        }

        // seekbar preview setup
        playerControlsBinding.seekbarPreview.isGone = true
        seekBarPreviewListener?.let { playerControlsBinding.exoProgress.removeSeekBarListener(it) }

        lifecycleScope.launch {
            val timeFrameReceiver = getTimeFrameReceiver() ?: return@launch
            val listener = SeekbarPreviewListener(
                timeFrameReceiver,
                playerControlsBinding,
                streams.duration * 1000
            )

            seekBarPreviewListener = listener
            playerControlsBinding.exoProgress.addSeekBarListener(listener)
        }
    }

    private suspend fun showRelatedStreams() {
        if (!PlayerHelper.relatedStreamsEnabled) return

        val relatedStreams = if (isOffline) {
            withContext(Dispatchers.IO) {
                DatabaseHolder.Database.downloadDao().getAll()
                    .filter { it.download.videoId != videoId }
                    .map { it.download.toStreamItem() }
            }
        } else {
            streams.relatedStreams.filter { !it.title.isNullOrBlank() }
        }

        val relatedLayoutManager = binding.relatedRecView.layoutManager as LinearLayoutManager
        binding.relatedRecView.adapter = VideoCardsAdapter(
            columnWidthDp = if (relatedLayoutManager.orientation == LinearLayoutManager.HORIZONTAL) 250f else null
        ).also { adapter ->
            adapter.submitList(relatedStreams)
        }
    }

    private fun showAutoPlayCountdown() {
        if (!PlayingQueue.hasNext()) return

        disableController()
        playerBackgroundBinding.autoplayCountdown.setHideSelfListener {
            // could fail if the video already got closed before
            runCatching {
                playerBackgroundBinding.autoplayCountdown.isGone = true
                binding.player.useController = true
            }
        }
        playerBackgroundBinding.autoplayCountdown.startCountdown {
            PlayingQueue.getNext()?.let { playNextVideo(it) }
        }
    }

    /**
     * Handle a link clicked in the description
     */
    private fun handleLink(link: String) {
        // get video id if the link is a valid youtube video link
        val uri = link.toUri()
        val videoId = TextUtils.getVideoIdFromUri(uri)

        if (videoId.isNullOrEmpty()) {
            // not a YouTube video link, thus handle normally
            val intent = Intent(Intent.ACTION_VIEW, uri)

            // start PiP mode if enabled
            onUserLeaveHint()
            startActivity(intent)

            return
        }

        // check if the video is the current video and has a valid time
        if (videoId == this.videoId) {
            // try finding the time stamp of the url and seek to it if found
            uri.getQueryParameter("t")?.toTimeInSeconds()?.let {
                playerController.seekTo(it * 1000)
            }
        } else {
            // YouTube video link without time or not the current video, thus load in player
            playNextVideo(videoId)
        }
    }

    private fun updatePlayPauseButton() {
        val playPauseAction = PlayerHelper.getPlayPauseActionIcon(playerController)
        binding.playImageView.setImageResource(playPauseAction)
    }

    private suspend fun getTimeFrameReceiver(): TimeFrameReceiver? = withContext(Dispatchers.IO) {
        return@withContext if (isOffline) {
            val downloadItems =
                DatabaseHolder.Database.downloadDao().getDownloadById(videoId)?.downloadItems
            downloadItems?.firstOrNull { it.path.exists() && it.type == FileType.VIDEO }?.path?.let {
                OfflineTimeFrameReceiver(requireContext(), it)
            }
        } else {
            OnlineTimeFrameReceiver(requireContext(), streams.previewFrames)
        }
    }

    private suspend fun initializeHighlight(highlight: Segment) {
        val frameReceiver = getTimeFrameReceiver() ?: return
        val highlightStart = highlight.segmentStartAndEnd.first.toLong()
        val frame = withContext(Dispatchers.IO) {
            frameReceiver.getFrameAtTime(highlightStart * 1000)
        }
        val highlightChapter = ChapterSegment(
            title = getString(R.string.chapters_videoHighlight),
            start = highlightStart,
            highlightDrawable = frame?.toDrawable(requireContext().resources)
        )
        chaptersViewModel.chaptersLiveData.postValue(
            chaptersViewModel.chapters.plus(highlightChapter).sortedBy { it.start }
        )
    }

    /**
     * Use the sensor mode if auto fullscreen is enabled
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private fun changeOrientationMode() {
        if (PlayerHelper.autoFullscreenEnabled) {
            // enable auto rotation
            baseActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        } else {
            // go to portrait mode
            baseActivity.requestedOrientation =
                (requireActivity() as BaseActivity).screenOrientationPref
        }
    }


    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            // hide and disable exoPlayer controls
            disableController()

            binding.player.updateCurrentSubtitle(null)

            openOrCloseFullscreenDialog(true)
            pipActivity = activity
        } else {
            binding.player.useController = true

            // close button got clicked in PiP mode
            // pause the video and keep the app alive
            if (lifecycle.currentState == Lifecycle.State.CREATED) {
                playerController.pause()
                closedVideo = true
            }

            binding.player.updateCurrentSubtitle(viewModel.currentCaptionId)

            // unset fullscreen if it's not been enabled before the start of PiP
            if (commonPlayerViewModel.isFullscreen.value != true) {
                openOrCloseFullscreenDialog(false)
            }
        }
    }

    fun onUserLeaveHint() {
        if (shouldStartPiP()) {
            PictureInPictureCompat.enterPictureInPictureMode(requireActivity(), pipParams)
        } else if (PlayerHelper.pauseOnQuit) {
            playerController.pause()
        }
    }

    private val pipParams: PictureInPictureParamsCompat
        get() = run {
            val isPlaying = ::playerController.isInitialized && playerController.isPlaying

            PictureInPictureParamsCompat.Builder()
                .setActions(
                    PlayerHelper.getPiPModeActions(
                        requireActivity(),
                        isPlaying
                    )
                )
                .setAutoEnterEnabled(PlayerHelper.pipEnabled && isPlaying)
                .apply {
                    if (isPlaying) {
                        setAspectRatio(playerController.videoSize)
                    }
                }
                .build()
        }

    /**
     * Detect whether PiP is supported and enabled
     */
    private fun shouldUsePip(): Boolean {
        return PictureInPictureCompat.isPictureInPictureAvailable(requireContext()) && PlayerHelper.pipEnabled
    }

    private fun shouldStartPiP(): Boolean {
        return shouldUsePip() && ::playerController.isInitialized && playerController.isPlaying
    }

    /**
     * Check if the activity needs to be recreated due to an orientation change
     * If true, the activity will be automatically restarted
     */
    private fun restartActivityIfNeeded() {
        if (baseActivity.screenOrientationPref in lockedOrientations || viewModel.isOrientationChangeInProgress) return

        val orientation = resources.configuration.orientation
        if (commonPlayerViewModel.isFullscreen.value != true && orientation != playerLayoutOrientation) {
            // remember the current position before recreating the activity
            playerLayoutOrientation = orientation

            viewModel.isOrientationChangeInProgress = true

            // detach player view from player to stop surface rendering
            binding.player.player = null

            if (::playerController.isInitialized) playerController.release()

            activity?.recreate()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (_binding == null ||
            // If in PiP mode, orientation is given as landscape.
            PictureInPictureCompat.isInPictureInPictureMode(requireActivity())
        ) {
            return
        }

        if (PlayerHelper.autoFullscreenEnabled) {
            when (newConfig.orientation) {
                // go to fullscreen mode
                Configuration.ORIENTATION_LANDSCAPE -> setFullscreen()
                // exit fullscreen if not landscape
                else -> unsetFullscreen()
            }
        }

        restartActivityIfNeeded()
    }

    private fun disableController() {
        binding.player.useController = false
        binding.player.hideController()
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return _binding?.player?.onKeyUp(keyCode, event) ?: false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getVideoId(): String {
        return videoId
    }

    override fun isVideoShort(): Boolean {
        return isShort
    }
}
