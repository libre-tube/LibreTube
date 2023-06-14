package com.github.libretube.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.compat.PictureInPictureCompat
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.ActivityMainBinding
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.NavBarHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.NetworkHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ThemeHelper
import com.github.libretube.helpers.WindowHelper
import com.github.libretube.services.ClosingService
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.dialogs.ErrorDialog
import com.github.libretube.ui.fragments.AudioPlayerFragment
import com.github.libretube.ui.fragments.DownloadsFragment
import com.github.libretube.ui.fragments.PlayerFragment
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.ui.models.SearchViewModel
import com.github.libretube.ui.models.SubscriptionsViewModel
import com.github.libretube.ui.tools.SleepTimer
import com.google.android.material.elevation.SurfaceColors

class MainActivity : BaseActivity() {

    lateinit var binding: ActivityMainBinding

    lateinit var navController: NavController
    private var startFragmentId = R.id.homeFragment

    val autoRotationEnabled: Boolean by lazy {
        PreferenceHelper.getBoolean(
            PreferenceKeys.AUTO_ROTATION,
            resources.getBoolean(R.bool.config_default_auto_rotation_pref)
        )
    }

    lateinit var searchView: SearchView
    private lateinit var searchItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // enable auto rotation if turned on
        requestOrientationChange()

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
        } else if (PreferenceHelper.getString(PreferenceKeys.FETCH_INSTANCE, "").isEmpty()) {
            val welcomeIntent = Intent(this, WelcomeActivity::class.java)
            startActivity(welcomeIntent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // set the action bar for the activity
        setSupportActionBar(binding.toolbar)

        navController = findNavController(R.id.fragment)
        binding.bottomNav.setupWithNavController(navController)

        // save start tab fragment id and apply navbar style
        startFragmentId = try {
            NavBarHelper.applyNavBarStyle(binding.bottomNav)
        } catch (e: Exception) {
            R.id.homeFragment
        }

        // sets the navigation bar color to the previously calculated color
        window.navigationBarColor = if (binding.bottomNav.menu.size() > 0) {
            SurfaceColors.getColorForElevation(this, binding.bottomNav.elevation)
        } else {
            ThemeHelper.getThemeColor(this, android.R.attr.colorBackground)
        }

        // set default tab as start fragment
        navController.graph.setStartDestination(startFragmentId)

        // navigate to the default fragment
        navController.navigate(startFragmentId)

        binding.bottomNav.setOnApplyWindowInsetsListener(null)

        // Prevent duplicate entries into backstack, if selected item and current
        // visible fragment is different, then navigate to selected item.
        binding.bottomNav.setOnItemReselectedListener {
            if (it.itemId != navController.currentDestination?.id) {
                navigateToBottomSelectedItem(it)
            } else {
                // get the host fragment containing the current fragment
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment?
                // get the current fragment
                val fragment = navHostFragment?.childFragmentManager?.fragments?.getOrNull(0)
                tryScrollToTop(fragment?.requireView() as? ViewGroup)
            }
        }

        binding.bottomNav.setOnItemSelectedListener {
            navigateToBottomSelectedItem(it)
            false
        }

        if (binding.bottomNav.menu.children.none { it.itemId == startFragmentId }) deselectBottomBarItems()

        binding.toolbar.title = ThemeHelper.getStyledAppName(this)

        // handle error logs
        PreferenceHelper.getErrorLog().ifBlank { null }?.let {
            ErrorDialog().show(supportFragmentManager, null)
        }

        SleepTimer.setup(this)

        setupSubscriptionsBadge()

        val playerViewModel = ViewModelProvider(this)[PlayerViewModel::class.java]

        // new way of handling back presses
        onBackPressedDispatcher.addCallback {
            if (playerViewModel.isFullscreen.value == true) {
                supportFragmentManager.fragments.filterIsInstance<PlayerFragment>()
                    .firstOrNull()
                    ?.let {
                        it.unsetFullscreen()
                        return@addCallback
                    }
            }

            if (binding.mainMotionLayout.progress == 0F) {
                runCatching {
                    minimizePlayer()
                    return@addCallback
                }
            }

            when (navController.currentDestination?.id) {
                startFragmentId -> {
                    moveTaskToBack(true)
                }
                R.id.searchResultFragment -> {
                    navController.popBackStack(R.id.searchFragment, true) ||
                        navController.popBackStack()
                }
                else -> {
                    navController.popBackStack()
                }
            }
        }

        loadIntentData()
    }

    /**
     * Deselect all bottom bar items
     */
    private fun deselectBottomBarItems() {
        binding.bottomNav.menu.setGroupCheckable(0, true, false)
        for (child in binding.bottomNav.menu.children) {
            child.isChecked = false
        }
        binding.bottomNav.menu.setGroupCheckable(0, true, true)
    }

    /**
     * Try to find a scroll or recycler view and scroll it back to the top
     */
    private fun tryScrollToTop(viewGroup: ViewGroup?) {
        (viewGroup as? ScrollView)?.scrollTo(0, 0)

        if (viewGroup == null || viewGroup.childCount == 0) return

        viewGroup.children.forEach {
            (it as? ScrollView)?.let { scrollView ->
                scrollView.smoothScrollTo(0, 0)
                return
            }
            (it as? NestedScrollView)?.let { scrollView ->
                scrollView.smoothScrollTo(0, 0)
                return
            }
            (it as? RecyclerView)?.let { recyclerView ->
                recyclerView.smoothScrollToPosition(0)
                return
            }
            tryScrollToTop(it as? ViewGroup)
        }
    }

    /**
     * Rotate according to the preference
     */
    fun requestOrientationChange() {
        requestedOrientation = if (autoRotationEnabled) {
            ActivityInfo.SCREEN_ORIENTATION_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }
    }

    /**
     * Initialize the notification badge showing the amount of new videos
     */
    private fun setupSubscriptionsBadge() {
        if (!PreferenceHelper.getBoolean(
                PreferenceKeys.NEW_VIDEOS_BADGE,
                false,
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
            binding.bottomNav.getOrCreateBadge(R.id.subscriptionsFragment).apply {
                number = lastSeenVideoIndex
                backgroundColor = ThemeHelper.getThemeColor(
                    this@MainActivity,
                    androidx.appcompat.R.attr.colorPrimary,
                )
                badgeTextColor = ThemeHelper.getThemeColor(
                    this@MainActivity,
                    com.google.android.material.R.attr.colorOnPrimary,
                )
            }
        }
    }

    /**
     * Remove the focus of the search view in the toolbar
     */
    private fun removeSearchFocus() {
        searchView.setQuery("", false)
        searchView.clearFocus()
        searchView.isIconified = true
        searchItem.collapseActionView()
        searchView.onActionViewCollapsed()
    }

    private fun isSearchInProgress(): Boolean {
        if (!::navController.isInitialized) return false
        val id = navController.currentDestination?.id ?: return false
        return id in listOf(R.id.searchFragment, R.id.searchResultFragment)
    }

    override fun invalidateMenu() {
        // Don't invalidate menu when in search in progress
        // this is a workaround as there is bug in android code
        // details of bug: https://issuetracker.google.com/issues/244336571
        if (isSearchInProgress()) {
            return
        }
        super.invalidateMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.action_bar, menu)

        // stuff for the search in the topBar
        val searchItem = menu.findItem(R.id.action_search)
        this.searchItem = searchItem
        searchView = searchItem.actionView as SearchView

        val searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                navController.navigate(R.id.searchResultFragment, bundleOf("query" to query))
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Prevent navigation when search view is collapsed
                if (searchView.isIconified ||
                    binding.bottomNav.menu.children.any {
                        it.itemId == navController.currentDestination?.id
                    }
                ) {
                    return true
                }

                // prevent malicious navigation when the search view is getting collapsed
                val destIds = listOf(
                    R.id.searchResultFragment,
                    R.id.channelFragment,
                    R.id.playlistFragment,
                )
                if (navController.currentDestination?.id in destIds && newText.isNullOrEmpty()) {
                    return false
                }

                if (navController.currentDestination?.id != R.id.searchFragment) {
                    navController.navigate(R.id.searchFragment, bundleOf("query" to newText))
                } else {
                    searchViewModel.setQuery(newText)
                }

                return true
            }
        })

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                if (navController.currentDestination?.id != R.id.searchResultFragment) {
                    searchViewModel.setQuery(null)
                    navController.navigate(R.id.searchFragment)
                }
                item.setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW,
                )
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                if (binding.mainMotionLayout.progress == 0F) {
                    runCatching {
                        minimizePlayer()
                    }
                }
                // Handover back press to `BackPressedDispatcher`
                else if (binding.bottomNav.menu.children.none {
                        it.itemId == navController.currentDestination?.id
                    }
                ) {
                    this@MainActivity.onBackPressedDispatcher.onBackPressed()
                }

                // Suppress collapsing of search when search in progress.
                return !isSearchInProgress()
            }
        })
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
            R.id.action_help -> {
                val helpIntent = Intent(this, HelpActivity::class.java)
                startActivity(helpIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadIntentData() {
        // If activity is running in PiP mode, then start it in front.
        if (PictureInPictureCompat.isInPictureInPictureMode(this)) {
            val nIntent = Intent(this, MainActivity::class.java)
            nIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(nIntent)
        }

        if (intent?.getBooleanExtra(IntentData.openAudioPlayer, false) == true) {
            NavigationHelper.startAudioPlayer(this)
            return
        }

        intent?.getStringExtra(IntentData.channelId)?.let {
            navController.navigate(
                R.id.channelFragment,
                bundleOf(IntentData.channelId to it),
            )
        }
        intent?.getStringExtra(IntentData.channelName)?.let {
            navController.navigate(
                R.id.channelFragment,
                bundleOf(IntentData.channelName to it),
            )
        }
        intent?.getStringExtra(IntentData.playlistId)?.let {
            navController.navigate(
                R.id.playlistFragment,
                bundleOf(IntentData.playlistId to it),
            )
        }
        intent?.getStringExtra(IntentData.videoId)?.let {
            NavigationHelper.navigateVideo(
                context = this,
                videoId = it,
                timeStamp = intent?.getLongExtra(IntentData.timeStamp, 0L),
            )
        }

        intent?.getStringExtra("fragmentToOpen")?.let {
            if (it != "downloads") { // Not a shortcut
                ShortcutManagerCompat.reportShortcutUsed(this, it)
            }

            when (it) {
                "home" -> navController.navigate(R.id.homeFragment)
                "trends" -> navController.navigate(R.id.trendsFragment)
                "subscriptions" -> navController.navigate(R.id.subscriptionsFragment)
                "library" -> navController.navigate(R.id.libraryFragment)
                "downloads" -> navController.navigate(R.id.downloadsFragment)
            }
        }
        if (intent?.getBooleanExtra(IntentData.downloading, false) == true) {
            (supportFragmentManager.fragments.find { it is NavHostFragment })
                ?.childFragmentManager?.fragments?.forEach { fragment ->
                    (fragment as? DownloadsFragment)?.bindDownloadService()
                }
        }
    }

    private fun minimizePlayer() {
        binding.mainMotionLayout.transitionToEnd()
        supportFragmentManager.fragments.forEach { fragment ->
            (fragment as? PlayerFragment)?.binding?.apply {
                mainContainer.isClickable = false
                linLayout.visibility = View.VISIBLE
                playerMotionLayout.setTransitionDuration(250)
                playerMotionLayout.transitionToEnd()
                playerMotionLayout.getConstraintSet(R.id.start).constrainHeight(R.id.player, 0)
                playerMotionLayout.enableTransition(R.id.yt_transition, true)
            }
            (fragment as? AudioPlayerFragment)?.binding?.apply {
                audioPlayerContainer.isClickable = false
                playerMotionLayout.transitionToEnd()
            }
        }

        val playerViewModel = ViewModelProvider(this)[PlayerViewModel::class.java]
        playerViewModel.isFullscreen.value = false
        requestedOrientation = if (autoRotationEnabled) {
            ActivityInfo.SCREEN_ORIENTATION_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> WindowHelper.toggleFullscreen(this, false)
            Configuration.ORIENTATION_LANDSCAPE -> WindowHelper.toggleFullscreen(this, true)
        }
    }

    private fun navigateToBottomSelectedItem(item: MenuItem) {
        // clear backstack if it's the start fragment
        if (startFragmentId == item.itemId) navController.backQueue.clear()

        if (item.itemId == R.id.subscriptionsFragment) {
            binding.bottomNav.removeBadge(R.id.subscriptionsFragment)
        }

        // navigate to the selected fragment, if the fragment already
        // exists in backstack then pop up to that entry
        if (!navController.popBackStack(item.itemId, false)) {
            navController.navigate(item.itemId)
        }

        // Remove focus from search view when navigating to bottom view.
        // Call only after navigate to destination, so it can be used in
        // onMenuItemActionCollapse for backstack management
        removeSearchFocus()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        supportFragmentManager.fragments.forEach { fragment ->
            (fragment as? PlayerFragment)?.onUserLeaveHint()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
        loadIntentData()
    }
}
