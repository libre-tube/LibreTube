package com.github.libretube.ui.sheets

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.constants.IntentData
import com.github.libretube.enums.ImportFormat
import com.github.libretube.enums.PlaylistType
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.enums.menuoption.PlayListMenuOption
import com.github.libretube.extensions.serializable
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.ContextHelper
import com.github.libretube.helpers.DownloadHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.parcelable.PlayerData

import com.github.libretube.repo.UserDataRepositoryHelper
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.dialogs.DeletePlaylistDialog
import com.github.libretube.ui.dialogs.PlaylistDescriptionDialog
import com.github.libretube.ui.dialogs.RenamePlaylistDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.preferences.BackupRestoreSettings
import com.github.libretube.util.PlayingQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistOptionsBottomSheet : BaseBottomSheet() {
    private lateinit var playlistName: String
    private lateinit var playlistId: String
    private lateinit var playlistType: PlaylistType

    private var exportFormat: ImportFormat = ImportFormat.NEWPIPE

    private fun buildOptionsList(isBookmarked: Boolean?): List<PlayListMenuOption> {
        // options for the dialog
        val optionsList = mutableListOf(PlayListMenuOption.PLAY_ON_BACKGROUND, PlayListMenuOption.DOWNLOAD)

        if (PlayingQueue.isNotEmpty()) optionsList.add(PlayListMenuOption.ADD_TO_QUEUE)

        if (playlistType == PlaylistType.PUBLIC) {
            optionsList.add(PlayListMenuOption.SHARE)
            optionsList.add(PlayListMenuOption.CLONE)

            // only add the bookmark option to the playlist if public
            if (isBookmarked != null) {
                optionsList.add(
                    if (isBookmarked) PlayListMenuOption.REMOVE_FROM_BOOKMARKS else PlayListMenuOption.ADD_TO_BOOKMARKS
                )
            }
        } else {
            optionsList.add(PlayListMenuOption.EXPORT)
            optionsList.add(PlayListMenuOption.RENAME)
            optionsList.add(PlayListMenuOption.CHANGE_DESCRIPTION)
            optionsList.add(PlayListMenuOption.DELETE)
        }

        return optionsList
    }

    private suspend fun onOptionSelected(playListOption: PlayListMenuOption, isBookmarked: Boolean) {
        val mFragmentManager = (context as BaseActivity).supportFragmentManager

        when (playListOption) {
            // play the playlist in the background
            PlayListMenuOption.PLAY_ON_BACKGROUND -> {
                val playlist = withContext(Dispatchers.IO) {
                    runCatching { PlaylistsHelper.getPlaylist(playlistId, playlistType) }
                }.getOrElse {
                    context?.toastFromMainDispatcher(R.string.error)
                    return
                }

                playlist.relatedStreams.firstOrNull()?.let {
                    BackgroundHelper.playOnBackground(
                        requireContext(),
                        PlayerData(it.url!!.toID(), playlistId = playlistId)
                    )
                }
            }

            PlayListMenuOption.ADD_TO_QUEUE -> {
                PlayingQueue.insertPlaylist(playlistId, playlistType, null)
            }
            // Clone the playlist to the users Piped account
            PlayListMenuOption.CLONE -> {
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
            PlayListMenuOption.SHARE -> {
                val newShareDialog = ShareDialog()
                newShareDialog.arguments = bundleOf(
                    IntentData.id to playlistId,
                    IntentData.shareObjectType to ShareObjectType.PLAYLIST,
                    IntentData.shareData to ShareData(currentPlaylist = playlistName)
                )
                // using parentFragmentManager, childFragmentManager doesn't work here
                newShareDialog.show(parentFragmentManager, ShareDialog::class.java.name)
            }

            PlayListMenuOption.DELETE -> {
                val newDeletePlaylistDialog = DeletePlaylistDialog()
                newDeletePlaylistDialog.arguments = bundleOf(
                    IntentData.playlistId to playlistId
                )
                newDeletePlaylistDialog.show(mFragmentManager, null)
            }

            PlayListMenuOption.RENAME -> {
                val newRenamePlaylistDialog = RenamePlaylistDialog()
                newRenamePlaylistDialog.arguments = bundleOf(
                    IntentData.playlistId to playlistId,
                    IntentData.playlistName to playlistName
                )
                newRenamePlaylistDialog.show(mFragmentManager, null)
            }

            PlayListMenuOption.CHANGE_DESCRIPTION -> {
                val newPlaylistDescriptionDialog = PlaylistDescriptionDialog()
                newPlaylistDescriptionDialog.arguments = bundleOf(
                    IntentData.playlistId to playlistId,
                    IntentData.playlistDescription to ""
                )
                newPlaylistDescriptionDialog.show(mFragmentManager, null)
            }

            PlayListMenuOption.DOWNLOAD -> {
                DownloadHelper.startDownloadPlaylistDialog(
                    requireContext(),
                    mFragmentManager,
                    playlistId,
                    playlistName,
                    playlistType
                )
            }

            PlayListMenuOption.EXPORT -> {
                val context = requireContext()

                BackupRestoreSettings.createImportFormatDialog(
                    context,
                    R.string.export_playlist,
                    BackupRestoreSettings.exportPlaylistFormatList + listOf(ImportFormat.URLSORIDS)
                ) { format, includeTimestamp ->
                    exportFormat = format
                    ContextHelper.unwrapActivity<MainActivity>(context)
                        .startPlaylistExport(
                            playlistId,
                            playlistName,
                            exportFormat,
                            includeTimestamp
                        )
                }
            }

            else -> {
                withContext(Dispatchers.IO) {
                    if (isBookmarked) {
                        UserDataRepositoryHelper.userDataRepository.deletePlaylistBookmark(
                            playlistId
                        )
                    } else {
                        val bookmark = runCatching {
                            MediaServiceRepository.instance.getPlaylist(playlistId)
                        }.getOrElse { return@withContext }.toPlaylistBookmark(playlistId)
                        UserDataRepositoryHelper.userDataRepository.createPlaylistBookmark(bookmark)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            playlistName = it.getString(IntentData.playlistName)!!
            playlistId = it.getString(IntentData.playlistId)!!
            playlistType = it.serializable(IntentData.playlistType)!!
        }

        setTitle(playlistName)

        var optionsList = buildOptionsList(null)
        setItems(optionsList.map { option ->
            option.toBottomSheetItem(::getString)
        }) { which ->
            onOptionSelected(optionsList[which], false)
        }

        // we have to update the options list as soon as we know whether the playlist is bookmarked
        lifecycleScope.launch(Dispatchers.IO) {
            val isBookmarked = runCatching {
                UserDataRepositoryHelper.userDataRepository.getPlaylistBookmark(playlistId)
            }.getOrNull() != null

            withContext(Dispatchers.Main) {
                optionsList = buildOptionsList(isBookmarked)

                setItems(optionsList.map { option ->
                    option.toBottomSheetItem(::getString)
                }) { which ->
                    onOptionSelected(optionsList[which], isBookmarked)
                }
            }
        }
    }

    companion object {
        const val PLAYLIST_OPTIONS_REQUEST_KEY = "playlist_options_request_key"
    }
}
