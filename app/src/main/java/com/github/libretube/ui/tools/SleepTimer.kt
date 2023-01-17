package com.github.libretube.ui.tools

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.snackbar.Snackbar

object SleepTimer {
    private val handler = Handler(Looper.getMainLooper())
    private const val REACTION_INTERVAL = 5L

    /**
     * Kill the app after showing a warning after a certain amount of time
     * @param context This must not be the applicationContext!
     */
    fun setup(context: Context) {
        if (!PreferenceHelper.getBoolean(PreferenceKeys.SLEEP_TIMER, false)) return

        val breakReminderPref = PreferenceHelper.getString(
            PreferenceKeys.SLEEP_TIMER_DELAY,
            ""
        ).ifEmpty { return }

        handler.postDelayed(
            {
                var killApp = true
                val mainActivity = context as? MainActivity ?: return@postDelayed
                val snackBar = Snackbar.make(
                    mainActivity.binding.root,
                    R.string.take_a_break,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.cancel) {
                        killApp = false
                    }
                snackBar.show()
                (0..REACTION_INTERVAL).forEach {
                    handler.postDelayed({
                        val remainingTime = " (${REACTION_INTERVAL - it})"
                        snackBar.setText(context.getString(R.string.take_a_break) + remainingTime)
                    }, it * 1000)
                }
                handler.postDelayed(
                    killApp@{
                        if (!killApp) return@killApp

                        // kill the application
                        mainActivity.finishAffinity()
                        mainActivity.finish()
                        android.os.Process.killProcess(android.os.Process.myPid())
                    },
                    REACTION_INTERVAL * 1000
                )
            },
            breakReminderPref.toLong() * 60 * 1000
        )
    }
}
