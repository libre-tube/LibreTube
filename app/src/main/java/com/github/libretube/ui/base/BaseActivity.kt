package com.github.libretube.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.helpers.ThemeHelper

/**
 * Activity that applies the LibreTube theme and the in-app language
 */
open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // set the app theme (e.g. Material You)
        ThemeHelper.updateTheme(this)

        // set the apps language
        LocaleHelper.updateLanguage(this)

        super.onCreate(savedInstanceState)
    }
}
