package com.github.libretube.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.util.ThemeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RequireRestartDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.require_restart)
                .setMessage(R.string.require_restart_message)
                .setPositiveButton(R.string.okay) { _, _ ->
                    activity?.recreate()
                    ThemeHelper.restartMainActivity(requireContext())
                }
                .setNegativeButton(R.string.cancel, null)
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
