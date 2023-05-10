package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.toastFromMainDispatcher
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeletePlaylistDialog(
    private val playlistId: String,
    private val playlistType: PlaylistType,
    private val onSuccess: () -> Unit,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.deletePlaylist)
            .setMessage(R.string.areYouSure)
            .setPositiveButton(R.string.yes) { _, _ ->
                val appContext = context?.applicationContext
                CoroutineScope(Dispatchers.IO).launch {
                    val success = PlaylistsHelper.deletePlaylist(playlistId, playlistType)
                    appContext?.toastFromMainDispatcher(
                        if (success) R.string.success else R.string.fail,
                    )
                    withContext(Dispatchers.Main) {
                        runCatching {
                            onSuccess.invoke()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
