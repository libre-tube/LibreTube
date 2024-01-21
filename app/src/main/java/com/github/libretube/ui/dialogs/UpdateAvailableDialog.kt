package com.github.libretube.ui.dialogs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import com.github.libretube.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UpdateAvailableDialog {

    fun showDialog(changelog: String,releaseUrl:String, context: Context) {

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.update_available)
            .setMessage(changelog)
            .setPositiveButton(R.string.download) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW).setData(releaseUrl.toUri())
                startActivity(context,intent, Bundle())
            }
            .setNegativeButton(R.string.tooltip_dismiss) { dialog, _ ->
                dialog.dismiss()
            }
                .show()
    }
}