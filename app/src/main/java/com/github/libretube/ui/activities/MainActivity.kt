package com.github.libretube.ui.activities

import android.annotation.SuppressLint
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
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
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
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.ActivityMainBinding
import com.github.libretube.extensions.toID
import com.github.libretube.services.ClosingService
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.dialogs.ErrorDialog
import com.github.libretube.ui.fragments.PlayerFragment
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.ui.models.SearchViewModel
import com.github.libretube.ui.models.SubscriptionsViewModel
import com.github.libretube.ui.sheets.PlayingQueueSheet
import com.github.libretube.ui.tools.BreakReminder
import com.github.libretube.util.NavBarHelper
import com.github.libretube.util.NetworkHelper
import com.github.libretube.util.PlayingQueue
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.util.ThemeHelper
import com.google.android.material.elevation.SurfaceColors

class MainActivity : BaseActivity() {

    lateinit var binding: ActivityMainBinding

    lateinit var navController: NavController
    private var startFragmentId = R.id.homeFragment

    val autoRotationEnabled = PreferenceHelper.getBoolean(PreferenceKeys.AUTO_ROTATION, false)

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
            SurfaceColors.getColorForElevation(this, 10F)
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
                val fragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
                tryScrollToTop(fragment?.requireView() as? ViewGroup)
            }
        }

        binding.bottomNav.setOnItemSelectedListener {
            navigateToBottomSelectedItem(it)
            false
        }

        binding.toolbar.title = ThemeHelper.getStyledAppName(this)

        /**
         * handle error logs
         */
        val log = PreferenceHelper.getErrorLog()
        if (log != "") ErrorDialog().show(supportFragmentManager, null)

        BreakReminder.setupBreakReminder(applicationContext)

        setupSubscriptionsBadge()

        val playerViewModel = ViewModelProvider(this)[PlayerViewModel::class.java]

        // new way of handling back presses
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (playerViewModel.isFullscreen.value == true) {
                    for (fragment in supportFragmentManager.fragments) {
                        if (fragment is PlayerFragment) {
                            fragment.unsetFullscreen()
                            return
                        }
                    }
                }

                if (binding.mainMotionLayout.progress == 0F) {
                    try {
                        minimizePlayer()
                        return
                    } catch (e: Exception) {
                        // current fragment isn't the player fragment
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
        })

        loadIntentData()
    }

    /**
     * Try to find a scroll or recycler view and scroll it back to the top
     */
    private fun tryScrollToTop(viewGroup: ViewGroup?) {
        (viewGroup as? ScrollView)?.scrollTo(0, 0)

        if (viewGroup == null || viewGroup.childCount == 0) return

        viewGroup.children.forEach {
            (it as? ScrollView)?.let {
                it.smoothScrollTo(0, 0)
                return
            }
            (it as? NestedScrollView)?.let {
                it.smoothScrollTo(0, 0)
                return
            }
            (it as? RecyclerView)?.let {
                it.smoothScrollToPosition(0)
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

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_queue)?.isVisible = PlayingQueue.isNotEmpty()
        return super.onPrepareOptionsMenu(menu)
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
            binding.bottomNav.getOrCreateBadge(R.id.subscriptionsFragment).apply {
                number = lastSeenVideoIndex
                backgroundColor = ThemeHelper.getThemeColor(this@MainActivity, R.attr.colorPrimary)
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
                val bundle = Bundle()
                bundle.putString("query", query)
                navController.navigate(R.id.searchResultFragment, bundle)
                searchViewModel.setQuery("")
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
                if (navController.currentDestination?.id in listOf(
                        R.id.searchResultFragment,
                        R.id.channelFragment,
                        R.id.playlistFragment
                    ) &&
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

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                if (navController.currentDestination?.id != R.id.searchResultFragment) {
                    searchViewModel.setQuery(null)
                    navController.navigate(R.id.searchFragment)
                }
                item.setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW
                )
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                if (binding.mainMotionLayout.progress == 0F) {
                    try {
                        minimizePlayer()
                    } catch (e: Exception) {
                        // current fragment isn't the player fragment
                    }
                }
                // Handover back press to `BackPressedDispatcher`
                else if (binding.bottomNav.menu.children.none {
                        it.itemId == navController.currentDestination?.id
                    }
                ) {
                    this@MainActivity.onBackPressedDispatcher.onBackPressed()
                }

                return true
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
            R.id.action_community -> {
                val communityIntent = Intent(this, CommunityActivity::class.java)
                startActivity(communityIntent)
                true
            }
            R.id.action_queue -> {
                PlayingQueueSheet().show(supportFragmentManager, null)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadIntentData() {
        // If activity is running in PiP mode, then start it in front.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            isInPictureInPictureMode
        ) {
            moveTaskToBack(true)
            intent?.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }

        intent?.getStringExtra(IntentData.channelId)?.let {
            navController.navigate(
                R.id.channelFragment,
                bundleOf(IntentData.channelName to it)
            )
        }
        intent?.getStringExtra(IntentData.channelName)?.let {
            navController.navigate(
                R.id.channelFragment,
                bundleOf(IntentData.channelName to it)
            )
        }
        intent?.getStringExtra(IntentData.playlistId)?.let {
            navController.navigate(
                R.id.playlistFragment,
                bundleOf(IntentData.playlistId to it)
            )
        }
        intent?.getStringExtra(IntentData.videoId)?.let {
            loadVideo(it, intent?.getLongExtra(IntentData.timeStamp, 0L))
        }
        when (intent?.getStringExtra("fragmentToOpen")) {
            "home" ->
                navController.navigate(R.id.homeFragment)
            "trends" ->
                navController.navigate(R.id.trendsFragment)
            "subscriptions" ->
                navController.navigate(R.id.subscriptionsFragment)
            "library" ->
                navController.navigate(R.id.libraryFragment)
        }
        if (intent?.getBooleanExtra(IntentData.openQueueOnce, false) == true) {
            PlayingQueueSheet()
                .show(supportFragmentManager)
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
            supportFragmentManager.fragments.forEach { fragment ->
                (fragment as? PlayerFragment)
                    ?.binding?.playerMotionLayout?.apply {
                        transitionToEnd()
                        transitionToStart()
                    }
            }
        }, 300)
    }

    private fun minimizePlayer() {
        binding.mainMotionLayout.transitionToEnd()
        supportFragmentManager.fragments.forEach { fragment ->
            (fragment as? PlayerFragment)?.binding?.apply {
                mainContainer.isClickable = false
                linLayout.visibility = View.VISIBLE
            }
        }
        supportFragmentManager.fragments.forEach { fragment ->
            (fragment as? PlayerFragment)?.binding?.playerMotionLayout?.apply {
                // set the animation duration
                setTransitionDuration(250)
                transitionToEnd()
                getConstraintSet(R.id.start).constrainHeight(R.id.player, 0)
                enableTransition(R.id.yt_transition, true)
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
            Configuration.ORIENTATION_PORTRAIT -> unsetFullscreen()
            Configuration.ORIENTATION_LANDSCAPE -> setFullscreen()
        }
    }

    fun setFullscreen() {
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

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
