package com.github.libretube.activity

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.github.libretube.R
import com.github.libretube.RetrofitInstance
import com.github.libretube.databinding.ActivityMainBinding
import com.github.libretube.fragment.*
import com.google.android.material.color.DynamicColors

private const val TAG = "MainActivity"
private const val MOTION_LAYOUT_DELAY_IN_MILLIS = 100.toLong()

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        RetrofitInstance.url =
            sharedPreferences.getString("instance", "https://pipedapi.kavin.rocks/")!!
        DynamicColors.applyToActivitiesIfAvailable(application)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigationView.apply {
            setupWithNavController(navController)
            setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.homeFragment -> {
                        navController.backQueue.clear()
                        navController.navigate(R.id.homeFragment)
                        true
                    }
                    R.id.subscriptionsFragment -> {
                        //navController.backQueue.clear()
                        navController.navigate(R.id.subscriptionsFragment)
                        true
                    }
                    R.id.libraryFragment -> {
                        //navController.backQueue.clear()
                        navController.navigate(R.id.libraryFragment)
                        true
                    }
                }
                false
            }
        }

        val hexColor = String.format("#%06X", 0xFFFFFF and 0xcc322d)
        val appName = HtmlCompat.fromHtml(
            "Libre<span  style='color:$hexColor';>Tube</span>",
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        binding.toolbar.apply {
            title = appName
            setNavigationOnClickListener {
                navController.navigate(R.id.settingsFragment)
                true
            }
            setOnMenuItemClickListener {
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
        val data: Uri? = intent?.data
        var bundle = Bundle()

        Log.d(TAG, "dafaq" + data.toString())

        if (data != null) {
            Log.d("dafaq", data.host + " ${data.path} ")
            data.path?.let { path ->
                if (checkIfPathContainsSubString(path, setOf("/channel/", "/c/", "/user/"))) {
                    val channel = path.replace("/c/", "")
                        .replace("/user/", "")
                    bundle = bundleOf(KEY_CHANNEL_ID to channel)
                    navController.navigate(R.id.channelFragment, bundle)
                } else if (checkIfPathContainsSubString(path, setOf("/playlist"))) {
                    var playlist = data.query!!
                    playlist = checkIfQueryContainsString(playlist, "list=")
                    playlist = playlist.replace("list=", "")
                    bundle = bundleOf("playlist_id" to playlist)
                    navController.navigate(R.id.playlistFragment, bundle)
                } else if (checkIfPathContainsSubString(path,
                        setOf("/shorts/", "/embed/", "/v/"))
                ) {
                    val watch = path.replace("/shorts/", "")
                        .replace("/embed/", "")
                        .replace("/v/", "")
                    bundle.putString(KEY_VIDEO_ID, watch)
                    showPlayer(bundle)
                } else if (path.contains("/watch") && data.query != null) {
                    Log.d("dafaq", data.query!!)
                    var watch = data.query!!
                    watch = checkIfQueryContainsString(watch, "v=")
                    bundle.putString(KEY_VIDEO_ID, watch.replace("v=", ""))
                    showPlayer(bundle)
                } else {
                    val watch = path.replace("/", "")
                    bundle.putString(KEY_VIDEO_ID, watch)
                    showPlayer(bundle)
                }
            }
        }
    }

    private fun checkIfPathContainsSubString(path: String, substrings: Set<String>): Boolean {
        for (substring in substrings) {
            if (path.contains(substring)) {
                return true
            }
        }
        return false
    }

    private fun checkIfQueryContainsString(
        queryString: String,
        stringToFind: String,
    ): String {
        var modifiedQueryString = queryString
        if (modifiedQueryString.contains("&")) {
            val subStrings = modifiedQueryString.split("&")
            for (subString in subStrings) {
                if (subString.contains(stringToFind)) {
                    modifiedQueryString = subString
                    break
                }
            }
        }
        return modifiedQueryString
    }

    private fun showPlayer(
        bundle: Bundle,
    ) {
        val playerFragment = PlayerFragment().apply { arguments = bundle }
        supportFragmentManager.beginTransaction()
            .remove(PlayerFragment())
            .commit()
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, playerFragment)
            .commitNow()
        Handler().postDelayed({
            val motionLayout = findViewById<MotionLayout>(R.id.playerMotionLayout)
            motionLayout.transitionToEnd()
            motionLayout.transitionToStart()
        }, MOTION_LAYOUT_DELAY_IN_MILLIS)
    }

    override fun onBackPressed() {
        try {
            if (binding.mlMain.progress == 0.toFloat()) {
                binding.mlMain.transitionToEnd()
                findViewById<ConstraintLayout>(R.id.main_container).isClickable = false
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                val playerMotionLayout = findViewById<MotionLayout>(R.id.playerMotionLayout)
                with(playerMotionLayout) {
                    transitionToEnd()
                    getConstraintSet(R.id.start).constrainHeight(R.id.player, 0)
                    enableTransition(R.id.yt_transition, true)
                }
                findViewById<LinearLayout>(R.id.linLayout).isVisible = true
                isFullScreen = false
            } else {
                navController.popBackStack()
                if (navController.currentBackStackEntry == null) {
                    super.onBackPressed()
                }
            }
        } catch (e: Exception) {
            navController.popBackStack()
            if (navController.currentBackStackEntry == null) {
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
        } else
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
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
        } else
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_VISIBLE or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }
}
