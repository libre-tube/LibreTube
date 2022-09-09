package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.services.UpdateService
import com.github.libretube.update.UpdateInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UpdateDialog(
    private val updateInfo: UpdateInfo
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(context?.getString(R.string.update_available, updateInfo.name))
            .setMessage(context?.getString(R.string.update_now))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(context?.getString(R.string.okay)) { _, _ ->
                val downloadUrl = getDownloadUrl(updateInfo)
                Log.i("downloadUrl", downloadUrl.toString())
                if (downloadUrl != null) {
                    val intent = Intent(context, UpdateService::class.java)
                    intent.putExtra("downloadUrl", downloadUrl)
                    context?.startService(intent)
                } else {
                    val uri = Uri.parse(updateInfo.html_url)
                    val intent = Intent(Intent.ACTION_VIEW).setData(uri)
                    startActivity(intent)
                }
            }
            .show()
    }

    private fun getDownloadUrl(updateInfo: UpdateInfo): String? {
        val supportedArchitectures = Build.SUPPORTED_ABIS
        supportedArchitectures.forEach { arch ->
            updateInfo.assets?.forEach { asset ->
                if (asset.name?.contains(arch) == true) return asset.browser_download_url
            }
        }
        return null
    }
}
