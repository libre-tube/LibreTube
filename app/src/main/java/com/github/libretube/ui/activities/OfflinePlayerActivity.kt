package com.github.libretube.ui.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.FileDataSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
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
import com.github.libretube.extensions.toAndroidUri
import com.github.libretube.extensions.updateParameters
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.WindowHelper
import com.github.libretube.obj.PlayerNotificationData
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.fragments.DownloadTab
import com.github.libretube.ui.interfaces.TimeFrameReceiver
import com.github.libretube.ui.listeners.SeekbarPreviewListener
import com.github.libretube.ui.models.ChaptersViewModel
import com.github.libretube.ui.models.CommonPlayerViewModel
import com.github.libretube.ui.models.OfflinePlayerViewModel
import com.github.libretube.util.NowPlayingNotification
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
    private lateinit var playerView: PlayerView
    private var timeFrameReceiver: TimeFrameReceiver? = null
    private var nowPlayingNotification: NowPlayingNotification? = null

    private lateinit var playerBinding: ExoStyledPlayerControlViewBinding
    private val commonPlayerViewModel: CommonPlayerViewModel by viewModels()
    private val viewModel: OfflinePlayerViewModel by viewModels { OfflinePlayerViewModel.Factory }
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
                        viewModel.player.duration
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
            if (PlayerHelper.handlePlayerAction(viewModel.player, event)) return

            when (event) {
                PlayerEvent.Prev -> playNextVideo(PlayingQueue.getPrev() ?: return)
                PlayerEvent.Next -> playNextVideo(PlayingQueue.getNext() ?: return)
                else -> Unit
            }
        }
    }

    private val pipParams
        get() = PictureInPictureParamsCompat.Builder()
            .setActions(PlayerHelper.getPiPModeActions(this, viewModel.player.isPlaying))
            .setAutoEnterEnabled(PlayerHelper.pipEnabled && viewModel.player.isPlaying)
            .setAspectRatio(viewModel.player.videoSize)
            .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowHelper.toggleFullscreen(window, true)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE

        super.onCreate(savedInstanceState)

        videoId = intent?.getStringExtra(IntentData.videoId)!!

        binding = ActivityOfflinePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PlayingQueue.resetToDefaults()
        PlayingQueue.clear()

        PlayingQueue.setOnQueueTapListener { streamItem ->
            playNextVideo(streamItem.url ?: return@setOnQueueTapListener)
        }

        initializePlayer()
        playVideo()

        requestedOrientation = PlayerHelper.getOrientation(
            viewModel.player.videoSize.width,
            viewModel.player.videoSize.height
        )

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

    private fun initializePlayer() {
        viewModel.player.setWakeMode(C.WAKE_MODE_LOCAL)
        viewModel.player.addListener(playerListener)

        playerView = binding.player
        playerView.setShowSubtitleButton(true)
        playerView.subtitleView?.isVisible = true
        playerView.player = viewModel.player
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

        nowPlayingNotification = NowPlayingNotification(
            this,
            viewModel.player,
            offlinePlayer = true,
            intentActivity = OfflinePlayerActivity::class.java
        )
    }

    private fun playVideo() {
        lifecycleScope.launch {
            val (downloadInfo, downloadItems, downloadChapters) = withContext(Dispatchers.IO) {
                Database.downloadDao().findById(videoId)
            }
            PlayingQueue.updateCurrent(downloadInfo.toStreamItem())

            val chapters = downloadChapters.map(DownloadChapter::toChapterSegment)
            chaptersViewModel.chaptersLiveData.value = chapters
            binding.player.setChapters(chapters)

            val downloadFiles = downloadItems.filter { it.path.exists() }
            playerBinding.exoTitle.text = downloadInfo.title
            playerBinding.exoTitle.isVisible = true

            val video = downloadFiles.firstOrNull { it.type == FileType.VIDEO }
            val audio = downloadFiles.firstOrNull { it.type == FileType.AUDIO }
            val subtitle = downloadFiles.firstOrNull { it.type == FileType.SUBTITLE }

            val videoUri = video?.path?.toAndroidUri()
            val audioUri = audio?.path?.toAndroidUri()
            val subtitleUri = subtitle?.path?.toAndroidUri()

            setMediaSource(videoUri, audioUri, subtitleUri)

            viewModel.trackSelector.updateParameters {
                setPreferredTextRoleFlags(C.ROLE_FLAG_CAPTION)
                setPreferredTextLanguage("en")
            }

            timeFrameReceiver = video?.path?.let {
                OfflineTimeFrameReceiver(this@OfflinePlayerActivity, it)
            }

            viewModel.player.playWhenReady = PlayerHelper.playAutomatically
            viewModel.player.prepare()

            if (PlayerHelper.watchPositionsVideo) {
                PlayerHelper.getStoredWatchPosition(videoId, downloadInfo.duration)?.let {
                    viewModel.player.seekTo(it)
                }
            }

            val data = PlayerNotificationData(
                downloadInfo.title,
                downloadInfo.uploader,
                downloadInfo.thumbnailPath.toString()
            )
            nowPlayingNotification?.updatePlayerNotification(videoId, data)
        }
    }

    private fun setMediaSource(videoUri: Uri?, audioUri: Uri?, subtitleUri: Uri?) {
        val subtitle = subtitleUri?.let {
            SubtitleConfiguration.Builder(it)
                .setMimeType(MimeTypes.APPLICATION_TTML)
                .setLanguage("en")
                .build()
        }

        when {
            videoUri != null && audioUri != null -> {
                val videoItem = MediaItem.Builder()
                    .setUri(videoUri)
                    .setSubtitleConfigurations(listOfNotNull(subtitle))
                    .build()

                val videoSource = ProgressiveMediaSource.Factory(FileDataSource.Factory())
                    .createMediaSource(videoItem)

                val audioSource = ProgressiveMediaSource.Factory(FileDataSource.Factory())
                    .createMediaSource(MediaItem.fromUri(audioUri))

                var mediaSource = MergingMediaSource(audioSource, videoSource)
                if (subtitle != null) {
                    val subtitleSource = SingleSampleMediaSource.Factory(FileDataSource.Factory())
                        .createMediaSource(subtitle, C.TIME_UNSET)

                    mediaSource = MergingMediaSource(mediaSource, subtitleSource)
                }

                viewModel.player.setMediaSource(mediaSource)
            }

            videoUri != null -> viewModel.player.setMediaItem(
                MediaItem.Builder()
                    .setUri(videoUri)
                    .setSubtitleConfigurations(listOfNotNull(subtitle))
                    .build()
            )

            audioUri != null -> viewModel.player.setMediaItem(
                MediaItem.Builder()
                    .setUri(audioUri)
                    .setSubtitleConfigurations(listOfNotNull(subtitle))
                    .build()
            )
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

        PlayerHelper.saveWatchPosition(viewModel.player, videoId)
    }

    override fun onResume() {
        commonPlayerViewModel.isFullscreen.value = true
        super.onResume()
    }

    override fun onPause() {
        commonPlayerViewModel.isFullscreen.value = false
        super.onPause()

        if (PlayerHelper.pauseOnQuit) {
            viewModel.player.pause()
        }
    }

    override fun onDestroy() {
        saveWatchPosition()

        nowPlayingNotification?.destroySelf()
        nowPlayingNotification = null
        watchPositionTimer.destroy()

        runCatching {
            viewModel.player.stop()
        }

        runCatching {
            unregisterReceiver(playerActionReceiver)
        }

        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        if (PlayerHelper.pipEnabled && viewModel.player.isPlaying) {
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
