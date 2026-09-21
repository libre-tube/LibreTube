package com.github.libretube.helpers

import com.github.libretube.db.obj.Download
import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.enums.FileType
import com.github.libretube.ui.fragments.DownloadTab
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.io.path.Path

class DownloadTabSelectionTest {
    @Test
    fun audioOnlyDownloadsSelectAudio() {
        assertEquals(DownloadTab.AUDIO, listOf(download(FileType.AUDIO)).initialDownloadTab(false))
    }

    @Test
    fun videoDownloadsKeepVideoSelected() {
        assertEquals(DownloadTab.VIDEO, listOf(download(FileType.AUDIO), download(FileType.VIDEO)).initialDownloadTab(true))
    }

    @Test
    fun playlistOnlyDownloadsSelectPlaylists() {
        assertEquals(DownloadTab.PLAYLIST, emptyList<DownloadWithItems>().initialDownloadTab(true))
    }

    @Test
    fun noDownloadsKeepsVideoSelected() {
        assertEquals(DownloadTab.VIDEO, emptyList<DownloadWithItems>().initialDownloadTab(false))
    }

    @Test
    fun unstartedDownloadsRemainVisibleInVideo() {
        assertEquals(DownloadTab.VIDEO, listOf(download()).initialDownloadTab(false))
    }

    @Test
    fun audioAndVideoForSameDownloadBelongToVideo() {
        assertEquals(DownloadTab.VIDEO, listOf(download(FileType.AUDIO, FileType.VIDEO)).initialDownloadTab(false))
    }

    private fun download(vararg types: FileType) = DownloadWithItems(
        Download(videoId = "test"),
        types.map { type -> DownloadItem(type = type, videoId = "test", fileName = "test", path = Path("test")) }
    )
}
