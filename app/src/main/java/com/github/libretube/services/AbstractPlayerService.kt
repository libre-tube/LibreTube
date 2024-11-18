package com.github.libretube.services

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.app.ServiceCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.github.libretube.R
import com.github.libretube.api.obj.Subtitle
import com.github.libretube.enums.PlayerCommand
import com.github.libretube.enums.PlayerEvent
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.extensions.updateParameters
import com.github.libretube.helpers.PlayerHelper
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

    private var nowPlayingNotification: NowPlayingNotification? = null
    var trackSelector: DefaultTrackSelector? = null

    lateinit var videoId: String
    var isTransitioning = true

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
            toastFromMainThread(error.localizedMessage)
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
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        if (customCommand.customAction == START_SERVICE_ACTION) {
            PlayingQueue.resetToDefaults()

            CoroutineScope(Dispatchers.IO).launch {
                onServiceCreated(args)
                startPlayback()
            }

            return super.onCustomCommand(session, controller, customCommand, args)
        }

        if (customCommand.customAction == RUN_PLAYER_COMMAND_ACTION) {
            runPlayerCommand(args)

            return super.onCustomCommand(session, controller, customCommand, args)
        }

        handlePlayerAction(PlayerEvent.valueOf(customCommand.customAction))

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
                    val roleFlags = if (subtitle?.code != null) getSubtitleRoleFlags(subtitle) else 0
                    setPreferredTextRoleFlags(roleFlags)
                    setPreferredTextLanguage(subtitle?.code)
                }
            }
        }
    }

    fun getSubtitleRoleFlags(subtitle: Subtitle?): Int {
        return if (subtitle?.autoGenerated != true) {
            C.ROLE_FLAG_CAPTION
        } else {
            PlayerHelper.ROLE_FLAG_AUTO_GEN_SUBTITLE
        }
    }

    private fun handlePlayerAction(event: PlayerEvent) {
        if (PlayerHelper.handlePlayerAction(exoPlayer ?: return, event)) return

        when (event) {
            PlayerEvent.Next -> {
                PlayingQueue.navigateNext()
            }

            PlayerEvent.Prev -> {
                PlayingQueue.navigatePrev()
            }

            PlayerEvent.Stop -> {
                onDestroy()
            }

            else -> Unit
        }
    }

    abstract val isOfflinePlayer: Boolean
    abstract val isAudioOnlyPlayer: Boolean
    abstract val intentActivity: Class<*>

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaLibrarySession

    override fun onCreate() {
        super.onCreate()

        val notificationProvider = NowPlayingNotification(
            this,
            backgroundOnly = isAudioOnlyPlayer,
            offlinePlayer = isOfflinePlayer,
            intentActivity = intentActivity
        )
        setMediaNotificationProvider(notificationProvider)

        createPlayerAndMediaSession()
    }

    abstract suspend fun onServiceCreated(args: Bundle)

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)

        // Select the button to display.
        val customLayout = listOf(
            CommandButton.Builder()
                .setDisplayName(getString(R.string.rewind))
                .setSessionCommand(SessionCommand(PlayerEvent.Prev.name, Bundle.EMPTY))
                .setIconResId(R.drawable.ic_prev_outlined)
                .build(),
            CommandButton.Builder()
                .setDisplayName(getString(R.string.play_next))
                .setSessionCommand(SessionCommand(PlayerEvent.Next.name, Bundle.EMPTY))
                .setIconResId(R.drawable.ic_next_outlined)
                .build(),
        )
        val mediaNotificationSessionCommands =
            connectionResult.availableSessionCommands.buildUpon()
                .also { builder ->
                    builder.add(startServiceCommand)
                    builder.add(runPlayerActionCommand)
                    customLayout.forEach { commandButton ->
                        commandButton.sessionCommand?.let { builder.add(it) }
                    }
                }
                .build()

        val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
            .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
            .build()

        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(mediaNotificationSessionCommands)
            .setAvailablePlayerCommands(playerCommands)
            .setCustomLayout(customLayout)
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

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, this)
            .setId(this.javaClass.name)
            .build()
    }

    abstract suspend fun startPlayback()

    fun saveWatchPosition() {
        if (isTransitioning || !PlayerHelper.watchPositionsVideo) return

        exoPlayer?.let { PlayerHelper.saveWatchPosition(it, videoId) }
    }

    override fun onDestroy() {
        PlayingQueue.resetToDefaults()

        saveWatchPosition()

        nowPlayingNotification = null
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

    /**
     * Stop the service when app is removed from the task manager.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        onDestroy()
    }

    companion object {
        private const val START_SERVICE_ACTION = "start_service_action"
        private const val RUN_PLAYER_COMMAND_ACTION = "run_player_command_action"

        val startServiceCommand = SessionCommand(START_SERVICE_ACTION, Bundle.EMPTY)
        val runPlayerActionCommand = SessionCommand(RUN_PLAYER_COMMAND_ACTION, Bundle.EMPTY)
    }
}
