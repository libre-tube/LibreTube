package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.obj.PlaylistId
import com.github.libretube.util.RetrofitInstance
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class PlaylistOptionsDialog(
    private val playlistId: String,
    context: Context
) : DialogFragment() {
    val TAG = "PlaylistOptionsDialog"

    private val optionsList = listOf(
        context.getString(R.string.clonePlaylist),
        context.getString(R.string.share)
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setAdapter(
                ArrayAdapter(
                    requireContext(),
                    R.layout.video_options_dialog_item,
                    optionsList
                )
            ) { _, which ->
                when (which) {
                    // Clone the playlist to the users Piped account
                    0 -> {
                        val sharedPref =
                            context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                        val token = sharedPref?.getString("token", "")!!
                        importPlaylist(token, playlistId)
                    }
                    // share the playlist
                    1 -> {
                        val shareDialog = ShareDialog(playlistId, true)
                        // using parentFragmentManager is important here
                        shareDialog.show(parentFragmentManager, "ShareDialog")
                    }
                }
            }
        return dialog.show()
    }

    private fun importPlaylist(token: String, playlistId: String) {
        fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                val response = try {
                    RetrofitInstance.api.importPlaylist(token, PlaylistId(playlistId))
                } catch (e: IOException) {
                    println(e)
                    return@launch
                } catch (e: HttpException) {
                    return@launch
                }
                Log.e(TAG, response.toString())
            }
        }
        run()
    }
}
