package com.github.libretube.ui.base

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ThemeHelper
import com.github.libretube.helpers.ThemeHelper.getThemeMode
import com.github.libretube.helpers.WindowHelper
import java.util.Locale

/**
 * Activity that applies the LibreTube theme and the in-app language
 */
open class BaseActivity : AppCompatActivity() {
    open val isDialogActivity: Boolean = false

    val screenOrientationPref by lazy {
        val orientationPref = PreferenceHelper.getString(
            PreferenceKeys.ORIENTATION,
            resources.getString(R.string.config_default_orientation_pref)
        )
        when (orientationPref) {
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            "auto" -> ActivityInfo.SCREEN_ORIENTATION_USER
            else -> throw IllegalArgumentException()
        }
    }

    /**
     * Whether the phone of the user has a cutout like a notch or not
     */
    var hasCutout: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // set the app theme (e.g. Material You)
        ThemeHelper.updateTheme(this)
        if (isDialogActivity) ThemeHelper.applyDialogActivityTheme(this)

        requestOrientationChange()

        // wait for the window decor view to be drawn before detecting display cutouts
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            hasCutout = WindowHelper.hasCutout(view)
            window.decorView.onApplyWindowInsets(insets)
        }

        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)

        val configuration = Configuration().apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // TODO: remove this case in the future
                @Suppress("DEPRECATION")
                val locale = LocaleHelper.getAppLocale()
                Locale.setDefault(locale)
                setLocale(locale)
            }

            val uiPref = PreferenceHelper.getString(PreferenceKeys.THEME_MODE, "A")
            AppCompatDelegate.setDefaultNightMode(getThemeMode(uiPref))
        }

        applyOverrideConfiguration(configuration)
    }

    /**
     * Rotate the screen according to the app orientation preference
     */
    open fun requestOrientationChange() {
        requestedOrientation = screenOrientationPref
    }
}
