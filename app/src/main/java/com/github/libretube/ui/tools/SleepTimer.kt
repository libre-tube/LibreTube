package com.github.libretube.ui.tools

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.core.os.bundleOf
import com.github.libretube.R
import com.github.libretube.enums.PlayerCommand
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.ContextHelper
import com.github.libretube.services.AbstractPlayerService
import com.github.libretube.services.OfflinePlayerService
import com.github.libretube.services.OnlinePlayerService
import com.github.libretube.ui.base.BaseActivity
import com.google.android.material.snackbar.Snackbar
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.div

object SleepTimer {
    private var handler: Handler? = null
    private const val REACTION_INTERVAL = 5L
    private const val PLAYER_VOLUME_FADE_OUT_DURATION = 15L
    private const val TIMER_DELAY = 1000L
    private const val MILLIS_PER_SECOND = 1000L
    private const val SECONDS_PER_MINUTE = 60

    @Volatile
    private var timer: Timer? = null
    private var snackBar: Snackbar? = null
    private var isOffline: Boolean = false
    var timeLeftMillis: Long = 0L

    /**
     * Kill the app after showing a warning after a certain amount of time
     *
     * @param context This must not be the applicationContext, but an activity context!
     * @param delayInMinutes The delay in minutes before the timer ends
     * @param isOffline Whether the offline player is being used
     */
    fun setup(context: Context, delayInMinutes: Long, isOffline: Boolean = false) {
        if (delayInMinutes == 0L) return

        handler = Handler(Looper.getMainLooper())

        timeLeftMillis = delayInMinutes * SECONDS_PER_MINUTE * MILLIS_PER_SECOND
        this.isOffline = isOffline

        timer = Timer()
        timer?.scheduleAtFixedRate(TIMER_DELAY, TIMER_DELAY) {
            handleTimerUpdate(context)
        }
    }


    /**
     * Disable the scheduled sleep timer
     */
    fun disableSleepTimer() {
        synchronized(this) {
            handler?.removeCallbacksAndMessages(null)
            handler = null
            timer?.cancel()
            timeLeftMillis = 0L
        }
    }

    private fun handleTimerUpdate(context: Context) {
        timeLeftMillis -= TIMER_DELAY

        if (timeLeftMillis <= PLAYER_VOLUME_FADE_OUT_DURATION * MILLIS_PER_SECOND) {
            val volume = timeLeftMillis.toFloat() / (PLAYER_VOLUME_FADE_OUT_DURATION * MILLIS_PER_SECOND)
            setPlayerVolume(context, volume)
        }

        if (timeLeftMillis <= REACTION_INTERVAL * MILLIS_PER_SECOND) {
            if (timeLeftMillis == REACTION_INTERVAL * MILLIS_PER_SECOND) {
                showSnackBar(context)
            }

            updateSnackBarText(context)

            if (timeLeftMillis == 0L) {
                val activity = ContextHelper.unwrapActivity<BaseActivity>(context)
                // kill the application
                activity.finishAffinity()
                activity.finish()
                Process.killProcess(Process.myPid())
            }

        }
    }

    private fun showSnackBar(context: Context) {
        handler?.post {
            val activity = ContextHelper.unwrapActivity<BaseActivity>(context)
            snackBar = Snackbar.make(
                activity.window.decorView.rootView,
                R.string.take_a_break,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.cancel) {
                    setPlayerVolume(context, 1.0f)
                    disableSleepTimer()
                }

            snackBar?.show()
        }
    }

    private fun updateSnackBarText(context: Context) {
        handler?.post {
            val secondsLeft = timeLeftMillis / MILLIS_PER_SECOND
            val message = context.getString(R.string.take_a_break) + " " + secondsLeft
            snackBar?.setText(message)
        }
    }


    /**
     * Set the volume of the currently active player service
     */
    private fun setPlayerVolume(context: Context, volume: Float) {
        val serviceClass = if (isOffline) {
            OfflinePlayerService::class.java
        } else {
            OnlinePlayerService::class.java
        }

        BackgroundHelper.startMediaService(context, serviceClass, Bundle.EMPTY) { controller ->
            controller.sendCustomCommand(
                AbstractPlayerService.runPlayerActionCommand,
                bundleOf(PlayerCommand.SET_VOLUME.name to volume)
            )
        }
    }
}
