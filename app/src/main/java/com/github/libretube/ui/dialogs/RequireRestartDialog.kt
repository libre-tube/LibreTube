package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.helpers.NavigationHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RequireRestartDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.require_restart)
            .setMessage(R.string.require_restart_message)
            .setPositiveButton(R.string.okay) { _, _ ->
                ActivityCompat.recreate(requireActivity())
                NavigationHelper.restartMainActivity(requireContext())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
