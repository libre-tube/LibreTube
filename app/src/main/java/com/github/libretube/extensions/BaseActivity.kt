package com.github.libretube.extensions

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.libretube.util.ThemeHelper

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // set the app theme (e.g. Material You)
        ThemeHelper.updateTheme(this)

        super.onCreate(savedInstanceState)
    }
}
