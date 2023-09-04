package com.github.libretube.ui.sheets

import android.os.Bundle
import androidx.core.os.bundleOf
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.enums.PlaylistType
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.ui.dialogs.DeletePlaylistDialog
import com.github.libretube.ui.dialogs.PlaylistDescriptionDialog
import com.github.libretube.ui.dialogs.RenamePlaylistDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.util.PlayingQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PlaylistOptionsBottomSheet(
    private val playlistId: String,
    private val playlistName: String,
    private val playlistType: PlaylistType,
    private val onRename: (newName: String) -> Unit = {},
    private val onChangeDescription: (newDescription: String) -> Unit = {},
    private val onDelete: () -> Unit = {}
) : BaseBottomSheet() {
    private val shareData = ShareData(currentPlaylist = playlistName)
    override fun onCreate(savedInstanceState: Bundle?) {
        // options for the dialog
        val optionsList = mutableListOf(
            getString(R.string.playOnBackground)
        )

        if (PlayingQueue.isNotEmpty()) optionsList.add(getString(R.string.add_to_queue))

        val isBookmarked = runBlocking(Dispatchers.IO) {
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
            optionsList.add(getString(R.string.renamePlaylist))
            optionsList.add(getString(R.string.change_playlist_description))
            optionsList.add(getString(R.string.deletePlaylist))
        }

        setSimpleItems(optionsList) { which ->
            when (optionsList[which]) {
                // play the playlist in the background
                getString(R.string.playOnBackground) -> {
                    val playlist = withContext(Dispatchers.IO) {
                        PlaylistsHelper.getPlaylist(playlistId)
                    }
                    playlist.relatedStreams.firstOrNull()?.let {
                        BackgroundHelper.playOnBackground(
                            requireContext(),
                            it.url!!.toID(),
                            playlistId = playlistId
                        )
                    }
                }

                getString(R.string.add_to_queue) -> {
                    PlayingQueue.insertPlaylist(playlistId, null)
                }
                // Clone the playlist to the users Piped account
                getString(R.string.clonePlaylist) -> {
                    val context = requireContext()
                    val playlistId = withContext(Dispatchers.IO) {
                        runCatching {
                            PlaylistsHelper.clonePlaylist(playlistId)
                        }.getOrNull()
                    }
                    context.toastFromMainDispatcher(
                        if (playlistId != null) R.string.playlistCloned else R.string.server_error
                    )
                }
                // share the playlist
                getString(R.string.share) -> {
                    val bundle = bundleOf(
                        IntentData.id to playlistId,
                        IntentData.shareObjectType to ShareObjectType.PLAYLIST,
                        IntentData.shareData to shareData
                    )
                    val newShareDialog = ShareDialog()
                    newShareDialog.arguments = bundle
                    // using parentFragmentManager, childFragmentManager doesn't work here
                    newShareDialog.show(parentFragmentManager, ShareDialog::class.java.name)
                }

                getString(R.string.deletePlaylist) -> {
                    DeletePlaylistDialog(playlistId, playlistType) {
                        // try to refresh the playlists in the library on deletion success
                        onDelete()
                    }.show(parentFragmentManager, null)
                }

                getString(R.string.renamePlaylist) -> {
                    RenamePlaylistDialog(playlistId, playlistName, onRename)
                        .show(parentFragmentManager, null)
                }

                getString(R.string.change_playlist_description) -> {
                    val bundle = bundleOf(
                        IntentData.playlistId to playlistId,
                        IntentData.playlistDescription to ""
                    )
                    val newShareDialog = PlaylistDescriptionDialog()
                    newShareDialog.arguments = bundle
                    newShareDialog.show(parentFragmentManager, null)
                    parentFragmentManager.setFragmentResultListener(
                        IntentData.requestKey,
                        this
                    ) { _, resultBundle ->
                        val newDescription = resultBundle.getString(IntentData.playlistDescription)!!
                        onChangeDescription.invoke(newDescription)
                    }
                }

                else -> {
                    withContext(Dispatchers.IO) {
                        if (isBookmarked) {
                            DatabaseHolder.Database.playlistBookmarkDao().deleteById(playlistId)
                        } else {
                            val bookmark = try {
                                RetrofitInstance.api.getPlaylist(playlistId)
                            } catch (e: Exception) {
                                return@withContext
                            }.toPlaylistBookmark(playlistId)
                            DatabaseHolder.Database.playlistBookmarkDao().insert(bookmark)
                        }
                    }
                }
            }
        }
        super.onCreate(savedInstanceState)
    }
}
