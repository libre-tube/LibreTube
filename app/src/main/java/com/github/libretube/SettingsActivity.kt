package com.github.libretube

import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.github.libretube.preferences.SettingsFragment
import com.github.libretube.util.restartMainActivity
import com.github.libretube.util.updateTheme
import com.google.android.material.color.DynamicColors

var isCurrentViewMainSettings = true
var requireMainActivityRestart = false

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        updateTheme(this)

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
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }

    override fun onBackPressed() {
        if (isCurrentViewMainSettings) {
            if (requireMainActivityRestart) {
                requireMainActivityRestart = false
                // kill player notification
                val nManager =
                    this.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
                nManager.cancelAll()
                restartMainActivity(this)
                ActivityCompat.finishAffinity(this)
            } else {
                super.onBackPressed()
            }
            finishAndRemoveTask()
        } else {
            isCurrentViewMainSettings = true
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }
}
