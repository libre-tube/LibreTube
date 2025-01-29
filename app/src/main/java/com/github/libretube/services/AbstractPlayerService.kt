package com.github.libretube.services

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.annotation.CallSuper
import androidx.annotation.OptIn
import androidx.core.app.ServiceCompat
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.github.libretube.api.obj.Subtitle
import com.github.libretube.constants.IntentData
import com.github.libretube.enums.PlayerCommand
import com.github.libretube.enums.PlayerEvent
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.parcelableExtra
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.extensions.updateParameters
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.util.NowPlayingNotification
import com.github.libretube.util.PauseableTimer
import com.github.libretube.util.PlayingQueue
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
abstract class AbstractPlayerService : MediaLibraryService(), MediaLibrarySession.Callback {
    private var mediaLibrarySession: MediaLibrarySession? = null
    var exoPlayer: ExoPlayer? = null

    private var notificationProvider: NowPlayingNotification? = null
    var trackSelector: DefaultTrackSelector? = null

    lateinit var videoId: String
        private set

    var isTransitioning = false
        private set

    val handler = Handler(Looper.getMainLooper())

    private val watchPositionTimer = PauseableTimer(
        onTick = ::saveWatchPosition,
        delayMillis = PlayerHelper.WATCH_POSITION_TIMER_DELAY_MS
    )

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)

            // Start or pause watch position timer
            if (isPlaying) {
                watchPositionTimer.resume()
            } else {
                watchPositionTimer.pause()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            // show a toast on errors
            toastFromMainThread(error.localizedMessage.orEmpty())
        }

        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)

            if (events.contains(Player.EVENT_TRACKS_CHANGED)) {
                PlayerHelper.setPreferredAudioQuality(
                    this@AbstractPlayerService,
                    player,
                    trackSelector ?: return
                )
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            if (playbackState == Player.STATE_READY) {
                isTransitioning = false
            }
        }
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            START_SERVICE_ACTION -> {
                CoroutineScope(Dispatchers.IO).launch {
                    onServiceCreated(args)
                    notificationProvider?.intentActivity = getIntentActivity()

                    if (::videoId.isInitialized) startPlayback()
                }
            }

            STOP_SERVICE_ACTION -> {
                onDestroy()
            }

            RUN_PLAYER_COMMAND_ACTION -> {
                runPlayerCommand(args)
            }
        }

        return super.onCustomCommand(session, controller, customCommand, args)
    }

    open fun runPlayerCommand(args: Bundle) {
        when {
            args.containsKey(PlayerCommand.SKIP_SILENCE.name) -> exoPlayer?.skipSilenceEnabled =
                args.getBoolean(PlayerCommand.SKIP_SILENCE.name)

            args.containsKey(PlayerCommand.SET_VIDEO_TRACK_TYPE_DISABLED.name) -> trackSelector?.updateParameters {
                setTrackTypeDisabled(
                    C.TRACK_TYPE_VIDEO,
                    args.getBoolean(PlayerCommand.SET_VIDEO_TRACK_TYPE_DISABLED.name)
                )
            }

            args.containsKey(PlayerCommand.SET_AUDIO_ROLE_FLAGS.name) -> {
                trackSelector?.updateParameters {
                    setPreferredAudioRoleFlags(args.getInt(PlayerCommand.SET_AUDIO_ROLE_FLAGS.name))
                }
            }

            args.containsKey(PlayerCommand.SET_AUDIO_LANGUAGE.name) -> {
                trackSelector?.updateParameters {
                    setPreferredAudioLanguage(args.getString(PlayerCommand.SET_AUDIO_LANGUAGE.name))
                }
            }

            args.containsKey(PlayerCommand.SET_RESOLUTION.name) -> {
                trackSelector?.updateParameters {
                    val resolution = args.getInt(PlayerCommand.SET_RESOLUTION.name)
                    setMinVideoSize(Int.MIN_VALUE, resolution)
                    setMaxVideoSize(Int.MAX_VALUE, resolution)
                }
            }

            args.containsKey(PlayerCommand.SET_SUBTITLE.name) -> {
                val subtitle: Subtitle? = args.parcelable(PlayerCommand.SET_SUBTITLE.name)

                trackSelector?.updateParameters {
                    val roleFlags =
                        if (subtitle?.code != null) getSubtitleRoleFlags(subtitle) else 0
                    setPreferredTextRoleFlags(roleFlags)
                    setPreferredTextLanguage(subtitle?.code)
                }
            }

            args.containsKey(PlayerCommand.PLAY_VIDEO_BY_ID.name) -> {
                navigateVideo(args.getString(PlayerCommand.PLAY_VIDEO_BY_ID.name) ?: return)
            }
        }
    }

    private fun navigateVideo(videoId: String) {
        setVideoId(videoId)

        CoroutineScope(Dispatchers.IO).launch {
            startPlayback()
        }
    }

    fun getSubtitleRoleFlags(subtitle: Subtitle?): Int {
        return if (subtitle?.autoGenerated != true) {
            C.ROLE_FLAG_CAPTION
        } else {
            PlayerHelper.ROLE_FLAG_AUTO_GEN_SUBTITLE
        }
    }

    /**
     * Update the [videoId] to the new videoId and change the playlist metadata
     * to reflect that videoId change
     */
    protected fun setVideoId(videoId: String) {
        this.videoId = videoId

        updatePlaylistMetadata {
            setExtras(bundleOf(IntentData.videoId to videoId))
        }
    }

    protected fun updatePlaylistMetadata(updateAction: MediaMetadata.Builder.() -> Unit) {
        handler.post {
            exoPlayer?.playlistMetadata = MediaMetadata.Builder()
                .apply(updateAction)
                // send a unique timestamp to notify that the metadata changed, even if playing the same video twice
                .setTrackNumber(System.currentTimeMillis().mod(Int.MAX_VALUE))
                .build()
        }
    }

    private fun handlePlayerAction(event: PlayerEvent) {
        if (PlayerHelper.handlePlayerAction(exoPlayer ?: return, event)) return

        when (event) {
            PlayerEvent.Next -> {
                navigateVideo(PlayingQueue.getNext() ?: return)
            }

            PlayerEvent.Prev -> {
                navigateVideo(PlayingQueue.getPrev() ?: return)
            }

            PlayerEvent.Stop -> {
                onDestroy()
            }

            else -> Unit
        }
    }

    abstract val isOfflinePlayer: Boolean
    abstract val isAudioOnlyPlayer: Boolean

    val watchPositionsEnabled get() =
        (PlayerHelper.watchPositionsAudio && isAudioOnlyPlayer) || (PlayerHelper.watchPositionsVideo && !isAudioOnlyPlayer)

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaLibrarySession

    override fun onCreate() {
        super.onCreate()

        notificationProvider = NowPlayingNotification(
            this,
            backgroundOnly = isAudioOnlyPlayer,
            offlinePlayer = isOfflinePlayer,
        )
        setMediaNotificationProvider(notificationProvider!!)

        createPlayerAndMediaSession()
    }

    open fun getIntentActivity(): Class<*> = MainActivity::class.java

    abstract suspend fun onServiceCreated(args: Bundle)

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)

        val mediaNotificationSessionCommands =
            connectionResult.availableSessionCommands.buildUpon()
                .also { builder ->
                    builder.addSessionCommands(
                        listOf(
                            startServiceCommand,
                            runPlayerActionCommand,
                            stopServiceCommand
                        )
                    )
                }
                .build()

        val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .build()

        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(mediaNotificationSessionCommands)
            .setAvailablePlayerCommands(playerCommands)
            .build()
    }

    @OptIn(UnstableApi::class)
    private fun createPlayerAndMediaSession() {
        val trackSelector = DefaultTrackSelector(this)
        this.trackSelector = trackSelector

        if (isAudioOnlyPlayer) {
            trackSelector.updateParameters {
                setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            }
        }

        val player = PlayerHelper.createPlayer(this, trackSelector, true)
        // prevent android from putting LibreTube to sleep when locked
        player.setWakeMode(if (isOfflinePlayer) C.WAKE_MODE_LOCAL else C.WAKE_MODE_NETWORK)
        player.addListener(playerListener)
        this.exoPlayer = player

        PlayerHelper.setPreferredCodecs(trackSelector)

        val forwardingPlayer = MediaSessionForwarder(player)
        mediaLibrarySession = MediaLibrarySession.Builder(this, forwardingPlayer, this)
            .setId(this.javaClass.name)
            .build()
    }

    /**
     * Load the stream source and start the playback.
     *
     * This function should base its actions on the videoId variable.
     */
    @CallSuper
    open suspend fun startPlayback() {
        isTransitioning = true
    }

    private fun saveWatchPosition() {
        if (isTransitioning || !watchPositionsEnabled || !::videoId.isInitialized) return

        exoPlayer?.let { PlayerHelper.saveWatchPosition(it, videoId) }
    }

    override fun onMediaButtonEvent(
        session: MediaSession,
        controllerInfo: MediaSession.ControllerInfo,
        intent: Intent
    ): Boolean {
        val event: KeyEvent = intent.parcelableExtra(Intent.EXTRA_KEY_EVENT) ?: return false
        when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                handlePlayerAction(PlayerEvent.Next)
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                handlePlayerAction(PlayerEvent.Prev)
                return true
            }

            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                handlePlayerAction(PlayerEvent.Rewind)
            }

            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                handlePlayerAction(PlayerEvent.Forward)
            }

            KeyEvent.KEYCODE_MEDIA_STOP -> {
                handlePlayerAction(PlayerEvent.Stop)
            }
        }

        return super.onMediaButtonEvent(session, controllerInfo, intent)
    }

    override fun onDestroy() {
        // wait for a short time before killing the mediaSession
        // as the playerController must be released before we finish the session
        // otherwise there would be a
        // java.lang.SecurityException: Session rejected the connection request.
        // because there can't be two active playerControllers at the same time.
        handler.postDelayed(50) {
            saveWatchPosition()

            notificationProvider = null
            watchPositionTimer.destroy()

            handler.removeCallbacksAndMessages(null)

            runCatching {
                exoPlayer?.stop()
                exoPlayer?.release()
            }

            kotlin.runCatching {
                mediaLibrarySession?.release()
                mediaLibrarySession = null
            }

            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()

            super.onDestroy()
        }
    }

    /**
     * Stop the service when app is removed from the task manager.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        onDestroy()
    }

    /**
     * [Player] wrapper that handles seeking actions (next/previous) itself instead of using the
     * default [Player] implementation
     */
    inner class MediaSessionForwarder(player: Player) : ForwardingPlayer(player) {
        override fun hasNextMediaItem(): Boolean {
            return PlayingQueue.hasNext()
        }

        override fun hasPreviousMediaItem(): Boolean {
            return PlayingQueue.hasPrev()
        }

        override fun seekToPrevious() {
            handlePlayerAction(PlayerEvent.Prev)
        }

        override fun seekToNext() {
            handlePlayerAction(PlayerEvent.Next)
        }

        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands().buildUpon()
                .addAll(Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_NEXT)
                .build()
        }

        override fun isCommandAvailable(command: Int): Boolean {
            if (command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS) return true

            return super.isCommandAvailable(command)
        }
    }

    companion object {
        private const val START_SERVICE_ACTION = "start_service_action"
        private const val STOP_SERVICE_ACTION = "stop_service_action"
        private const val RUN_PLAYER_COMMAND_ACTION = "run_player_command_action"

        val startServiceCommand = SessionCommand(START_SERVICE_ACTION, Bundle.EMPTY)
        val stopServiceCommand = SessionCommand(STOP_SERVICE_ACTION, Bundle.EMPTY)
        val runPlayerActionCommand = SessionCommand(RUN_PLAYER_COMMAND_ACTION, Bundle.EMPTY)
    }
}
