package com.github.libretube.ui.tools

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.core.os.postDelayed
import com.github.libretube.R
import com.github.libretube.ui.activities.MainActivity
import com.google.android.material.snackbar.Snackbar
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

object SleepTimer {
    private val handler = Handler(Looper.getMainLooper())
    private const val REACTION_INTERVAL = 5L
    private const val TIMER_DELAY = 1000L
    private var timer: Timer? = null
    var timeLeftMillis: Long = 0L

    /**
     * Kill the app after showing a warning after a certain amount of time
     *
     * @param context This must not be the applicationContext, but an activity context!
     */
    fun setup(context: Context, delayInMinutes: Long) {
        if (delayInMinutes == 0L) return

        timeLeftMillis = delayInMinutes * 60 * 1000

        timer = Timer()
        timer?.scheduleAtFixedRate(TIMER_DELAY, TIMER_DELAY) {
            timeLeftMillis -= TIMER_DELAY
            if (timeLeftMillis == 0L) showTimerEndedSnackBar(context)
        }
    }

    /**
     * Disable the scheduled sleep timer
     */
    fun disableSleepTimer() {
        timer?.cancel()
        timeLeftMillis = 0L
    }

    private fun showTimerEndedSnackBar(context: Context) {
        var killApp = true
        val mainActivity = context as? MainActivity ?: return
        val snackBar = Snackbar.make(
            mainActivity.binding.root,
            R.string.take_a_break,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.cancel) {
                killApp = false
            }
        snackBar.show()
        for (i in 0..REACTION_INTERVAL) {
            handler.postDelayed(i * 1000) {
                val remainingTime = " (${REACTION_INTERVAL - i})"
                snackBar.setText(context.getString(R.string.take_a_break) + remainingTime)
            }
        }
        handler.postDelayed(REACTION_INTERVAL * 1000) {
            if (killApp) {
                // kill the application
                mainActivity.finishAffinity()
                mainActivity.finish()
                Process.killProcess(Process.myPid())
            }
        }
    }
}
