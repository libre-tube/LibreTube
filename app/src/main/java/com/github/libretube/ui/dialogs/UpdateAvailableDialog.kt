package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.obj.update.UpdateInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.serialization.json.Json

class UpdateAvailableDialog : DialogFragment() {
    private lateinit var updateInfo: UpdateInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val encodedString = it.getString("updateInfo")!!
            updateInfo = Json.decodeFromString(encodedString)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(context?.getString(R.string.update_available, updateInfo.name))
            .setMessage(context?.getString(R.string.update_available_text))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(context?.getString(R.string.okay)) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW).setData(updateInfo.htmlUrl.toUri())
                startActivity(intent)
            }
            .show()
    }
}
