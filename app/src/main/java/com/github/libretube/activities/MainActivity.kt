package com.github.libretube.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.github.libretube.R
import com.github.libretube.databinding.ActivityMainBinding
import com.github.libretube.dialogs.ErrorDialog
import com.github.libretube.extensions.BaseActivity
import com.github.libretube.extensions.TAG
import com.github.libretube.fragments.PlayerFragment
import com.github.libretube.models.PlayerViewModel
import com.github.libretube.models.SearchViewModel
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import com.github.libretube.services.ClosingService
import com.github.libretube.util.LocaleHelper
import com.github.libretube.util.NetworkHelper
import com.github.libretube.util.ThemeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.navigation.NavigationBarView

class MainActivity : BaseActivity() {

    lateinit var binding: ActivityMainBinding

    lateinit var navController: NavController
    private var startFragmentId = R.id.homeFragment
    var autoRotationEnabled = false

    lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        // set the language
        LocaleHelper.updateLanguage(this)

        super.onCreate(savedInstanceState)

        autoRotationEnabled = PreferenceHelper.getBoolean(PreferenceKeys.AUTO_ROTATION, false)

        // enable auto rotation if turned on
        requestedOrientation = if (autoRotationEnabled) ActivityInfo.SCREEN_ORIENTATION_USER
        else ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT

