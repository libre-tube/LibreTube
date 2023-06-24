package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.obj.update.UpdateInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UpdateAvailableDialog(
    private val updateInfo: UpdateInfo
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(context?.getString(R.string.update_available, updateInfo.name))
            .setMessage(context?.getString(R.string.update_available_text))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(context?.getString(R.string.okay)) { _, _ ->
                val uri = Uri.parse(updateInfo.htmlUrl)
                val intent = Intent(Intent.ACTION_VIEW).setData(uri)
                startActivity(intent)
            }
            .show()
    }
}
