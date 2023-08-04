package com.github.libretube.ui.activities

import android.content.pm.ActivityInfo
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.viewModels
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.FileDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.github.libretube.compat.PictureInPictureCompat
import com.github.libretube.compat.PictureInPictureParamsCompat
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.ActivityOfflinePlayerBinding
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.enums.FileType
import com.github.libretube.extensions.toAndroidUri
import com.github.libretube.extensions.updateParameters
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PlayerHelper.loadPlaybackParams
import com.github.libretube.helpers.WindowHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.interfaces.TimeFrameReceiver
import com.github.libretube.ui.listeners.SeekbarPreviewListener
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.util.OfflineTimeFrameReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class OfflinePlayerActivity : BaseActivity() {
    private lateinit var binding: ActivityOfflinePlayerBinding
    private lateinit var videoId: String
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var trackSelector: DefaultTrackSelector
    private var timeFrameReceiver: TimeFrameReceiver? = null

    private lateinit var playerBinding: ExoStyledPlayerControlViewBinding
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowHelper.toggleFullscreen(this, true)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE

        super.onCreate(savedInstanceState)

        videoId = intent?.getStringExtra(IntentData.videoId)!!

        binding = ActivityOfflinePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializePlayer()
        playVideo()

        requestedOrientation = PlayerHelper.getOrientation(
            player.videoSize.width,
            player.videoSize.height
        )
    }

    private fun initializePlayer() {
        trackSelector = DefaultTrackSelector(this)

        player = ExoPlayer.Builder(this)
            .setUsePlatformDiagnostics(false)
            .setHandleAudioBecomingNoisy(true)
            .setTrackSelector(trackSelector)
            .setLoadControl(PlayerHelper.getLoadControl())
            .setAudioAttributes(PlayerHelper.getAudioAttributes(), true)
            .setUsePlatformDiagnostics(false)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        super.onEvents(player, events)
                        // update the displayed duration on changes
                        playerBinding.duration.text = DateUtils.formatElapsedTime(
                            player.duration / 1000
                        )
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        // setup seekbar preview
                        if (playbackState == Player.STATE_READY) {
                            binding.player.binding.exoProgress.addListener(
                                SeekbarPreviewListener(
                                    timeFrameReceiver ?: return,
                                    binding.player.binding,
                                    player.duration
                                )
                            )
                        }
                    }
                })
            }
            .loadPlaybackParams()

        playerView = binding.player
        playerView.setShowSubtitleButton(true)
        playerView.subtitleView?.isVisible = true
        playerView.player = player
        playerBinding = binding.player.binding

        playerBinding.fullscreen.isInvisible = true
        playerBinding.closeImageButton.setOnClickListener {
            finish()
        }

        binding.player.initialize(
            binding.doubleTapOverlay.binding,
            binding.playerGestureControlsView.binding
        )
    }

    private fun playVideo() {
        lifecycleScope.launch {
            val downloadInfo = withContext(Dispatchers.IO) {
                Database.downloadDao().findById(videoId)
            }
            val downloadFiles = downloadInfo.downloadItems
            playerBinding.exoTitle.text = downloadInfo.download.title
            playerBinding.exoTitle.isVisible = true

            val video = downloadFiles.firstOrNull { it.type == FileType.VIDEO }
            val audio = downloadFiles.firstOrNull { it.type == FileType.AUDIO }
            val subtitle = downloadFiles.firstOrNull { it.type == FileType.SUBTITLE }

            val videoUri = video?.path?.toAndroidUri()
            val audioUri = audio?.path?.toAndroidUri()
            val subtitleUri = subtitle?.path?.toAndroidUri()

            setMediaSource(videoUri, audioUri, subtitleUri)

            trackSelector.updateParameters {
                setPreferredTextRoleFlags(C.ROLE_FLAG_CAPTION)
                setPreferredTextLanguage("en")
            }

            timeFrameReceiver = video?.path?.let {
                OfflineTimeFrameReceiver(this@OfflinePlayerActivity, it)
            }

            player.playWhenReady = PlayerHelper.playAutomatically
            player.prepare()
        }
    }

    private fun setMediaSource(videoUri: Uri?, audioUri: Uri?, subtitleUri: Uri?) {
        val subtitle = subtitleUri?.let {
            SubtitleConfiguration.Builder(it)
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .build()
        }

        when {
            videoUri != null && audioUri != null -> {
                val videoItem = MediaItem.Builder()
                    .setUri(videoUri)
                    .apply {
                        if (subtitle != null) setSubtitleConfigurations(listOf(subtitle))
                    }
                    .build()

                val videoSource = ProgressiveMediaSource.Factory(FileDataSource.Factory())
                    .createMediaSource(videoItem)

                val audioSource = ProgressiveMediaSource.Factory(FileDataSource.Factory())
                    .createMediaSource(MediaItem.fromUri(audioUri))

                val mediaSource = MergingMediaSource(audioSource, videoSource)

                player.setMediaSource(mediaSource)
            }

            videoUri != null -> player.setMediaItem(
                MediaItem.Builder()
                    .setUri(videoUri)
                    .apply {
                        if (subtitle != null) setSubtitleConfigurations(listOf(subtitle))
                    }
                    .build()
            )

            audioUri != null -> player.setMediaItem(
                MediaItem.Builder()
                    .setUri(audioUri)
                    .apply {
                        if (subtitle != null) setSubtitleConfigurations(listOf(subtitle))
                    }
                    .build()
            )
        }
    }

    override fun onResume() {
        playerViewModel.isFullscreen.value = true
        super.onResume()
    }

    override fun onPause() {
        playerViewModel.isFullscreen.value = false
        super.onPause()
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        if (PlayerHelper.pipEnabled && player.playbackState != PlaybackState.STATE_PAUSED) {
            PictureInPictureCompat.enterPictureInPictureMode(
                this,
                PictureInPictureParamsCompat.Builder()
                    .setAspectRatio(player.videoSize)
                    .build()
            )
        }

        super.onUserLeaveHint()
    }
}
