package com.github.libretube.ui.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.text.format.DateUtils
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView
import com.github.libretube.compat.PictureInPictureCompat
import com.github.libretube.compat.PictureInPictureParamsCompat
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.ActivityOfflinePlayerBinding
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.DownloadChapter
import com.github.libretube.db.obj.filterByTab
import com.github.libretube.enums.FileType
import com.github.libretube.enums.PlayerEvent
import com.github.libretube.extensions.serializableExtra
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.WindowHelper
import com.github.libretube.services.VideoOfflinePlayerService
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.fragments.DownloadTab
import com.github.libretube.ui.interfaces.TimeFrameReceiver
import com.github.libretube.ui.listeners.SeekbarPreviewListener
import com.github.libretube.ui.models.ChaptersViewModel
import com.github.libretube.ui.models.CommonPlayerViewModel
import com.github.libretube.util.OfflineTimeFrameReceiver
import com.github.libretube.util.PauseableTimer
import com.github.libretube.util.PlayingQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.path.exists

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class OfflinePlayerActivity : BaseActivity() {
    private lateinit var binding: ActivityOfflinePlayerBinding
    private lateinit var videoId: String

    private lateinit var playerController: MediaController
    private lateinit var playerView: PlayerView
    private var timeFrameReceiver: TimeFrameReceiver? = null

    private lateinit var playerBinding: ExoStyledPlayerControlViewBinding
    private val commonPlayerViewModel: CommonPlayerViewModel by viewModels()
    private val chaptersViewModel: ChaptersViewModel by viewModels()

    private val watchPositionTimer = PauseableTimer(
        onTick = this::saveWatchPosition,
        delayMillis = PlayerHelper.WATCH_POSITION_TIMER_DELAY_MS
    )

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)
            // update the displayed duration on changes
            playerBinding.duration.text = DateUtils.formatElapsedTime(
                player.duration / 1000
            )
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)

            requestedOrientation = PlayerHelper.getOrientation(
                playerController.videoSize.width,
                playerController.videoSize.height
            )

            if (PlayerHelper.pipEnabled) {
                PictureInPictureCompat.setPictureInPictureParams(
                    this@OfflinePlayerActivity,
                    pipParams
                )
            }

            // Start or pause watch position timer
            if (isPlaying) {
                watchPositionTimer.resume()
            } else {
                watchPositionTimer.pause()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            // setup seekbar preview
            if (playbackState == Player.STATE_READY) {
                binding.player.binding.exoProgress.addSeekBarListener(
                    SeekbarPreviewListener(
                        timeFrameReceiver ?: return,
                        binding.player.binding,
                        playerController.duration
                    )
                )
            }

            if (playbackState == Player.STATE_ENDED && PlayerHelper.isAutoPlayEnabled()) {
                playNextVideo(PlayingQueue.getNext() ?: return)
            }
        }
    }

    private val playerActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val event = intent.serializableExtra<PlayerEvent>(PlayerHelper.CONTROL_TYPE) ?: return
            if (PlayerHelper.handlePlayerAction(playerController, event)) return

            when (event) {
                PlayerEvent.Prev -> playNextVideo(PlayingQueue.getPrev() ?: return)
                PlayerEvent.Next -> playNextVideo(PlayingQueue.getNext() ?: return)
                else -> Unit
            }
        }
    }

    private val pipParams
        get() = run {
            val isPlaying = ::playerController.isInitialized && playerController.isPlaying

            PictureInPictureParamsCompat.Builder()
                .setActions(PlayerHelper.getPiPModeActions(this, isPlaying))
                .setAutoEnterEnabled(PlayerHelper.pipEnabled && isPlaying)
                .apply {
                    if (isPlaying) {
                        setAspectRatio(playerController.videoSize)
                    }
                }
                .build()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowHelper.toggleFullscreen(window, true)

        super.onCreate(savedInstanceState)

        videoId = intent?.getStringExtra(IntentData.videoId)!!

        binding = ActivityOfflinePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PlayingQueue.resetToDefaults()
        PlayingQueue.clear()

        PlayingQueue.setOnQueueTapListener { streamItem ->
            playNextVideo(streamItem.url ?: return@setOnQueueTapListener)
        }

        val arguments = bundleOf(
            IntentData.downloadTab to DownloadTab.VIDEO,
            IntentData.videoId to videoId
        )
        BackgroundHelper.startMediaService(this, VideoOfflinePlayerService::class.java, arguments) {
            playerController = it
            playerController.addListener(playerListener)
            initializePlayerView()
        }

        ContextCompat.registerReceiver(
            this,
            playerActionReceiver,
            IntentFilter(PlayerHelper.getIntentActionName(this)),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        if (PlayerHelper.pipEnabled) {
            PictureInPictureCompat.setPictureInPictureParams(this, pipParams)
        }

        lifecycleScope.launch { fillQueue() }
    }

    private fun playNextVideo(videoId: String) {
        saveWatchPosition()
        this.videoId = videoId
        playVideo()
    }

    private fun initializePlayerView() {
        playerView = binding.player
        playerView.setShowSubtitleButton(true)
        playerView.subtitleView?.isVisible = true
        playerView.player = playerController
        playerBinding = binding.player.binding

        playerBinding.fullscreen.isInvisible = true
        playerBinding.closeImageButton.setOnClickListener {
            finish()
        }

        playerBinding.skipPrev.setOnClickListener {
            playNextVideo(PlayingQueue.getPrev() ?: return@setOnClickListener)
        }

        playerBinding.skipNext.setOnClickListener {
            playNextVideo(PlayingQueue.getNext() ?: return@setOnClickListener)
        }

        binding.player.initialize(
            binding.doubleTapOverlay.binding,
            binding.playerGestureControlsView.binding,
            chaptersViewModel
        )
    }

    private fun playVideo() {
        lifecycleScope.launch {
            val (downloadInfo, downloadItems, downloadChapters) = withContext(Dispatchers.IO) {
                Database.downloadDao().findById(videoId)
            }!!

            val chapters = downloadChapters.map(DownloadChapter::toChapterSegment)
            chaptersViewModel.chaptersLiveData.value = chapters
            binding.player.setChapters(chapters)

            val downloadFiles = downloadItems.filter { it.path.exists() }
            playerBinding.exoTitle.text = downloadInfo.title
            playerBinding.exoTitle.isVisible = true

            timeFrameReceiver = downloadFiles.firstOrNull { it.type == FileType.VIDEO }?.path?.let {
                OfflineTimeFrameReceiver(this@OfflinePlayerActivity, it)
            }

            if (PlayerHelper.watchPositionsVideo) {
                PlayerHelper.getStoredWatchPosition(videoId, downloadInfo.duration)?.let {
                    playerController.seekTo(it)
                }
            }
        }
    }

    private suspend fun fillQueue() {
        val downloads = withContext(Dispatchers.IO) {
            Database.downloadDao().getAll()
        }.filterByTab(DownloadTab.VIDEO)

        PlayingQueue.insertRelatedStreams(downloads.map { it.download.toStreamItem() })
    }

    private fun saveWatchPosition() {
        if (!PlayerHelper.watchPositionsVideo) return

        PlayerHelper.saveWatchPosition(playerController, videoId)
    }

    override fun onResume() {
        commonPlayerViewModel.isFullscreen.value = true
        super.onResume()
    }

    override fun onPause() {
        commonPlayerViewModel.isFullscreen.value = false
        super.onPause()

        if (PlayerHelper.pauseOnQuit) {
            playerController.pause()
        }
    }

    override fun onDestroy() {
        saveWatchPosition()

        watchPositionTimer.destroy()

        runCatching {
            playerController.stop()
        }

        runCatching {
            unregisterReceiver(playerActionReceiver)
        }

        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        if (PlayerHelper.pipEnabled && playerController.isPlaying) {
            PictureInPictureCompat.enterPictureInPictureMode(this, pipParams)
        }

        super.onUserLeaveHint()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

        if (isInPictureInPictureMode) {
            playerView.hideController()
            playerView.useController = false
        } else {
            playerView.useController = true
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (binding.player.onKeyBoardAction(keyCode)) {
            return true
        }

        return super.onKeyUp(keyCode, event)
    }
}
