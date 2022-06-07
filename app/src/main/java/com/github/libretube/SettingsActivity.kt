package com.github.libretube

import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.github.libretube.preferences.MainSettings
import com.github.libretube.util.ThemeHelper
import com.google.android.material.color.DynamicColors

var isCurrentViewMainSettings = true
var requireMainActivityRestart = false

class SettingsActivity : AppCompatActivity() {
    val TAG = "SettingsActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        ThemeHelper().updateTheme(this)

        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            overridePendingTransition(50, 50)
        }
        val view = this.findViewById<View>(android.R.id.content)
        view.alpha = 0F
        view.animate().alpha(1F).duration = 300

        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, MainSettings())
                .commit()
        }
    }

    override fun onBackPressed() {
        if (isCurrentViewMainSettings) {
            if (requireMainActivityRestart) {
                requireMainActivityRestart = false
                // kill player notification
                val nManager =
                    this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nManager.cancelAll()
                ThemeHelper().restartMainActivity(this)
                ActivityCompat.finishAffinity(this)
            } else {
                super.onBackPressed()
            }
            finishAndRemoveTask()
        } else {
            isCurrentViewMainSettings = true
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, MainSettings())
                .commit()
        }
    }
}
