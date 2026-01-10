package com.github.libretube.ui.tools

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.text.format.DateUtils
import com.github.libretube.R
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.ContextHelper
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

    var timeLeftMillis: Long = 0L
        private set
    private var timer: Timer? = null
    private var snackBar: Snackbar? = null
    private val handler = Handler(Looper.getMainLooper())


    /**
     * Start the sleep timer that will close the app after the specified delay
     *
     * @param context This must not be the applicationContext, but an activity context!
     * @param delayInMinutes The delay in minutes before the timer ends
     */
    fun start(context: Context, delayInMinutes: Long) {
        if (delayInMinutes == 0L) return

        // Stop any existing timer first
        stop(context)

        timeLeftMillis = delayInMinutes * DateUtils.MINUTE_IN_MILLIS

        timer = Timer()
        timer?.scheduleAtFixedRate(TIMER_DELAY, TIMER_DELAY) {
            onTimerTick(context)
        }
    }

    /**
     * Stop the sleep timer and restore volume to maximum
     */
    fun stop(context: Context) {
        cleanup()
        handler.post {
            BackgroundHelper.setVolume(context, MAX_VOLUME)
        }
    }


    private fun onTimerTick(context: Context) {
        timeLeftMillis -= TIMER_DELAY
        val secondsLeft = timeLeftMillis / DateUtils.SECOND_IN_MILLIS
        val fadeOutDuration = FADE_OUT_START_SECONDS * DateUtils.SECOND_IN_MILLIS

        // Smooth exponential fade out during last 10 seconds
        if (timeLeftMillis > 0 && timeLeftMillis <= fadeOutDuration) {
            val progress = (timeLeftMillis / fadeOutDuration.toFloat()).coerceIn(0f, 1f)
            val volume = progress.pow(2.0f) // Quadratic curve for natural fade
            handler.post {
                BackgroundHelper.setVolume(context, volume)
            }
        }

        // Update snackbar only on whole seconds to avoid excessive UI updates
        val isWholeSecond = timeLeftMillis % DateUtils.SECOND_IN_MILLIS == 0L
        if (secondsLeft in 1..SNACKBAR_START_SECONDS && isWholeSecond) {
            val activity = ContextHelper.tryUnwrapActivity<BaseActivity>(context)?.takeIf {
                !it.isFinishing && !it.isDestroyed
            }
            if (activity != null) {
                handler.post {
                    showOrUpdateSnackBar(activity)
                }
            }
        }

        // Close app with a pause after fade out completes
        if (timeLeftMillis <= 0) {
            handler.post {
                BackgroundHelper.setVolume(context, 0.0f)
            }
            handler.postDelayed({
                closeApp(context)
            }, CLOSE_DELAY)
        }
    }


    private fun showOrUpdateSnackBar(activity: BaseActivity) {
        val secondsLeft = timeLeftMillis / DateUtils.SECOND_IN_MILLIS
        val message = "${activity.getString(R.string.take_a_break)}: $secondsLeft"

        if (snackBar?.isShownOrQueued == true) {
            snackBar?.setText(message)
        } else {
            snackBar = Snackbar.make(
                activity.window.decorView.rootView,
                message,
                Snackbar.LENGTH_INDEFINITE
            ).setAction(R.string.cancel) {
                stop(activity)
            }
            snackBar?.show()
        }
    }

    private fun closeApp(context: Context) {
        val activity = context.let { ContextHelper.tryUnwrapActivity<BaseActivity>(it) }

        activity?.finishAffinity()
        activity?.finish()
        Process.killProcess(Process.myPid())
    }

    private fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        handler.post {
            snackBar?.dismiss()
            snackBar = null
        }

        timer?.cancel()
        timer = null
        timeLeftMillis = 0L
    }
}
