package com.github.libretube.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import com.github.libretube.api.obj.ChapterSegment

class OfflinePlayerView(
    context: Context,
    attributeSet: AttributeSet? = null
) : CustomExoPlayerView(context, attributeSet) {
    private var chapters: List<ChapterSegment> = emptyList()

    override fun hideController() {
        super.hideController()
        // hide the status bars when continuing to watch video
        toggleSystemBars(false)
    }

    override fun showController() {
        super.showController()
        // show status bar when showing player options
        toggleSystemBars(true)
    }

    override fun minimizeOrExitPlayer() {
        (context as AppCompatActivity).onBackPressedDispatcher.onBackPressed()
    }

    fun setChapters(chapters: List<ChapterSegment>) {
        this.chapters = chapters
        setCurrentChapterName()
    }
}
