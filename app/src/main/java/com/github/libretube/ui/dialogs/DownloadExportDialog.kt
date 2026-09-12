package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.media3.common.util.UnstableApi
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.enums.FileType
import com.github.libretube.extensions.toAndroidUri
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.util.MediaExporter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.loadingindicator.LoadingIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DownloadExportDialog : DialogFragment() {
    @OptIn(UnstableApi::class)
    private suspend fun exportDownload(videoId: String, outputUri: Uri, onCompletion: () -> Unit) {
        val activity = requireActivity()

        val downloadItems =
            DatabaseHolder.Database.downloadDao().getDownloadById(videoId)!!.downloadItems
        val video = downloadItems.firstOrNull { it.type == FileType.VIDEO }
        val audio = downloadItems.firstOrNull { it.type == FileType.AUDIO }
        if (audio == null && video == null) return

        if (audio == null) {
            MediaExporter.copyFileContents(activity, video!!.path.toAndroidUri(), outputUri)
            onCompletion()
        } else if (video == null) {
            MediaExporter.copyFileContents(activity, audio.path.toAndroidUri(), outputUri)
            onCompletion()
        } else {
            // output stream has to be opened shortly after the file picker was shown, otherwise
            // the URI gets invalid
            val outputStream = activity.contentResolver.openOutputStream(outputUri)!!

            // we write everything to a temporary file first because the `MediaExporter`
            // can't write to URIs directly, only to paths
            val tempFile = File.createTempFile("mux", ".mp4", activity.cacheDir)

            MediaExporter.muxMedia(
                activity,
                audio.path.toAndroidUri(),
                video.path.toAndroidUri(),
                tempFile.path,
                onCompletion = {
                    // copy to user-specified file
                    MediaExporter.copyFileContents(
                        activity.contentResolver.openInputStream(tempFile.toUri())!!,
                        outputStream
                    )
                    tempFile.delete()
                    onCompletion()
                },
                onError = { exportException ->
                    exportException.printStackTrace()
                    activity.toastFromMainThread(exportException.message.orEmpty())
                    dismiss()
                }
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val videoId = requireArguments().getString(IntentData.videoId)!!
        val outputUri = requireArguments().getString(IntentData.outputUri)!!.toUri()

        var dialog: AlertDialog? = null
        val loadingIndicator = LoadingIndicator(requireContext())

        CoroutineScope(Dispatchers.IO).launch {
            exportDownload(videoId, outputUri) {
                dialog?.setTitle(R.string.exportsuccess)
                // remove loading indicator and show button to close dialog
                loadingIndicator.isVisible = false
                dialog?.getButton(Dialog.BUTTON_POSITIVE)?.isVisible = true
            }
        }

        // prevent user from closing dialog
        isCancelable = false

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.exporting)
            .setView(loadingIndicator)
            .setPositiveButton(R.string.okay, null)
            .setCancelable(false)
            .show()
            .also {
                it.getButton(Dialog.BUTTON_POSITIVE)?.isGone = true
                dialog = it
            }
    }
}