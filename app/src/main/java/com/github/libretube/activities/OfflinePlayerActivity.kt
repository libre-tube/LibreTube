package com.github.libretube.activities

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.ActivityOfflinePlayerBinding
import com.github.libretube.databinding.ExoStyledPlayerControlViewBinding
import com.github.libretube.extensions.BaseActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.FileDataSource
import java.io.File

class OfflinePlayerActivity : BaseActivity() {
    private lateinit var binding: ActivityOfflinePlayerBinding
    private lateinit var fileName: String
    private lateinit var player: ExoPlayer
    private lateinit var playerView: StyledPlayerView
    private lateinit var playerBinding: ExoStyledPlayerControlViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        hideSystemBars()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE

        super.onCreate(savedInstanceState)

        fileName = intent?.getStringExtra(IntentData.fileName)!!

        binding = ActivityOfflinePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializePlayer()
        playVideo()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .build()

        playerView = binding.player

        playerView.player = player

        playerBinding = binding.player.binding

        playerBinding.fullscreen.visibility = View.GONE
        playerBinding.closeImageButton.setOnClickListener {
            finish()
        }

        binding.player.initialize(
            supportFragmentManager,
            null,
            binding.doubleTapOverlay.binding,
            null
        )
    }

    private fun playVideo() {
        val videoDownloadDir = File(
            getExternalFilesDir(null),
            "video"
        )

        val videoFile = File(
            videoDownloadDir,
            fileName
        )

        val audioDownloadDir = File(
            getExternalFilesDir(null),
            "audio"
        )
        val audioFile = File(
            audioDownloadDir,
            fileName
        )

        val videoUri = if (videoFile.exists()) Uri.fromFile(videoFile) else null
        val audioUri = if (audioFile.exists()) Uri.fromFile(audioFile) else null

        setMediaSource(
            videoUri,
            audioUri
        )

        player.prepare()
        player.play()
    }

    private fun setMediaSource(videoUri: Uri?, audioUri: Uri?) {
        when {
            videoUri != null && audioUri != null -> {
                val videoSource = ProgressiveMediaSource.Factory(FileDataSource.Factory())
                    .createMediaSource(
                        MediaItem.fromUri(videoUri)
                    )

                val audioSource = ProgressiveMediaSource.Factory(FileDataSource.Factory())
                    .createMediaSource(
                        MediaItem.fromUri(audioUri)
                    )

                val mediaSource = MergingMediaSource(
                    audioSource,
                    videoSource
                )

                player.setMediaSource(mediaSource)
            }
            videoUri != null -> player.setMediaItem(
                MediaItem.fromUri(videoUri)
            )
            audioUri != null -> player.setMediaItem(
                MediaItem.fromUri(audioUri)
            )
        }
    }

    private fun hideSystemBars() {
        window?.decorView?.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        window.statusBarColor = Color.TRANSPARENT

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        )

        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

        supportActionBar?.hide()

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}
