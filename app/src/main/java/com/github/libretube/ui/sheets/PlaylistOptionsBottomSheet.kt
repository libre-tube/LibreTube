package com.github.libretube.ui.sheets

import android.os.Bundle
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.enums.PlaylistType
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.awaitQuery
import com.github.libretube.extensions.query
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toPlaylistBookmark
import com.github.libretube.obj.ShareData
import com.github.libretube.ui.dialogs.DeletePlaylistDialog
import com.github.libretube.ui.dialogs.RenamePlaylistDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.util.BackgroundHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PlaylistOptionsBottomSheet(
    private val playlistId: String,
    private val playlistName: String,
    private val playlistType: PlaylistType,
    private val onDelete: () -> Unit = {}
) : BaseBottomSheet() {
    private val shareData = ShareData(currentPlaylist = playlistName)
    override fun onCreate(savedInstanceState: Bundle?) {
        // options for the dialog
        val optionsList = mutableListOf(
            getString(R.string.playOnBackground)
        )

        val isBookmarked = awaitQuery {
            DatabaseHolder.Database.playlistBookmarkDao().includes(playlistId)
        }

        if (playlistType == PlaylistType.PUBLIC) {
            optionsList.add(getString(R.string.share))
            optionsList.add(getString(R.string.clonePlaylist))

            // only add the bookmark option to the playlist if public
            optionsList.add(
                getString(if (isBookmarked) R.string.remove_bookmark else R.string.add_to_bookmarks)
            )
        } else {
            optionsList.add(context?.getString(R.string.renamePlaylist)!!)
            optionsList.add(context?.getString(R.string.deletePlaylist)!!)
        }

        setSimpleItems(optionsList) { which ->
            when (optionsList[which]) {
                // play the playlist in the background
                getString(R.string.playOnBackground) -> {
                    runBlocking {
                        val playlist =
                            if (playlistType == PlaylistType.PRIVATE) {
                                RetrofitInstance.authApi.getPlaylist(playlistId)
                            } else {
                                RetrofitInstance.api.getPlaylist(playlistId)
                            }
                        BackgroundHelper.playOnBackground(
                            context = requireContext(),
                            videoId = playlist.relatedStreams!![0].url!!.toID(),
                            playlistId = playlistId
                        )
                    }
                }
                // Clone the playlist to the users Piped account
                getString(R.string.clonePlaylist) -> {
                    PlaylistsHelper.clonePlaylist(requireContext(), playlistId)
                }
                // share the playlist
                getString(R.string.share) -> {
                    val shareDialog = ShareDialog(playlistId, ShareObjectType.PLAYLIST, shareData)
                    // using parentFragmentManager, childFragmentManager doesn't work here
                    shareDialog.show(parentFragmentManager, ShareDialog::class.java.name)
                }
                getString(R.string.deletePlaylist) -> {
                    DeletePlaylistDialog(playlistId, playlistType) {
                        // try to refresh the playlists in the library on deletion success
                        onDelete.invoke()
                    }.show(parentFragmentManager, null)
                }
                getString(R.string.renamePlaylist) -> {
                    RenamePlaylistDialog(playlistId, playlistName)
                        .show(parentFragmentManager, null)
                }
                else -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (isBookmarked) {
                            query {
                                DatabaseHolder.Database.playlistBookmarkDao()
                                    .deleteById(playlistId)
                            }
                        } else {
                            val bookmark = try {
                                RetrofitInstance.api.getPlaylist(playlistId)
                            } catch (e: Exception) {
                                return@launch
                            }.toPlaylistBookmark(playlistId)
                            DatabaseHolder.Database.playlistBookmarkDao().insertAll(bookmark)
                        }
                    }
                    dismiss()
                }
            }
        }
        super.onCreate(savedInstanceState)
    }
}