        // start service that gets called on closure
        try {
            startService(Intent(this, ClosingService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // show noInternet Activity if no internet available on app startup
        if (!NetworkHelper.isNetworkAvailable(this)) {
            val noInternetIntent = Intent(this, NoInternetActivity::class.java)
            startActivity(noInternetIntent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // set the action bar for the activity
        setSupportActionBar(binding.toolbar)

        navController = findNavController(R.id.fragment)
        binding.bottomNav.setupWithNavController(navController)

        // gets the surface color of the bottom navigation view
        val color = SurfaceColors.getColorForElevation(this, 10F)

        // sets the navigation bar color to the previously calculated color
        window.navigationBarColor = color

        // hide the trending page if enabled
        val hideTrendingPage =
            PreferenceHelper.getBoolean(PreferenceKeys.HIDE_TRENDING_PAGE, false)
        if (hideTrendingPage) binding.bottomNav.menu.findItem(R.id.homeFragment).isVisible =
            false

        // save start tab fragment id
        startFragmentId =
            when (PreferenceHelper.getString(PreferenceKeys.DEFAULT_TAB, "home")) {
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
            PreferenceHelper.getString(PreferenceKeys.LABEL_VISIBILITY, "always")
        ) {
            "always" -> NavigationBarView.LABEL_VISIBILITY_LABELED
            "selected" -> NavigationBarView.LABEL_VISIBILITY_SELECTED
            "never" -> NavigationBarView.LABEL_VISIBILITY_UNLABELED
            else -> NavigationBarView.LABEL_VISIBILITY_AUTO
        }
        binding.bottomNav.labelVisibilityMode = labelVisibilityMode

        binding.bottomNav.setOnApplyWindowInsetsListener(null)

        binding.bottomNav.setOnItemSelectedListener {
            // clear backstack if it's the start fragment
            if (startFragmentId == it.itemId) navController.backQueue.clear()
            // set menu item on click listeners
            removeSearchFocus()
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

        /**
         * handle error logs
         */
        val log = PreferenceHelper.getErrorLog()
        if (log != "") ErrorDialog().show(supportFragmentManager, null)

        setupBreakReminder()
    }

    /**
     * Show a break reminder when watched too long
     */
    private fun setupBreakReminder() {
        val breakReminderPref = PreferenceHelper.getString(
            PreferenceKeys.BREAK_REMINDER,
            "disabled"
        )
        if (breakReminderPref == "disabled") return
        Handler(Looper.getMainLooper()).postDelayed(
            {
                try {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.share_with_time))
                        .setMessage(
                            getString(
                                R.string.already_spent_time,
                                breakReminderPref
                            )
                        )
                        .setPositiveButton(R.string.okay, null)
                        .show()
                } catch (e: Exception) {
                    kotlin.runCatching {
                        Toast.makeText(this, R.string.take_a_break, Toast.LENGTH_LONG).show()
                    }
                }
            },
            breakReminderPref.toLong() * 60 * 1000
        )
    }

    private fun removeSearchFocus() {
        searchView.setQuery("", false)
        searchView.clearFocus()
        searchView.onActionViewCollapsed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.action_bar, menu)

        // stuff for the search in the topBar
        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView

        val searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        searchView.setOnSearchClickListener {
            if (navController.currentDestination?.id != R.id.searchResultFragment) {
                searchViewModel.setQuery(null)
                navController.navigate(R.id.searchFragment)
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val bundle = Bundle()
                bundle.putString("query", query)
                navController.navigate(R.id.searchResultFragment, bundle)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (navController.currentDestination?.id != R.id.searchFragment) {
                    val bundle = Bundle()
                    bundle.putString("query", newText)
                    navController.navigate(R.id.searchFragment, bundle)
                } else {
                    searchViewModel.setQuery(newText)
                }
                return true
            }
        })

        searchItem.setOnActionExpandListener(
            object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                    return true
                }

                override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                    val currentFragmentId = navController.currentDestination?.id
                    if (currentFragmentId == R.id.searchFragment || currentFragmentId == R.id.searchResultFragment) {
                        onBackPressed()
                    }
                    return true
                }
            }
        )

        searchView.setOnCloseListener {
            if (navController.currentDestination?.id == R.id.searchFragment) {
                searchViewModel.setQuery(null)
                onBackPressed()
            }
            false
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
                true
            }
            R.id.action_about -> {
                val aboutIntent = Intent(this, AboutActivity::class.java)
                startActivity(aboutIntent)
                true
            }
            R.id.action_community -> {
                val communityIntent = Intent(this, CommunityActivity::class.java)
                startActivity(communityIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        val intentData: Uri? = intent?.data
        // check whether an URI got submitted over the intent data
        if (intentData != null && intentData.host != null && intentData.path != null) {
            Log.d(TAG(), "intentData: ${intentData.host} ${intentData.path} ")
            // load the URI of the submitted link (e.g. video)
            loadIntentData(intentData)
        }
    }

    private fun loadIntentData(data: Uri) {
        if (data.path!!.contains("/channel/")
        ) {
            val channelId = data.path!!
                .replace("/channel/", "")

            loadChannel(channelId = channelId)
        } else if (
            data.path!!.contains("/c/") ||
            data.path!!.contains("/user/")
        ) {
            val channelName = data.path!!
                .replace("/c/", "")
                .replace("/user/", "")

            loadChannel(channelName = channelName)
        } else if (
            data.path!!.contains("/playlist")
        ) {
            var playlistId = data.query!!
            if (playlistId.contains("&")) {
                for (v in playlistId.split("&")) {
                    if (v.contains("list=")) {
                        playlistId = v.replace("list=", "")
                        break
                    }
                }
            } else {
                playlistId = playlistId.replace("list=", "")
            }

            loadPlaylist(playlistId)
        } else if (
            data.path!!.contains("/shorts/") ||
            data.path!!.contains("/embed/") ||
            data.path!!.contains("/v/")
        ) {
            val videoId = data.path!!
                .replace("/shorts/", "")
                .replace("/v/", "")
                .replace("/embed/", "")

            loadVideo(videoId, data.query)
        } else if (data.path!!.contains("/watch") && data.query != null) {
            var videoId = data.query!!

            if (videoId.contains("&")) {
                val watches = videoId.split("&")
                for (v in watches) {
                    if (v.contains("v=")) {
                        videoId = v.replace("v=", "")
                        break
                    }
                }
            } else {
                videoId = videoId
                    .replace("v=", "")
            }

            loadVideo(videoId, data.query)
        } else {
            val videoId = data.path!!.replace("/", "")

            loadVideo(videoId, data.query)
        }
    }

    private fun loadVideo(videoId: String, query: String?) {
        Log.i(TAG(), "URI type: Video")

        val bundle = Bundle()
        Log.e(TAG(), videoId)

        // for time stamped links
        if (query != null && query.contains("t=")) {
            val timeStamp = query.toString().split("t=")[1]
            bundle.putLong("timeStamp", timeStamp.toLong())
        }

        bundle.putString("videoId", videoId)
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

    private fun loadChannel(
        channelId: String? = null,
        channelName: String? = null
    ) {
        Log.i(TAG(), "Uri Type: Channel")

        val bundle = if (channelId != null) bundleOf("channel_id" to channelId)
        else bundleOf("channel_name" to channelName)
        navController.navigate(R.id.channelFragment, bundle)
    }

    private fun loadPlaylist(playlistId: String) {
        Log.i(TAG(), "Uri Type: Playlist")

        val bundle = bundleOf("playlist_id" to playlistId)
        navController.navigate(R.id.playlistFragment, bundle)
    }

    override fun onBackPressed() {
        // remove focus from search
        removeSearchFocus()
        navController.popBackStack(R.id.searchFragment, false)

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
        val playerViewModel = ViewModelProvider(this)[PlayerViewModel::class.java]
        playerViewModel.isFullscreen.value = false
        requestedOrientation = if (autoRotationEnabled) ActivityInfo.SCREEN_ORIENTATION_USER
        else ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
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

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

    private fun unsetFullscreen() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

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
