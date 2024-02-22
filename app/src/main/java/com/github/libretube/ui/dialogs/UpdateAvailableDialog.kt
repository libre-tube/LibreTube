package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UpdateAvailableDialog : DialogFragment() {

    fun onCreateDialog(
        changelog: String,
        releaseUrl: String,
        context: Context
    ): Dialog {

        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.update_available)
            .setMessage(changelog)
            .setPositiveButton(R.string.download, null)
            .setNegativeButton(R.string.tooltip_dismiss, null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW).setData(releaseUrl.toUri())
                    startActivity(context, intent, Bundle())
                    dismiss()
                }
            }
    }
}
