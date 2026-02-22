package com.github.libretube.ui.activities

import android.content.Intent
import android.os.Bundle
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.ActivityNointernetBinding
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.extensions.onSystemInsets

class NoInternetActivity : AbstractPlayerHostActivity() {
    private lateinit var binding: ActivityNointernetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNointernetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // add padding to fragment containers to prevent overlap with edge-to-edge status bars
        binding.root.onSystemInsets { _, systemBarInsets ->
            with (binding.fragment) {
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

    // all these actions are no-ops for now because we don't have a navigation bar here
    override fun minimizePlayerContainerLayout() {}

    override fun maximizePlayerContainerLayout() {}

    override fun setPlayerContainerProgress(progress: Float) {}

    override fun clearSearchViewFocus(): Boolean = true
}
