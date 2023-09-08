package com.github.libretube.ui.preferences

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.IntentData
import com.github.libretube.ui.activities.SettingsActivity
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.UpdateAvailableDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.settings

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val update = findPreference<Preference>("update")

        // set the version of the update preference
        val versionString = if (BuildConfig.DEBUG) {
            "${BuildConfig.VERSION_NAME} Debug"
        } else {
            getString(R.string.version, BuildConfig.VERSION_NAME)
        }
        update?.title = versionString

        // checking for update: yes -> dialog, no -> snackBar
        update?.setOnPreferenceClickListener {
            lifecycleScope.launch {
                // check for update
                val updateInfo = try {
                    withContext(Dispatchers.IO) {
                        RetrofitInstance.externalApi.getUpdateInfo()
                    }
                } catch (e: Exception) {
                    showSnackBar(R.string.unknown_error)
                    return@launch
                }

                if (BuildConfig.VERSION_NAME != updateInfo.name) {
                    // show the UpdateAvailableDialog if there's an update available
                    val newUpdateAvailableDialog = UpdateAvailableDialog()
                    newUpdateAvailableDialog.arguments =
                        bundleOf(IntentData.updateInfo to updateInfo)
                    newUpdateAvailableDialog.show(
                        childFragmentManager,
                        UpdateAvailableDialog::class.java.name
                    )
                } else {
                    // otherwise show the no update available snackBar
                    showSnackBar(R.string.app_uptodate)
                }
            }
            true
        }
    }

    private fun showSnackBar(@StringRes text: Int) {
        (activity as? SettingsActivity)?.binding?.let {
            Snackbar.make(it.root, text, Snackbar.LENGTH_SHORT)
                .show()
        }
    }
}
