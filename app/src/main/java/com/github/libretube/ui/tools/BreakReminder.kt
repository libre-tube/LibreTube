package com.github.libretube.ui.tools

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object BreakReminder {
    /**
     * Show a break reminder when watched too long
     */
    fun setupBreakReminder(context: Context) {
        if (!PreferenceHelper.getBoolean(
                PreferenceKeys.BREAK_REMINDER_TOGGLE,
                false
            )
        ) {
            return
        }
        val breakReminderPref = PreferenceHelper.getString(
            PreferenceKeys.BREAK_REMINDER,
            "0"
        )
        if (!breakReminderPref.all { Character.isDigit(it) } ||
            breakReminderPref == "" || breakReminderPref == "0"
        ) {
            return
        }
        Handler(Looper.getMainLooper()).postDelayed(
            {
                try {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.take_a_break)
                        .setMessage(
                            context.getString(
                                R.string.already_spent_time,
                                breakReminderPref
                            )
                        )
                        .setPositiveButton(R.string.okay, null)
                        .show()
                } catch (e: Exception) {
                    runCatching {
                        Toast.makeText(context, R.string.take_a_break, Toast.LENGTH_LONG).show()
                    }
                }
            },
            breakReminderPref.toLong() * 60 * 1000
        )
    }
}
