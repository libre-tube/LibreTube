package com.github.libretube

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.github.libretube.fragments.PlayerFragment
import com.github.libretube.fragments.isFullScreen
import com.github.libretube.preferences.SponsorBlockSettings
import com.github.libretube.util.CronetHelper
import com.github.libretube.util.LocaleHelper
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.ThemeHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    lateinit var bottomNavigationView: BottomNavigationView
    lateinit var toolbar: Toolbar
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        CronetHelper.initCronet(this.applicationContext)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        RetrofitInstance.url =
            sharedPreferences.getString("selectInstance", "https://pipedapi.kavin.rocks/")!!
        SponsorBlockSettings.sponsorBlockEnabled =
            sharedPreferences.getBoolean("sb_enabled_key", false)
        SponsorBlockSettings.sponsorNotificationsEnabled =
            sharedPreferences.getBoolean("sb_notifications_key", false)
        SponsorBlockSettings.introEnabled =
            sharedPreferences.getBoolean("intro_category_key", false)
        SponsorBlockSettings.selfPromoEnabled =
            sharedPreferences.getBoolean("selfpromo_category_key", false)
        SponsorBlockSettings.interactionEnabled =
            sharedPreferences.getBoolean("interaction_category_key", false)
        SponsorBlockSettings.sponsorsEnabled =
            sharedPreferences.getBoolean("sponsors_category_key", false)
        SponsorBlockSettings.outroEnabled =
            sharedPreferences.getBoolean("outro_category_key", false)
        SponsorBlockSettings.fillerEnabled =
            sharedPreferences.getBoolean("filler_category_key", false)
        SponsorBlockSettings.musicOfftopicEnabled =
            sharedPreferences.getBoolean("music_offtopic_category_key", false)
        SponsorBlockSettings.previewEnabled =
            sharedPreferences.getBoolean("preview_category_key", false)

        ThemeHelper().updateTheme(this)
        LocaleHelper().updateLanguage(this)

        val connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected

        if (!isConnected) {
            setContentView(R.layout.activity_nointernet)
            findViewById<Button>(R.id.retry_button).setOnClickListener {
                recreate()
            }
            findViewById<ImageView>(R.id.noInternet_settingsImageView).setOnClickListener {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        } else {
            setContentView(R.layout.activity_main)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT

            bottomNavigationView = findViewById(R.id.bottomNav)
            navController = findNavController(R.id.fragment)
            bottomNavigationView.setupWithNavController(navController)

            when (sharedPreferences.getString("default_tab", "home")!!) {
                "home" -> navController.navigate(R.id.home2)
                "subscriptions" -> navController.navigate(R.id.subscriptions)
                "library" -> navController.navigate(R.id.library)
            }

            bottomNavigationView.setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.home2 -> {
                        navController.backQueue.clear()
                        navController.navigate(R.id.home2)
                        true
                    }
                    R.id.subscriptions -> {
                        // navController.backQueue.clear()
                        navController.navigate(R.id.subscriptions)
                        true
                    }
                    R.id.library -> {
                        // navController.backQueue.clear()
                        navController.navigate(R.id.library)
                        true
                    }
                }
                false
            }

            toolbar = findViewById(R.id.toolbar)
            val typedValue = TypedValue()
            this.theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
            val hexColor = String.format("#%06X", (0xFFFFFF and typedValue.data))
            val appName = HtmlCompat.fromHtml(
                "Libre<span  style='color:$hexColor';>Tube</span>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            toolbar.title = appName

            toolbar.setNavigationOnClickListener {
                // settings activity stuff
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }

            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_search -> {
                        navController.navigate(R.id.searchFragment)
                        true
                    }
                }
                false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intentData: Uri? = intent?.data
        // check whether an URI got submitted over the intent data
        if (intentData != null && intentData.host != null && intentData.path != null) {
            Log.d("intentData", "${intentData.host} ${intentData.path} ")
            // load the URI of the submitted link (e.g. video)
            loadIntentData(intentData)
        }
    }

    private fun loadIntentData(data: Uri) {
        // channel
        if (data.path!!.contains("/channel/") ||
            data.path!!.contains("/c/") ||
            data.path!!.contains("/user/")
        ) {
            Log.i(TAG, "URI Type: Channel")
            var channel = data.path
            channel = channel!!.replace("/c/", "")
            channel = channel.replace("/user/", "")
            val bundle = bundleOf("channel_id" to channel)
            navController.navigate(R.id.channel, bundle)
        } else if (data.path!!.contains("/playlist")) {
            Log.i(TAG, "URI Type: Playlist")
            var playlist = data.query!!
            if (playlist.contains("&")) {
                var playlists = playlist.split("&")
                for (v in playlists) {
                    if (v.contains("list=")) {
                        playlist = v
                        break
                    }
                }
            }
            playlist = playlist.replace("list=", "")
            val bundle = bundleOf("playlist_id" to playlist)
            navController.navigate(R.id.playlistFragment, bundle)
        } else if (data.path!!.contains("/shorts/") ||
            data.path!!.contains("/embed/") ||
            data.path!!.contains("/v/")
        ) {
            Log.i(TAG, "URI Type: Video")
            val watch = data.path!!
                .replace("/shorts/", "")
                .replace("/v/", "")
                .replace("/embed/", "")
            val bundle = Bundle()
            bundle.putString("videoId", watch)
            // for time stamped links
            if (data.query != null && data.query?.contains("t=")!!) {
                val timeStamp = data.query.toString().split("t=")[1]
                bundle.putLong("timeStamp", timeStamp.toLong())
            }
            loadWatch(bundle)
        } else if (data.path!!.contains("/watch") && data.query != null) {
            Log.d("dafaq", data.query!!)
            var watch = data.query!!
            if (watch.contains("&")) {
                var watches = watch.split("&")
                for (v in watches) {
                    if (v.contains("v=")) {
                        watch = v
                        break
                    }
                }
            }
            var bundle = Bundle()
            bundle.putString("videoId", watch.replace("v=", ""))
            // for time stamped links
            if (data.query != null && data.query?.contains("t=")!!) {
                val timeStamp = data.query.toString().split("t=")[1]
                bundle.putLong("timeStamp", timeStamp.toLong())
            }
            loadWatch(bundle)
        } else {
            var watch = data.path!!.replace("/", "")
            var bundle = Bundle()
            bundle.putString("videoId", watch)
            // for time stamped links
            if (data.query != null && data.query?.contains("t=")!!) {
                val timeStamp = data.query.toString().split("t=")[1]
                bundle.putLong("timeStamp", timeStamp.toLong())
            }
            loadWatch(bundle)
        }
    }

    private fun loadWatch(bundle: Bundle) {
        var frag = PlayerFragment()
        frag.arguments = bundle
        supportFragmentManager.beginTransaction()
            .remove(PlayerFragment())
            .commit()
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, frag)
            .commitNow()
        Handler(Looper.getMainLooper()).postDelayed({
            val motionLayout = findViewById<MotionLayout>(R.id.playerMotionLayout)
            motionLayout.transitionToEnd()
            motionLayout.transitionToStart()
        }, 100)
    }

    override fun onBackPressed() {
        try {
            val mainMotionLayout = findViewById<MotionLayout>(R.id.mainMotionLayout)
            if (mainMotionLayout.progress == 0.toFloat()) {
                mainMotionLayout.transitionToEnd()
                findViewById<ConstraintLayout>(R.id.main_container).isClickable = false
                val motionLayout = findViewById<MotionLayout>(R.id.playerMotionLayout)
                motionLayout.transitionToEnd()
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                with(motionLayout) {
                    getConstraintSet(R.id.start).constrainHeight(R.id.player, 0)
                    enableTransition(R.id.yt_transition, true)
                }
                findViewById<LinearLayout>(R.id.linLayout).visibility = View.VISIBLE
                isFullScreen = false
            } else {
                navController.popBackStack()
                if (navController.currentBackStackEntry == null &&
                    (parent as View).id != R.id.settings
                ) {
                    super.onBackPressed()
                }
            }
        } catch (e: Exception) {
            // try catch to prevent nointernet activity to crash
            try {
                navController.popBackStack()
                moveTaskToBack(true)
            } catch (e: Exception) {
                super.onBackPressed()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val orientation = newConfig.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            println("Portrait")
            unsetFullscreen()
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            println("Landscape")
            setFullscreen()
        }
    }

    private fun setFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    private fun unsetFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.apply {
                show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_VISIBLE or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        supportFragmentManager.fragments.forEach { fragment ->
            (fragment as? PlayerFragment)?.onUserLeaveHint()
        }
    }
}

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}
