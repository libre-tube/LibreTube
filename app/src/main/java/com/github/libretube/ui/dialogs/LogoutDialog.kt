package com.github.libretube.ui.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.preferences.InstanceSettings
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog for user logout confirmation.
 * Displays the currently logged-in username and allows the user to logout.
 */
class LogoutDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val username = PreferenceHelper.getUsername()
        val message = if (username.isNotEmpty()) {
            getString(R.string.already_logged_in, username)
        } else {
            getString(R.string.already_logged_in)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(message)
            .setPositiveButton(R.string.logout) { _, _ ->
                performLogout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Performs the logout operation and notifies the parent fragment.
     */
    private fun performLogout() {
        Toast.makeText(context, R.string.loggedout, Toast.LENGTH_SHORT).show()

        setFragmentResult(
            InstanceSettings.INSTANCE_DIALOG_REQUEST_KEY,
            bundleOf(IntentData.logoutTask to true)
        )
    }
}
