package com.github.libretube.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.ActivityMainBinding
import com.github.libretube.dialogs.ErrorDialog
import com.github.libretube.extensions.BaseActivity
import com.github.libretube.extensions.toID
import com.github.libretube.fragments.PlayerFragment
import com.github.libretube.models.PlayerViewModel
import com.github.libretube.models.SearchViewModel
import com.github.libretube.models.SubscriptionsViewModel
import com.github.libretube.services.ClosingService
import com.github.libretube.util.NetworkHelper
import com.github.libretube.util.PreferenceHelper
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
        super.onCreate(savedInstanceState)

        autoRotationEnabled = PreferenceHelper.getBoolean(PreferenceKeys.AUTO_ROTATION, false)

        // enable auto rotation if turned on
        requestedOrientation = if (autoRotationEnabled) {
            ActivityInfo.SCREEN_ORIENTATION_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }

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
        if (hideTrendingPage) {
            binding.bottomNav.menu.findItem(R.id.homeFragment).isVisible =
                false
        }

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
                    binding.bottomNav.removeBadge(R.id.subscriptionsFragment)
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

        setupSubscriptionsBadge()

        // new way of handling back presses
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navController.popBackStack(R.id.searchFragment, false)

                if (binding.mainMotionLayout.progress == 0F) {
                    try {
                        minimizePlayer()
                        return
                    } catch (e: Exception) {
                        // current fragment isn't the player fragment
                    }
                }

                if (navController.currentDestination?.id == startFragmentId) {
                    moveTaskToBack(true)
                } else {
                    navController.popBackStack()
                }
            }
        })
    }

    /**
     * Show a break reminder when watched too long
     */
    private fun setupBreakReminder() {
        if (!PreferenceHelper.getBoolean(
                PreferenceKeys.BREAK_REMINDER_TOGGLE,
                false
            )
        ) {
            return
        }
        val breakReminderPref = PreferenceHelper.getString(
            PreferenceKeys.BREAK_REMINDER,
            "0"
        )
        if (!breakReminderPref.all { Character.isDigit(it) } ||
            breakReminderPref == "" || breakReminderPref == "0"
        ) {
            return
        }
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

    /**
     * Initialize the notification badge showing the amount of new videos
     */
    private fun setupSubscriptionsBadge() {
        if (!PreferenceHelper.getBoolean(
                PreferenceKeys.NEW_VIDEOS_BADGE,
                false
            )
        ) {
            return
        }

        val subscriptionsViewModel = ViewModelProvider(this)[SubscriptionsViewModel::class.java]
        subscriptionsViewModel.fetchSubscriptions()

        subscriptionsViewModel.videoFeed.observe(this) {
            val lastSeenVideoId = PreferenceHelper.getLastSeenVideoId()
            val lastSeenVideoIndex = subscriptionsViewModel.videoFeed.value?.indexOfFirst {
                lastSeenVideoId == it.url?.toID()
            } ?: return@observe
            if (lastSeenVideoIndex < 1) return@observe
            binding.bottomNav.getOrCreateBadge(R.id.subscriptionsFragment).number = lastSeenVideoIndex
        }
    }

    /**
     * Remove the focus of the search view in the toolbar
     */
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
                searchViewModel.setQuery("")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // prevent malicious navigation when the search view is getting collapsed
                if (navController.currentDestination?.id == R.id.searchResultFragment &&
                    (newText == null || newText == "")
                ) {
                    return false
                }

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
                override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                    return true
                }

                override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                    val currentFragmentId = navController.currentDestination?.id
                    if (currentFragmentId == R.id.searchFragment || currentFragmentId == R.id.searchResultFragment) {
                        navController.popBackStack()
                    }
                    return true
                }
            }
        )
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
        // check whether an URI got submitted over the intent data and load it
        when {
            intent?.getStringExtra(IntentData.channelId) != null -> navController.navigate(
                R.id.channelFragment,
                bundleOf(
                    IntentData.channelName to intent?.getStringExtra(IntentData.channelId)!!
                )
            )
            intent?.getStringExtra(IntentData.channelName) != null -> navController.navigate(
                R.id.channelFragment,
                bundleOf(
                    IntentData.channelName to intent?.getStringExtra(IntentData.channelName)
                )
            )
            intent?.getStringExtra(IntentData.playlistId) != null -> navController.navigate(
                R.id.playlistFragment,
                bundleOf(
                    IntentData.playlistId to intent?.getStringExtra(IntentData.playlistId)!!
                )
            )
            intent?.getStringExtra(IntentData.videoId) != null -> loadVideo(
                videoId = intent?.getStringExtra(IntentData.videoId)!!,
                timeStamp = intent?.getLongExtra(IntentData.timeStamp, 0L)
            )
        }
    }

    private fun loadVideo(videoId: String, timeStamp: Long?) {
        val bundle = Bundle()

        bundle.putString(IntentData.videoId, videoId)
        if (timeStamp != null) bundle.putLong(IntentData.timeStamp, timeStamp)

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
        requestedOrientation = if (autoRotationEnabled) {
            ActivityInfo.SCREEN_ORIENTATION_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
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
        } else {
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

        showSystemBars()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.apply {
                show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                }
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_VISIBLE or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    /**
     * hide the status bar
     */
    fun hideSystemBars() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    /**
     * show the status bar
     */
    private fun showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        supportFragmentManager.fragments.forEach { fragment ->
            (fragment as? PlayerFragment)?.onUserLeaveHint()
        }
    }
}
