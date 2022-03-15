package com.github.libretube.activity

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.github.libretube.R
import com.github.libretube.RetrofitInstance
import com.github.libretube.databinding.ActivityMainBinding
import com.github.libretube.fragment.PlayerFragment
import com.github.libretube.fragment.isFullScreen
import com.google.android.material.color.DynamicColors
import java.lang.Exception

private const val TAG = "MainActivity"

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

        navController = findNavController(R.id.navHostFragment)

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
        intent?.action
        val data: Uri? = intent?.data
        Log.d(TAG, "dafaq" + data.toString())

        if (data != null) {
            Log.d("dafaq", data.host + " ${data.path} ")
            if (data.host != null) {
                if (data.path != null) {
                    if (data.path!!.contains("/channel/") || data.path!!.contains("/c/") || data.path!!.contains(
                            "/user/")
                    ) {
                        var channel = data.path
                        channel = channel!!.replace("/c/", "")
                        channel = channel.replace("/user/", "")
                        val bundle = bundleOf("channel_id" to channel)
                        navController.navigate(R.id.channelFragment, bundle)
                    } else if (data.path!!.contains("/playlist")) {
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
                    } else if (data.path!!.contains("/shorts/") || data.path!!.contains("/embed/") || data.path!!.contains(
                            "/v/")
                    ) {
                        var watch = data.path!!.replace("/shorts/", "").replace("/v/", "")
                            .replace("/embed/", "")
                        val bundle = Bundle()
                        val frag = PlayerFragment()

                        bundle.putString("videoId", watch)
                        frag.arguments = bundle
                        supportFragmentManager.beginTransaction()
                            .remove(PlayerFragment())
                            .commit()
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container, frag)
                            .commitNow()
                        Handler().postDelayed({
                            val motionLayout = findViewById<MotionLayout>(R.id.playerMotionLayout)
                            motionLayout.transitionToEnd()
                            motionLayout.transitionToStart()
                        }, 100)
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
                        var frag = PlayerFragment()
                        frag.arguments = bundle
                        supportFragmentManager.beginTransaction()
                            .remove(PlayerFragment())
                            .commit()
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container, frag)
                            .commitNow()
                        Handler().postDelayed({
                            val motionLayout = findViewById<MotionLayout>(R.id.playerMotionLayout)
                            motionLayout.transitionToEnd()
                            motionLayout.transitionToStart()
                        }, 100)

                    } else {
                        var watch = data.path!!.replace("/", "")
                        var bundle = Bundle()
                        bundle.putString("videoId", watch)
                        var frag = PlayerFragment()
                        frag.arguments = bundle
                        supportFragmentManager.beginTransaction()
                            .remove(PlayerFragment())
                            .commit()
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container, frag)
                            .commitNow()
                        Handler().postDelayed({
                            val motionLayout = findViewById<MotionLayout>(R.id.playerMotionLayout)
                            motionLayout.transitionToEnd()
                            motionLayout.transitionToStart()
                        }, 100)
                    }
                }

            }
        }
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
