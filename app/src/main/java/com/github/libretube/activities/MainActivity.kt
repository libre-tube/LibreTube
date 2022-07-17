package com.github.libretube.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.github.libretube.Globals
import com.github.libretube.PIPED_API_URL
import com.github.libretube.R
import com.github.libretube.databinding.ActivityMainBinding
import com.github.libretube.fragments.PlayerFragment
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.services.ClosingService
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.CronetHelper
import com.github.libretube.util.LocaleHelper
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.ThemeHelper
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    lateinit var binding: ActivityMainBinding

    lateinit var navController: NavController
    private var startFragmentId = R.id.homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        // set the app theme (e.g. Material You)
        ThemeHelper.updateTheme(this)

        // set the language
        LocaleHelper.updateLanguage(this)

        super.onCreate(savedInstanceState)

        // start service that gets called on closure
        startService(Intent(this, ClosingService::class.java))

        CronetHelper.initCronet(this.applicationContext)

        RetrofitInstance.url =
            PreferenceHelper.getString("selectInstance", PIPED_API_URL)!!
        // set auth instance
        RetrofitInstance.authUrl =
            if (PreferenceHelper.getBoolean("auth_instance_toggle", false)) {
                PreferenceHelper.getString(
                    "selectAuthInstance",
                    PIPED_API_URL
                )!!
            } else {
                RetrofitInstance.url
            }

        // save whether the data saver mode is enabled
        Globals.dataSaverModeEnabled = PreferenceHelper.getBoolean(
            "data_saver_mode",
            false
        )

        // show noInternet Activity if no internet available on app startup
        if (!ConnectionHelper.isNetworkAvailable(this)) {
            val noInternetIntent = Intent(this, NoInternetActivity::class.java)
            startActivity(noInternetIntent)
        } else {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            navController = findNavController(R.id.fragment)
            binding.bottomNav.setupWithNavController(navController)

            // gets the surface color of the bottom navigation view
            val color = SurfaceColors.getColorForElevation(this, 10F)

            // sets the navigation bar color to the previously calculated color
            window.navigationBarColor = color

            // hide the trending page if enabled
            val hideTrendingPage = PreferenceHelper.getBoolean("hide_trending_page", false)
            if (hideTrendingPage) binding.bottomNav.menu.findItem(R.id.homeFragment).isVisible =
                false

            // save start tab fragment id
            startFragmentId = when (PreferenceHelper.getString("default_tab", "home")) {
                "home" -> R.id.homeFragment
                "subscriptions" -> R.id.subscriptionsFragment
                "library" -> R.id.libraryFragment
                else -> R.id.homeFragment
            }

            // set default tab as start fragment
            navController.graph.setStartDestination(startFragmentId)

            // navigate to the default fragment
            navController.navigate(startFragmentId)

            val labelVisibilityMode = when (
                PreferenceHelper.getString("label_visibility", "always")
            ) {
                "always" -> NavigationBarView.LABEL_VISIBILITY_LABELED
                "selected" -> NavigationBarView.LABEL_VISIBILITY_SELECTED
                "never" -> NavigationBarView.LABEL_VISIBILITY_UNLABELED
                else -> NavigationBarView.LABEL_VISIBILITY_AUTO
            }
            binding.bottomNav.labelVisibilityMode = labelVisibilityMode

            binding.bottomNav.setOnItemSelectedListener {
                // clear backstack if it's the start fragment
                if (startFragmentId == it.itemId) navController.backQueue.clear()
                // set menu item on click listeners
                when (it.itemId) {
                    R.id.homeFragment -> {
                        navController.navigate(R.id.homeFragment)
                    }
                    R.id.subscriptionsFragment -> {
                        navController.navigate(R.id.subscriptionsFragment)
                    }
                    R.id.libraryFragment -> {
                        navController.navigate(R.id.libraryFragment)
                    }
                }
                false
            }

            binding.toolbar.title = ThemeHelper.getStyledAppName(this)

            binding.toolbar.setNavigationOnClickListener {
                // settings activity stuff
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

            binding.toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_search -> {
                        navController.navigate(R.id.searchFragment)
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
            navController.navigate(R.id.channelFragment, bundle)
        } else if (data.path!!.contains("/playlist")) {
            Log.i(TAG, "URI Type: Playlist")
            var playlist = data.query!!
            if (playlist.contains("&")) {
                val playlists = playlist.split("&")
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
                val watches = watch.split("&")
                for (v in watches) {
                    if (v.contains("v=")) {
                        watch = v
                        break
                    }
                }
            }
            val bundle = Bundle()
            bundle.putString("videoId", watch.replace("v=", ""))
            // for time stamped links
            if (data.query != null && data.query?.contains("t=")!!) {
                val timeStamp = data.query.toString().split("t=")[1]
                bundle.putLong("timeStamp", timeStamp.toLong())
            }
            loadWatch(bundle)
        } else {
            val watch = data.path!!.replace("/", "")
            val bundle = Bundle()
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
        val frag = PlayerFragment()
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
        if (binding.mainMotionLayout.progress == 0F) {
            try {
                minimizePlayer()
            } catch (e: Exception) {
                if (navController.currentDestination?.id == startFragmentId) {
                    // close app
                    moveTaskToBack(true)
                } else {
                    navController.popBackStack()
                }
            }
        } else if (navController.currentDestination?.id == startFragmentId) {
            super.onBackPressed()
        } else {
            navController.popBackStack()
        }
    }

    private fun minimizePlayer() {
        binding.mainMotionLayout.transitionToEnd()
        findViewById<ConstraintLayout>(R.id.main_container).isClickable = false
        val motionLayout = findViewById<MotionLayout>(R.id.playerMotionLayout)
        // set the animation duration
        motionLayout.setTransitionDuration(250)
        motionLayout.transitionToEnd()
        with(motionLayout) {
            getConstraintSet(R.id.start).constrainHeight(R.id.player, 0)
            enableTransition(R.id.yt_transition, true)
        }
        findViewById<LinearLayout>(R.id.linLayout).visibility = View.VISIBLE
        Globals.isFullScreen = false
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

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}
