package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.PlaylistId
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.TAG
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeletePlaylistDialog(
    private val playlistId: String,
    private val playlistType: PlaylistType,
    private val onSuccess: () -> Unit
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.deletePlaylist)
            .setMessage(R.string.areYouSure)
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    deletePlaylist()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private suspend fun deletePlaylist() {
        if (playlistType == PlaylistType.LOCAL) {
            DatabaseHolder.Database.localPlaylistsDao().deletePlaylistById(playlistId)
            DatabaseHolder.Database.localPlaylistsDao().deletePlaylistItemsByPlaylistId(playlistId)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
            return
        }

        val response = try {
            RetrofitInstance.authApi.deletePlaylist(
                PreferenceHelper.getToken(),
                PlaylistId(playlistId)
            )
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
            return
        }
        try {
            if (response.message == "ok") {
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
        }
    }
}
