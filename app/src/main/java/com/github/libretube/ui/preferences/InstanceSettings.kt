package com.github.libretube.ui.preferences

import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Instances
import com.github.libretube.constants.FALLBACK_INSTANCES_URL
import com.github.libretube.constants.PIPED_INSTANCES_URL
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
            PreferenceKeys.AUTH_INSTANCE_TOGGLE,
        )!!
        val authInstance = findPreference<ListPreference>(PreferenceKeys.AUTH_INSTANCE)!!
        initInstancesPref(listOf(instancePref, authInstance))

        instancePref.setOnPreferenceChangeListener { _, newValue ->
            RetrofitInstance.url = newValue.toString()
            if (!authInstanceToggle.isChecked) {
                RetrofitInstance.authUrl = newValue.toString()
                logoutAndUpdateUI()
            }
            RetrofitInstance.lazyMgr.reset()
            ActivityCompat.recreate(requireActivity())
            true
        }

        authInstance.setOnPreferenceChangeListener { _, newValue ->
            // save new auth url
            RetrofitInstance.authUrl = newValue.toString()
            RetrofitInstance.lazyMgr.reset()
            logoutAndUpdateUI()
            true
        }

        authInstanceToggle.setOnPreferenceChangeListener { _, newValue ->
            // either use new auth url or the normal api url if auth instance disabled
            RetrofitInstance.authUrl = if (newValue == false) {
                RetrofitInstance.url
            } else {
                authInstance.value
            }
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

    private fun initInstancesPref(instancePrefs: List<ListPreference>) {
        val appContext = requireContext().applicationContext

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val customInstances = withContext(Dispatchers.IO) {
                    Database.customInstanceDao().getAll()
                }

                for (instancePref in instancePrefs) {
                    instancePref.summaryProvider =
                        Preference.SummaryProvider<ListPreference> { preference ->
                            preference.entry
                        }
                }

                // fetch official public instances from kavin.rocks as well as tokhmi.xyz as
                // fallback
                val instances = withContext(Dispatchers.IO) {
                    runCatching {
                        RetrofitInstance.externalApi.getInstances(PIPED_INSTANCES_URL)
                            .toMutableList()
                    }.getOrNull() ?: runCatching {
                        RetrofitInstance.externalApi.getInstances(FALLBACK_INSTANCES_URL)
                            .toMutableList()
                    }.getOrNull() ?: run {
                        appContext.toastFromMainDispatcher(R.string.failed_fetching_instances)
                        val instanceNames = resources.getStringArray(R.array.instances)
                        resources.getStringArray(R.array.instancesValue)
                            .mapIndexed { index, instanceValue ->
                                Instances(instanceNames[index], instanceValue)
                            }
                    }
                }
                    .sortedBy { it.name }
                    .toMutableList()

                instances.addAll(customInstances.map { Instances(it.name, it.apiUrl) })

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
