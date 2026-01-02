package com.github.libretube.ui.tools

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.text.format.DateUtils
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
import kotlin.math.pow

object SleepTimer {
    private const val TIMER_DELAY = 500L // 500ms for smoother fade out
    private const val FADE_OUT_START_SECONDS = 10L
    private const val SNACKBAR_START_SECONDS = 5L
    private const val MAX_VOLUME = 1.0f
    private const val CLOSE_DELAY = 500L // Pause before closing

    private var isOffline: Boolean = false
    var timeLeftMillis: Long = 0L
        private set
    private var timer: Timer? = null
    private var snackBar: Snackbar? = null
    private var activity: BaseActivity? = null
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Start the sleep timer that will close the app after the specified delay
     *
     * @param context This must not be the applicationContext, but an activity context!
     * @param delayInMinutes The delay in minutes before the timer ends
     * @param isOffline Whether the offline player is being used
     */
    fun start(context: Context, delayInMinutes: Long, isOffline: Boolean = false) {
        if (delayInMinutes == 0L) return

        // Stop any existing timer first
        stop()

        activity = ContextHelper.unwrapActivity(context)
        timeLeftMillis = delayInMinutes * DateUtils.MINUTE_IN_MILLIS
        this.isOffline = isOffline

        timer = Timer()
        timer?.scheduleAtFixedRate(TIMER_DELAY, TIMER_DELAY) {
            onTimerTick()
        }
    }

    /**
     * Stop the sleep timer and restore volume to maximum
     */
    fun stop() {
        setPlayerVolume(MAX_VOLUME)
        cleanup()
    }


    private fun onTimerTick() {
        timeLeftMillis -= TIMER_DELAY
        val secondsLeft = timeLeftMillis / DateUtils.SECOND_IN_MILLIS
        val fadeOutDuration = FADE_OUT_START_SECONDS * DateUtils.SECOND_IN_MILLIS

        // Smooth exponential fade out during last 10 seconds
        if (timeLeftMillis > 0 && timeLeftMillis <= fadeOutDuration) {
            val progress = (timeLeftMillis / fadeOutDuration.toFloat()).coerceIn(0f, 1f)
            val volume = progress.pow(2.0f) // Quadratic curve for natural fade
            setPlayerVolume(volume)
        }

        // Update snackbar only on whole seconds to avoid excessive UI updates
        val isWholeSecond = timeLeftMillis % DateUtils.SECOND_IN_MILLIS == 0L
        if (secondsLeft in 1..SNACKBAR_START_SECONDS && isWholeSecond) {
            handler.post { showOrUpdateSnackBar() }
        }

        // Close app with a pause after fade out completes
        if (timeLeftMillis <= 0) {
            setPlayerVolume(0.0f) // Ensure complete silence
            handler.postDelayed({
                closeApp()
            }, CLOSE_DELAY)
        }
    }


    private fun showOrUpdateSnackBar() {
        val act = activity?.takeIf { !it.isFinishing && !it.isDestroyed } ?: run {
            stop()
            return
        }

        val secondsLeft = timeLeftMillis / DateUtils.SECOND_IN_MILLIS
        val message = "${act.getString(R.string.take_a_break)}: ${secondsLeft}"

        if (snackBar?.isShownOrQueued == true) {
            snackBar?.setText(message)
        } else {
            snackBar = Snackbar.make(
                act.window.decorView.rootView,
                message,
                Snackbar.LENGTH_INDEFINITE
            ).setAction(R.string.cancel) {
                stop()
            }
            snackBar?.show()
        }
    }

    private fun closeApp() {
        cleanup()
        // kill the application
        activity?.finishAffinity()
        activity?.finish()
        Process.killProcess(Process.myPid())
    }

    private fun cleanup() {
        timer?.cancel()
        timer = null

        handler.removeCallbacksAndMessages(null)
        handler.post {
            snackBar?.dismiss()
            snackBar = null
        }

        activity = null
        timeLeftMillis = 0L
    }


    private fun setPlayerVolume(volume: Float) {
        val act = activity ?: return

        val serviceClass = if (isOffline) {
            OfflinePlayerService::class.java
        } else {
            OnlinePlayerService::class.java
        }

        BackgroundHelper.startMediaService(act, serviceClass, Bundle.EMPTY) { controller ->
            controller.sendCustomCommand(
                AbstractPlayerService.runPlayerActionCommand,
                bundleOf(PlayerCommand.SET_VOLUME.name to volume)
            )
        }
    }
}
