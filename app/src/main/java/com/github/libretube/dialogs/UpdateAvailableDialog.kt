package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UpdateAvailableDialog(
    private val versionTag: String,
    private val updateLink: String,
    private val updateAvailable: Boolean
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            if (updateAvailable) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(context?.getString(R.string.update_available, versionTag))
                    .setMessage(context?.getString(R.string.update_available_text))
                    .setNegativeButton(context?.getString(R.string.cancel)) { _, _ ->
                        dismiss()
                    }
                    .setPositiveButton(context?.getString(R.string.okay)) { _, _ ->
                        val uri = Uri.parse(updateLink)
                        val intent = Intent(Intent.ACTION_VIEW).setData(uri)
                        startActivity(intent)
                    }
                    .show()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(context?.getString(R.string.app_uptodate))
                    .setMessage(context?.getString(R.string.no_update_available))
                    .setPositiveButton(context?.getString(R.string.okay)) { _, _ -> }
                    .show()
            }
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
