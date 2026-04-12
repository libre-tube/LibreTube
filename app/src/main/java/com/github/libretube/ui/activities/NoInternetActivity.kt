package com.github.libretube.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.navigation.findNavController
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.ActivityNointernetBinding
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.extensions.onSystemInsets
import com.github.libretube.ui.models.DownloadsViewModel

class NoInternetActivity : AbstractPlayerHostActivity() {
    private lateinit var binding: ActivityNointernetBinding

    private val downloadsModel: DownloadsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNointernetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // add padding to fragment containers to prevent overlap with edge-to-edge status bars
        binding.root.onSystemInsets { _, systemBarInsets ->
            with (binding.mainLayout) {
                setPadding(paddingLeft, systemBarInsets.top, paddingRight, systemBarInsets.bottom)
            }
            with (binding.container) {
                setPadding(paddingLeft, paddingTop, paddingRight, systemBarInsets.bottom)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.getBooleanExtra(IntentData.maximizePlayer, false)) {
            NavigationHelper.openAudioPlayerFragment(this, offlinePlayer = true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.action_bar, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.isIconified = true

        searchItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                // automatically navigate to the downloads fragment when the user clicks the search bar
                val navController = binding.fragment.findNavController()
                if (navController.currentDestination?.id != R.id.downloadsFragment) {
                    navController.navigate(R.id.downloadsFragment)
                }
                return true
            }
        })

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String?): Boolean {
                downloadsModel.setQuery(query)
                return true
            }

            override fun onQueryTextSubmit(p0: String?): Boolean {
                searchView.clearFocus()
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    // all these actions are no-ops for now because we don't have a navigation bar here
    override fun minimizePlayerContainerLayout() {}

    override fun maximizePlayerContainerLayout() {}

    override fun setPlayerContainerProgress(progress: Float) {}

    override fun clearSearchViewFocus(): Boolean = true
}
