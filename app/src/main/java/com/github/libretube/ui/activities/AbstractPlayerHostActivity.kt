package com.github.libretube.ui.activities

import android.content.Intent
import android.view.MenuItem
import com.github.libretube.R
import com.github.libretube.helpers.IntentHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.fragments.AudioPlayerFragment
import com.github.libretube.ui.fragments.PlayerFragment

abstract class AbstractPlayerHostActivity: BaseActivity() {
    abstract fun minimizePlayerContainerLayout()
    abstract fun maximizePlayerContainerLayout()
    abstract fun setPlayerContainerProgress(progress: Float)

    abstract fun clearSearchViewFocus(): Boolean


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
}