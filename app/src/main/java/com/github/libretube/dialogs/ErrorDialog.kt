package com.github.libretube.dialogs

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.preferences.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ErrorDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val errorLog = PreferenceHelper.getErrorLog()
        // reset the error log
        PreferenceHelper.saveErrorLog("")

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.error_occurred)
            .setMessage(errorLog)
            .setNegativeButton(R.string.okay, null)
            .setPositiveButton(R.string.copy) { _, _ ->
                /**
                 * copy the error log to the clipboard
                 */
                val clipboard: ClipboardManager =
                    context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(context?.getString(R.string.copied), errorLog)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
