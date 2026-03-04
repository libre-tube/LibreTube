package com.github.libretube.ui.activities

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
}