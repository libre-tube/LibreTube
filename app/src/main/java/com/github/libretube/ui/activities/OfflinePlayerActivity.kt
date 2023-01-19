package com.github.libretube.ui.activities

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.activity.viewModels
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.ActivityOfflinePlayerBinding
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.enums.FileType
import com.github.libretube.extensions.awaitQuery
import com.github.libretube.extensions.updateParameters
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.extensions.setAspectRatio
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.util.PlayerHelper
import com.github.libretube.util.WindowHelper
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.util.MimeTypes
import java.io.File

class OfflinePlayerActivity : BaseActivity() {
    private lateinit var binding: ActivityOfflinePlayerBinding
    private lateinit var videoId: String
    private lateinit var player: ExoPlayer
    private lateinit var playerView: StyledPlayerView
    private lateinit var trackSelector: DefaultTrackSelector

    private lateinit var playerBinding: ExoStyledPlayerControlViewBinding
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowHelper(this).setFullscreen()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE

        super.onCreate(savedInstanceState)

        videoId = intent?.getStringExtra(IntentData.videoId)!!

        binding = ActivityOfflinePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializePlayer()
        playVideo()

        requestedOrientation = PlayerHelper.getOrientation(player.videoSize)
    }

    private fun initializePlayer() {
        trackSelector = DefaultTrackSelector(this)

        player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .setTrackSelector(trackSelector)
            .setLoadControl(PlayerHelper.getLoadControl())
            .setAudioAttributes(PlayerHelper.getAudioAttributes(), true)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        super.onEvents(player, events)
                        // update the displayed duration on changes
                        playerBinding.duration.text = DateUtils.formatElapsedTime(
                            player.duration / 1000
                        )
                    }
                })
            }

        playerView = binding.player
        playerView.setShowSubtitleButton(true)
        playerView.subtitleView?.visibility = View.VISIBLE
        playerView.player = player
        playerBinding = binding.player.binding

        playerBinding.fullscreen.visibility = View.GONE
        playerBinding.closeImageButton.setOnClickListener {
            finish()
        }

        binding.player.initialize(
            null,
            binding.doubleTapOverlay.binding,
            binding.playerGestureControlsView.binding,
            trackSelector
        )
    }

    private fun File.toUri(): Uri? {
        return if (this.exists()) Uri.fromFile(this) else null
    }

    private fun playVideo() {
        val downloadFiles = awaitQuery {
            Database.downloadDao().findById(videoId).downloadItems
        }

        val video = downloadFiles.firstOrNull { it.type == FileType.VIDEO }
        val audio = downloadFiles.firstOrNull { it.type == FileType.AUDIO }
        val subtitle = downloadFiles.firstOrNull { it.type == FileType.SUBTITLE }

        val videoUri = video?.path?.let { File(it).toUri() }
        val audioUri = audio?.path?.let { File(it).toUri() }
        val subtitleUri = subtitle?.path?.let { File(it).toUri() }

        setMediaSource(videoUri, audioUri, subtitleUri)

        trackSelector.updateParameters {
            setPreferredTextRoleFlags(C.ROLE_FLAG_CAPTION)
            setPreferredTextLanguage("en")
        }

        player.prepare()
        player.play()
    }

    private fun setMediaSource(videoUri: Uri?, audioUri: Uri?, subtitleUri: Uri?) {
        val subtitle = subtitleUri?.let {
            SubtitleConfiguration.Builder(it)
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .build()
        }
        subtitle?.id

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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        if (!PlayerHelper.pipEnabled) return

        if (player.playbackState == PlaybackState.STATE_PAUSED) return

        enterPictureInPictureMode(
            PictureInPictureParams.Builder()
                .setActions(emptyList())
                .setAspectRatio(player.videoSize.width, player.videoSize.height)
                .build()
        )

        super.onUserLeaveHint()
    }
}
