package com.github.libretube.ui.preferences

import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.DeleteAccountDialog
import com.github.libretube.ui.dialogs.LoginDialog
import com.github.libretube.ui.dialogs.LogoutDialog
import com.github.libretube.ui.dialogs.SelectInstanceDialog
import com.github.libretube.ui.models.InstancesModel
import com.github.libretube.ui.views.ButtonGroupPreference
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class InstanceSettings : BasePreferenceFragment() {
    private val customInstancesModel: InstancesModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.instance_settings, rootKey)

        val instancePref = findPreference<ListPreference>(PreferenceKeys.FETCH_INSTANCE)!!
        val authInstance = findPreference<ListPreference>(PreferenceKeys.AUTH_INSTANCE)!!

        for (instancePref in arrayOf(instancePref, authInstance)) {
            instancePref.summaryProvider =
                Preference.SummaryProvider<ListPreference> { preference ->
                    preference.value
                }
        }

        authInstance.setOnPreferenceChangeListener { _, _ ->
            RetrofitInstance.apiLazyMgr.reset()
            logoutAndUpdateUI(true)
            true
        }

        val login = findPreference<Preference>(PreferenceKeys.LOGIN_REGISTER)
        val logout = findPreference<Preference>(PreferenceKeys.LOGOUT)
        val deleteAccount = findPreference<Preference>(PreferenceKeys.DELETE_ACCOUNT)

        childFragmentManager.setFragmentResultListener(
            INSTANCE_DIALOG_REQUEST_KEY,
            this
        ) { _, resultBundle ->
            val isLoggedIn = resultBundle.getBoolean(IntentData.loginTask)
            val isLoggedOut = resultBundle.getBoolean(IntentData.logoutTask)
            if (isLoggedIn) {
                toggleAuthAccountActionsUI(true)
            } else if (isLoggedOut) {
                logoutAndUpdateUI(true)
            }
        }

        login?.setOnPreferenceClickListener {
            LoginDialog().show(childFragmentManager, LoginDialog::class.java.name)
            true
        }

        logout?.setOnPreferenceClickListener {
            LogoutDialog().show(childFragmentManager, LogoutDialog::class.java.name)
            true
        }

        deleteAccount?.setOnPreferenceClickListener {
            DeleteAccountDialog()
                .show(childFragmentManager, DeleteAccountDialog::class.java.name)
            true
        }

        val youTubeDataSource = findPreference<ButtonGroupPreference>(PreferenceKeys.YOUTUBE_DATA_SOURCE)!!
        val localReturnYouTubeDislike = findPreference<SwitchPreferenceCompat>(PreferenceKeys.LOCAL_RYD)!!
        val instanceCategory = findPreference<PreferenceCategory>("instance_category")!!

        localReturnYouTubeDislike.isVisible = youTubeDataSource.value != "piped"
        instanceCategory.isVisible = youTubeDataSource.value == "piped"
        youTubeDataSource.setOnPreferenceChangeListener { _, newValue ->
            localReturnYouTubeDislike.isVisible = newValue != "piped"
            instanceCategory.isVisible = newValue == "piped"

            true
        }

        val syncServerType = findPreference<ButtonGroupPreference>(PreferenceKeys.SYNC_SERVER_TYPE)!!
        val libretubeSyncServerInstance = findPreference<EditTextPreference>(PreferenceKeys.LIBRETUBE_SYNC_SERVER_URL)!!

        authInstance.isVisible = syncServerType.value == "piped"
        libretubeSyncServerInstance.isVisible = syncServerType.value == "libretube"
        toggleAuthAccountActionsUI(syncServerType.value != "none")
        syncServerType.setOnPreferenceChangeListener { _, newValue ->
            authInstance.isVisible = newValue == "piped"
            libretubeSyncServerInstance.isVisible = newValue == "libretube"

            logoutAndUpdateUI(newValue != "none")
            true
        }

        libretubeSyncServerInstance.setOnPreferenceChangeListener { _, newValue ->
            // validate that the input is an actual URL
            if (newValue.toString().toHttpUrlOrNull() == null)  {
                context?.toastFromMainThread(R.string.invalid_url)
                return@setOnPreferenceChangeListener false
            }

            logoutAndUpdateUI(true)
            true
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference.key in arrayOf(
                PreferenceKeys.FETCH_INSTANCE,
                PreferenceKeys.AUTH_INSTANCE
            )
        ) {
            showInstanceSelectionDialog(preference as ListPreference)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun showInstanceSelectionDialog(preference: ListPreference) {
        SelectInstanceDialog()
            .apply {
                arguments = bundleOf(
                    SelectInstanceDialog.SELECT_INSTANCE_TITLE_EXTRA to getString(R.string.auth_instance),
                    SelectInstanceDialog.SELECT_INSTANCE_CURRENT_INSTANCE_API_URL_EXTRA to preference.value
                )
            }
            .show(childFragmentManager, null)
        childFragmentManager.setFragmentResultListener(
            SelectInstanceDialog.SELECT_INSTANCE_RESULT_KEY,
            this
        ) { _, bundle ->
            val apiUrl =
                bundle.getString(SelectInstanceDialog.SELECT_INSTANCE_CURRENT_INSTANCE_API_URL_EXTRA)
            preference.value = apiUrl
            resetForNewInstance()
            childFragmentManager.clearFragmentResultListener(SelectInstanceDialog.SELECT_INSTANCE_RESULT_KEY)
        }
    }

    private fun toggleAuthAccountActionsUI(hasAuthSupport: Boolean) {
        val loggedIn = PreferenceHelper.getToken().isNotBlank()

        findPreference<Preference>(PreferenceKeys.LOGIN_REGISTER)?.isVisible = !loggedIn && hasAuthSupport
        findPreference<Preference>(PreferenceKeys.LOGOUT)?.isVisible = loggedIn && hasAuthSupport
        findPreference<Preference>(PreferenceKeys.DELETE_ACCOUNT)?.isVisible = loggedIn && hasAuthSupport
    }

    private fun logoutAndUpdateUI(hasAuthSupport: Boolean) {
        if (PreferenceHelper.getToken().isNotBlank()) {
            Toast.makeText(context, getString(R.string.loggedout), Toast.LENGTH_SHORT).show()
            PreferenceHelper.setToken("")
        }
        toggleAuthAccountActionsUI(hasAuthSupport)
    }

    private fun resetForNewInstance() {
        RetrofitInstance.apiLazyMgr.reset()
        ActivityCompat.recreate(requireActivity())
    }

    companion object {
        const val INSTANCE_DIALOG_REQUEST_KEY = "instance_dialog_request_key"
    }
}
