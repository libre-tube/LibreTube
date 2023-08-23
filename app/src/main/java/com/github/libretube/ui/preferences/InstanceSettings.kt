package com.github.libretube.ui.preferences

import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.api.InstanceHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Instances
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.CustomInstanceDialog
import com.github.libretube.ui.dialogs.DeleteAccountDialog
import com.github.libretube.ui.dialogs.LoginDialog
import com.github.libretube.ui.dialogs.LogoutDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstanceSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.instance
    private val token get() = PreferenceHelper.getToken()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.instance_settings, rootKey)

        val instancePref = findPreference<ListPreference>(PreferenceKeys.FETCH_INSTANCE)!!
        val authInstanceToggle = findPreference<SwitchPreferenceCompat>(
            PreferenceKeys.AUTH_INSTANCE_TOGGLE
        )!!
        val authInstance = findPreference<ListPreference>(PreferenceKeys.AUTH_INSTANCE)!!
        val instancePrefs = listOf(instancePref, authInstance)

        val appContext = requireContext().applicationContext

        lifecycleScope.launch {
            // update the instances to also show custom ones
            initInstancesPref(instancePrefs, InstanceHelper.getInstancesFallback(appContext))

            // try to fetch the public list of instances async
            try {
                val instances = withContext(Dispatchers.IO) {
                    InstanceHelper.getInstances(appContext)
                }
                initInstancesPref(instancePrefs, instances)
            } catch (e: Exception) {
                appContext.toastFromMainDispatcher(e.message.orEmpty())
            }
        }

        instancePref.setOnPreferenceChangeListener { _, _ ->
            if (!authInstanceToggle.isChecked) {
                logoutAndUpdateUI()
            }
            RetrofitInstance.lazyMgr.reset()
            ActivityCompat.recreate(requireActivity())
            true
        }

        authInstance.setOnPreferenceChangeListener { _, _ ->
            RetrofitInstance.lazyMgr.reset()
            logoutAndUpdateUI()
            true
        }

        authInstanceToggle.setOnPreferenceChangeListener { _, _ ->
            RetrofitInstance.lazyMgr.reset()
            logoutAndUpdateUI()
            true
        }

        val customInstance = findPreference<Preference>(PreferenceKeys.CUSTOM_INSTANCE)
        customInstance?.setOnPreferenceClickListener {
            CustomInstanceDialog()
                .show(childFragmentManager, CustomInstanceDialog::class.java.name)
            true
        }

        val clearCustomInstances = findPreference<Preference>(PreferenceKeys.CLEAR_CUSTOM_INSTANCES)
        clearCustomInstances?.setOnPreferenceClickListener {
            lifecycleScope.launch {
                Database.customInstanceDao().deleteAll()
                ActivityCompat.recreate(requireActivity())
            }
            true
        }

        val login = findPreference<Preference>(PreferenceKeys.LOGIN_REGISTER)
        val logout = findPreference<Preference>(PreferenceKeys.LOGOUT)
        val deleteAccount = findPreference<Preference>(PreferenceKeys.DELETE_ACCOUNT)

        login?.isVisible = token.isEmpty()
        logout?.isVisible = token.isNotEmpty()
        deleteAccount?.isEnabled = token.isNotEmpty()

        login?.setOnPreferenceClickListener {
            LoginDialog {
                login.isVisible = false
                logout?.isVisible = true
                deleteAccount?.isEnabled = true
            }
                .show(childFragmentManager, LoginDialog::class.java.name)
            true
        }

        logout?.setOnPreferenceClickListener {
            LogoutDialog(this::logoutAndUpdateUI)
                .show(childFragmentManager, LogoutDialog::class.java.name)
            true
        }

        deleteAccount?.setOnPreferenceClickListener {
            DeleteAccountDialog(this::logoutAndUpdateUI)
                .show(childFragmentManager, DeleteAccountDialog::class.java.name)
            true
        }
    }

    private suspend fun initInstancesPref(
        instancePrefs: List<ListPreference>,
        publicInstances: List<Instances>
    ) = runCatching {
        val customInstanceList = withContext(Dispatchers.IO) {
            Database.customInstanceDao().getAll()
        }

        val customInstances = customInstanceList
            .map { Instances(it.name, it.apiUrl) }

        val instances = publicInstances
            .plus(customInstances)
            .sortedBy { it.name }

        for (instancePref in instancePrefs) {
            // add custom instances to the list preference
            instancePref.entries = instances.map { it.name }.toTypedArray()
            instancePref.entryValues = instances.map { it.apiUrl }.toTypedArray()
            instancePref.summaryProvider =
                Preference.SummaryProvider<ListPreference> { preference ->
                    preference.entry
                }
        }
    }

    private fun logoutAndUpdateUI() {
        PreferenceHelper.setToken("")
        Toast.makeText(context, getString(R.string.loggedout), Toast.LENGTH_SHORT).show()
        findPreference<Preference>(PreferenceKeys.LOGIN_REGISTER)?.isVisible = true
        findPreference<Preference>(PreferenceKeys.LOGOUT)?.isVisible = false
        findPreference<Preference>(PreferenceKeys.DELETE_ACCOUNT)?.isEnabled = false
    }
}
