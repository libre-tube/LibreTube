package com.github.libretube.preferences

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.github.libretube.R

class DownloadSettings : PreferenceFragmentCompat() {
    val TAG = "DownloadSettings"
    private val directoryRequestCode = 9999
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.download_settings, rootKey)

        // doesn't work yet
        val directory = findPreference<Preference>("download_directory")
        directory?.setOnPreferenceClickListener {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val pickerInitialUri = sharedPreferences.getString("download_directory_path", "")?.toUri()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            startActivityForResult(Intent.createChooser(intent, "Choose directory"), directoryRequestCode)
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            directoryRequestCode -> {
                val directoryUri = data?.data
                // save selected download directory to the shared preferences
                val sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(requireContext())
                sharedPreferences.edit().putString("download_directory_path", directoryUri.toString())
                    .apply()
            }
        }
    }
}