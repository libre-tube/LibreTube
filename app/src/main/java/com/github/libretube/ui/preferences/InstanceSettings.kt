package com.github.libretube.ui.preferences

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.FALLBACK_INSTANCES_URL
import com.github.libretube.constants.PIPED_INSTANCES_URL
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.extensions.awaitQuery
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.CustomInstanceDialog
import com.github.libretube.ui.dialogs.DeleteAccountDialog
import com.github.libretube.ui.dialogs.LoginDialog
import com.github.libretube.ui.dialogs.LogoutDialog
import com.github.libretube.util.PreferenceHelper

class InstanceSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.instance

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.instance_settings, rootKey)

        val instance = findPreference<ListPreference>(PreferenceKeys.FETCH_INSTANCE)
        // fetchInstance()
        initCustomInstances(instance!!)
        instance.setOnPreferenceChangeListener { _, newValue ->
            RetrofitInstance.url = newValue.toString()
            if (!PreferenceHelper.getBoolean(PreferenceKeys.AUTH_INSTANCE_TOGGLE, false)) {
                RetrofitInstance.authUrl = newValue.toString()
                logout()
            }
            RetrofitInstance.lazyMgr.reset()
            activity?.recreate()
            true
        }

        val authInstance = findPreference<ListPreference>(PreferenceKeys.AUTH_INSTANCE)
        initCustomInstances(authInstance!!)
        // hide auth instance if option deselected
        if (!PreferenceHelper.getBoolean(PreferenceKeys.AUTH_INSTANCE_TOGGLE, false)) {
            authInstance.isVisible = false
        }
        authInstance.setOnPreferenceChangeListener { _, newValue ->
            // save new auth url
            RetrofitInstance.authUrl = newValue.toString()
            RetrofitInstance.lazyMgr.reset()
            logout()
            activity?.recreate()
            true
        }

        val authInstanceToggle =
            findPreference<SwitchPreferenceCompat>(PreferenceKeys.AUTH_INSTANCE_TOGGLE)
        authInstanceToggle?.setOnPreferenceChangeListener { _, newValue ->
            authInstance.isVisible = newValue == true
            logout()
            // either use new auth url or the normal api url if auth instance disabled
            RetrofitInstance.authUrl = if (newValue == false) {
                RetrofitInstance.url
            } else {
                authInstance.value
            }
            RetrofitInstance.lazyMgr.reset()
            activity?.recreate()
            true
        }

        val customInstance = findPreference<Preference>(PreferenceKeys.CUSTOM_INSTANCE)
        customInstance?.setOnPreferenceClickListener {
            val newFragment = CustomInstanceDialog()
            newFragment.show(childFragmentManager, CustomInstanceDialog::class.java.name)
            true
        }

        val clearCustomInstances = findPreference<Preference>(PreferenceKeys.CLEAR_CUSTOM_INSTANCES)
        clearCustomInstances?.setOnPreferenceClickListener {
            awaitQuery {
                Database.customInstanceDao().deleteAll()
            }
            activity?.recreate()
            true
        }

        val login = findPreference<Preference>(PreferenceKeys.LOGIN_REGISTER)
        val token = PreferenceHelper.getToken()
        if (token != "") login?.setTitle(R.string.logout)
        login?.setOnPreferenceClickListener {
            if (token == "") {
                val newFragment = LoginDialog()
                newFragment.show(childFragmentManager, LoginDialog::class.java.name)
            } else {
                val newFragment = LogoutDialog()
                newFragment.show(childFragmentManager, LogoutDialog::class.java.name)
            }

            true
        }

        val deleteAccount = findPreference<Preference>(PreferenceKeys.DELETE_ACCOUNT)
        deleteAccount?.isEnabled = PreferenceHelper.getToken() != ""
        deleteAccount?.setOnPreferenceClickListener {
            val newFragment = DeleteAccountDialog()
            newFragment.show(childFragmentManager, DeleteAccountDialog::class.java.name)
            true
        }
    }

    private fun initCustomInstances(instancePref: ListPreference) {
        val appContext = requireContext().applicationContext
        lifecycleScope.launchWhenCreated {
            val customInstances = awaitQuery {
                Database.customInstanceDao().getAll()
            }

            val instanceNames = arrayListOf<String>()
            val instanceValues = arrayListOf<String>()

            // fetch official public instances from kavin.rocks as well as tokhmi.xyz as fallback
            val response = runCatching {
                RetrofitInstance.externalApi.getInstances(PIPED_INSTANCES_URL).toMutableList()
            }.getOrNull() ?: runCatching {
                RetrofitInstance.externalApi.getInstances(FALLBACK_INSTANCES_URL).toMutableList()
            }.getOrNull()

            if (response == null) {
                appContext.toastFromMainThread(R.string.failed_fetching_instances)
                instanceNames.addAll(resources.getStringArray(R.array.instances))
                instanceValues.addAll(resources.getStringArray(R.array.instancesValue))
            }

            response?.sortBy { it.name }

            instanceNames.addAll(response.orEmpty().map { it.name })
            instanceValues.addAll(response.orEmpty().map { it.apiUrl })

            customInstances.forEach { instance ->
                instanceNames += instance.name
                instanceValues += instance.apiUrl
            }

            runOnUiThread {
                // add custom instances to the list preference
                instancePref.entries = instanceNames.toTypedArray()
                instancePref.entryValues = instanceValues.toTypedArray()
                instancePref.summaryProvider =
                    Preference.SummaryProvider<ListPreference> { preference ->
                        preference.entry
                    }
            }
        }
    }

    private fun logout() {
        PreferenceHelper.setToken("")
        Toast.makeText(context, getString(R.string.loggedout), Toast.LENGTH_SHORT).show()
    }
}
