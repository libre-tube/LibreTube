package com.github.libretube.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.appcompat.widget.SearchView
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.allViews
import androidx.core.view.children
import androidx.core.view.isNotEmpty
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.BuildConfig
import com.github.libretube.NavDirections
import com.github.libretube.R
import com.github.libretube.compat.PictureInPictureCompat
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.ActivityMainBinding
import com.github.libretube.enums.ImportFormat
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.ImportHelper
import com.github.libretube.helpers.IntentHelper
import com.github.libretube.helpers.NavBarHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.NetworkHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ThemeHelper
import com.github.libretube.helpers.WindowHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.dialogs.ErrorDialog
import com.github.libretube.ui.dialogs.ImportTempPlaylistDialog
import com.github.libretube.ui.fragments.AudioPlayerFragment
import com.github.libretube.ui.fragments.DownloadsFragment
import com.github.libretube.ui.fragments.PlayerFragment
import com.github.libretube.ui.models.SearchViewModel
import com.github.libretube.ui.models.SubscriptionsViewModel
import com.github.libretube.ui.preferences.BackupRestoreSettings.Companion.FILETYPE_ANY
import com.github.libretube.util.UpdateChecker
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class MainActivity : BaseActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var navController: NavController
    lateinit var searchView: SearchView
    private lateinit var searchItem: MenuItem

    private var startFragmentId = R.id.homeFragment

    private val searchViewModel: SearchViewModel by viewModels()
    private val subscriptionsViewModel: SubscriptionsViewModel by viewModels()

    private var savedSearchQuery: String? = null
    private var shouldOpenSuggestions = true

    // registering for activity results is only possible, this here should have been part of
    // PlaylistOptionsBottomSheet instead if Android allowed us to
    private var playlistExportFormat: ImportFormat = ImportFormat.NEWPIPE
    private var exportPlaylistId: String? = null
    private val createPlaylistsFile = registerForActivityResult(
        ActivityResultContracts.CreateDocument(FILETYPE_ANY)
    ) { uri ->
        if (uri == null) return@registerForActivityResult

        lifecycleScope.launch(Dispatchers.IO) {
            ImportHelper.exportPlaylists(
                this@MainActivity,
                uri,
                playlistExportFormat,
                selectedPlaylistIds = listOf(exportPlaylistId!!)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // enable auto rotation if turned on
        requestOrientationChange()

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

        // Check update automatically
        if (PreferenceHelper.getBoolean(PreferenceKeys.AUTOMATIC_UPDATE_CHECKS, false)) {
            lifecycleScope.launch(Dispatchers.IO) {
                UpdateChecker(this@MainActivity).checkUpdate(false)
            }
        }

        // set the action bar for the activity
        setSupportActionBar(binding.toolbar)

        val navHostFragment = binding.fragment.getFragment<NavHostFragment>()
        navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        // save start tab fragment id and apply navbar style
        startFragmentId = try {
            NavBarHelper.applyNavBarStyle(binding.bottomNav)
        } catch (e: Exception) {
            R.id.homeFragment
        }

        // sets the color if the navigation bar is visible
        val bottomNavColor = getBottomNavColor()
        ThemeHelper.setSystemBarColors(this, window, bottomNavColor)

        // set default tab as start fragment
        navController.graph = navController.navInflater.inflate(R.navigation.nav).also {
            it.setStartDestination(startFragmentId)
        }

        binding.bottomNav.setOnApplyWindowInsetsListener(null)

        // Prevent duplicate entries into backstack, if selected item and current
        // visible fragment is different, then navigate to selected item.
        binding.bottomNav.setOnItemReselectedListener {
            if (it.itemId != navController.currentDestination?.id) {
                navigateToBottomSelectedItem(it)
            } else {
                // get the current fragment
                val fragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                tryScrollToTop(fragment?.requireView())
            }
        }

        binding.bottomNav.setOnItemSelectedListener {
            navigateToBottomSelectedItem(it)
        }

        if (binding.bottomNav.menu.children.none { it.itemId == startFragmentId }) deselectBottomBarItems()

        binding.toolbar.title = ThemeHelper.getStyledAppName(this)

        // handle error logs
        PreferenceHelper.getErrorLog().ifBlank { null }?.let {
            if (!BuildConfig.DEBUG)
                ErrorDialog().show(supportFragmentManager, null)
        }

        setupSubscriptionsBadge()

        loadIntentData()
    }

    @ColorInt
    fun getBottomNavColor(): Int? {
        return if (binding.bottomNav.menu.size() == 0) {
            null
        } else if (PreferenceHelper.getBoolean(PreferenceKeys.PURE_THEME, false)) {
            SurfaceColors.getColorForElevation(this, binding.bottomNav.elevation)
        } else {
            ThemeHelper.getThemeColor(
                this,
                com.google.android.material.R.attr.colorSurfaceContainer
            )
        }
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
    private fun tryScrollToTop(view: View?) {
        val scrollView = view?.allViews
            ?.firstOrNull { it is ScrollView || it is NestedScrollView || it is RecyclerView }
        when (scrollView) {
            is ScrollView -> scrollView.smoothScrollTo(0, 0)
            is NestedScrollView -> scrollView.smoothScrollTo(0, 0)
            is RecyclerView -> scrollView.smoothScrollToPosition(0)
        }
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

        subscriptionsViewModel.fetchSubscriptions(this)

        subscriptionsViewModel.videoFeed.observe(this) { feed ->
            val lastSeenVideoIndex = feed.orEmpty()
                .indexOfFirst { PreferenceHelper.getLastSeenVideoId() == it.url?.toID() }
            if (lastSeenVideoIndex < 1) return@observe
            binding.bottomNav.getOrCreateBadge(R.id.subscriptionsFragment).apply {
                number = lastSeenVideoIndex
                backgroundColor = ThemeHelper.getThemeColor(
                    this@MainActivity,
                    androidx.appcompat.R.attr.colorPrimary
                )
                badgeTextColor = ThemeHelper.getThemeColor(
                    this@MainActivity,
                    com.google.android.material.R.attr.colorOnPrimary
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
        if (!this::navController.isInitialized) return false
        val id = navController.currentDestination?.id ?: return false

        return id in listOf(
            R.id.searchFragment,
            R.id.searchResultFragment,
            R.id.channelFragment,
            R.id.playlistFragment
        )
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

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchView.clearFocus()

                // handle inserted YouTube-like URLs and directly open the referenced
                // channel, playlist or video instead of showing search results
                if (query.toHttpUrlOrNull() != null) {
                    val queryIntent = IntentHelper.resolveType(query.toUri())

                    val didNavigate = navigateToMediaByIntent(queryIntent) {
                        navController.popBackStack(R.id.searchFragment, true)
                        searchItem.collapseActionView()
                    }
                    if (didNavigate) return true
                }

                navController.navigate(NavDirections.showSearchResults(query))

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!shouldOpenSuggestions) return true

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
                    R.id.playlistFragment
                )
                if (navController.currentDestination?.id in destIds && newText == null) {
                    return false
                }

                if (navController.currentDestination?.id != R.id.searchFragment) {
                    navController.navigate(
                        R.id.searchFragment,
                        bundleOf(IntentData.query to newText)
                    )
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
                // Handover back press to `BackPressedDispatcher`
                if (binding.bottomNav.menu.children.none {
                        it.itemId == navController.currentDestination?.id
                    }
                ) {
                    this@MainActivity.onBackPressedDispatcher.onBackPressed()
                }

                // Suppress collapsing of search when search in progress.
                return !isSearchInProgress()
            }
        })

        // handle search queries passed by the intent
        if (savedSearchQuery != null) {
            searchItem.expandActionView()
            searchView.setQuery(savedSearchQuery, true)
            savedSearchQuery = null
        }

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Update the query text in the search bar without opening the search suggestions
     */
    fun setQuerySilent(query: String) {
        if (!this::searchView.isInitialized) return

        shouldOpenSuggestions = false
        searchView.setQuery(query, false)
        shouldOpenSuggestions = true
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

            R.id.action_donate -> {
                IntentHelper.openLinkFromHref(
                    this,
                    supportFragmentManager,
                    AboutActivity.DONATE_URL,
                    forceDefaultOpen = true
                )
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
            val offlinePlayer = intent!!.getBooleanExtra(IntentData.offlinePlayer, false)
            NavigationHelper.openAudioPlayerFragment(this, offlinePlayer = offlinePlayer)
            return
        }

        // navigate to (temporary) playlist or channel if available
        if (navigateToMediaByIntent(intent)) return

        intent?.getStringExtra(IntentData.query)?.let {
            savedSearchQuery = it
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

    /**
     * Navigates to the channel, video or playlist provided in the [Intent] if available
     *
     * @return Whether the method handled the event and triggered the navigation to a new fragment
     */
    fun navigateToMediaByIntent(intent: Intent, actionBefore: () -> Unit = {}): Boolean {
        intent.getStringExtra(IntentData.channelId)?.let {
            actionBefore()
            navController.navigate(NavDirections.openChannel(channelId = it))
            return true
        }
        intent.getStringExtra(IntentData.channelName)?.let {
            actionBefore()
            navController.navigate(NavDirections.openChannel(channelName = it))
            return true
        }
        intent.getStringExtra(IntentData.playlistId)?.let {
            actionBefore()
            navController.navigate(NavDirections.openPlaylist(playlistId = it))
            return true
        }
        intent.getStringArrayExtra(IntentData.videoIds)?.let {
            actionBefore()
            ImportTempPlaylistDialog()
                .apply {
                    arguments = bundleOf(
                        IntentData.playlistName to intent.getStringExtra(IntentData.playlistName),
                        IntentData.videoIds to it
                    )
                }
                .show(supportFragmentManager, null)
            return true
        }

        intent.getStringExtra(IntentData.videoId)?.let {
            // the below explained work around only seems to work on Android 11 and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && binding.bottomNav.menu.isNotEmpty()) {
                // the bottom navigation bar has to be created before opening the video
                // otherwise the player layout measures aren't calculated properly
                // and the miniplayer is opened at a closed state and overlapping the navigation bar
                binding.bottomNav.viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        NavigationHelper.navigateVideo(
                            context = this@MainActivity,
                            videoUrlOrId = it,
                            timestamp = intent.getLongExtra(IntentData.timeStamp, 0L)
                        )

                        binding.bottomNav.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            } else {
                NavigationHelper.navigateVideo(
                    context = this@MainActivity,
                    videoUrlOrId = it,
                    timestamp = intent.getLongExtra(IntentData.timeStamp, 0L)
                )
            }

            return true
        }

        return false
    }

    @SuppressLint("SwitchIntDef")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> WindowHelper.toggleFullscreen(window, false)
            Configuration.ORIENTATION_LANDSCAPE -> WindowHelper.toggleFullscreen(window, true)
        }
    }

    private fun navigateToBottomSelectedItem(item: MenuItem): Boolean {
        if (item.itemId == R.id.subscriptionsFragment) {
            binding.bottomNav.removeBadge(R.id.subscriptionsFragment)
        }

        // Remove focus from search view when navigating to bottom view.
        // Call only after navigate to destination, so it can be used in
        // onMenuItemActionCollapse for backstack management
        removeSearchFocus()

        return item.onNavDestinationSelected(navController)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        runOnPlayerFragment {
            onUserLeaveHint()
            true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        loadIntentData()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (runOnPlayerFragment { onKeyUp(keyCode) }) {
            return true
        }

        return super.onKeyUp(keyCode, event)
    }

    /**
     * Attempt to run code on the player fragment if running
     * Returns true if a running player fragment was found and the action got consumed, else false
     */
    fun runOnPlayerFragment(action: PlayerFragment.() -> Boolean): Boolean {
        return supportFragmentManager.fragments.filterIsInstance<PlayerFragment>()
            .firstOrNull()
            ?.let(action)
            ?: false
    }

    fun runOnAudioPlayerFragment(action: AudioPlayerFragment.() -> Boolean): Boolean {
        return supportFragmentManager.fragments.filterIsInstance<AudioPlayerFragment>()
            .firstOrNull()
            ?.let(action)
            ?: false
    }

    fun startPlaylistExport(playlistId: String, playlistName: String, format: ImportFormat) {
        playlistExportFormat = format
        exportPlaylistId = playlistId
        createPlaylistsFile.launch("${playlistName}.${format.fileExtension}")
    }
}
