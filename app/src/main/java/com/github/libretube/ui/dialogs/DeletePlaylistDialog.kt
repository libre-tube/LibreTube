package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.PlaylistId
import com.github.libretube.extensions.TAG
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeletePlaylistDialog(
    private val playlistId: String,
    private val onSuccess: () -> Unit = {}
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.deletePlaylist)
            .setMessage(R.string.areYouSure)
            .setPositiveButton(R.string.yes) { _, _ ->
                PreferenceHelper.getToken()
                deletePlaylist()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deletePlaylist() {
        CoroutineScope(Dispatchers.IO).launch {
            val response = try {
                RetrofitInstance.authApi.deletePlaylist(
                    PreferenceHelper.getToken(),
                    PlaylistId(playlistId)
                )
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                return@launch
            }
            try {
                if (response.message == "ok") {
                    onSuccess.invoke()
                }
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
            }
        }
    }
}
