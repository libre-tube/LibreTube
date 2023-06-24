package com.github.libretube.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.WindowInsetsCompat
import com.github.libretube.ui.extensions.toggleSystemBars

class OfflinePlayerView(
    context: Context,
    attributeSet: AttributeSet? = null
) : CustomExoPlayerView(context, attributeSet) {
    override fun hideController() {
        super.hideController()
        // hide the status bars when continuing to watch video
        activity.toggleSystemBars(WindowInsetsCompat.Type.systemBars(), false)
    }

    override fun showController() {
        super.showController()
        // show status bar when showing player options
        activity.toggleSystemBars(WindowInsetsCompat.Type.statusBars(), true)
    }

    override fun getTopBarMarginDp(): Int {
        // the offline player requires a bigger top bar margin
        return if (isFullscreen()) 18 else super.getTopBarMarginDp()
    }
}
