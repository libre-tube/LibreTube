package com.github.libretube.helpers

import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.db.obj.filterByTab
import com.github.libretube.ui.fragments.DownloadTab

internal fun List<DownloadWithItems>.initialDownloadTab(hasPlaylists: Boolean): DownloadTab = when {
    filterByTab(DownloadTab.VIDEO).isNotEmpty() -> DownloadTab.VIDEO
    filterByTab(DownloadTab.AUDIO).isNotEmpty() -> DownloadTab.AUDIO
    hasPlaylists -> DownloadTab.PLAYLIST
    else -> DownloadTab.VIDEO
}
