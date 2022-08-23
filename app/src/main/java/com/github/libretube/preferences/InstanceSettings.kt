package com.github.libretube.preferences

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.CustomInstance
import com.github.libretube.dialogs.CustomInstanceDialog
import com.github.libretube.dialogs.DeleteAccountDialog
import com.github.libretube.dialogs.LoginDialog
import com.github.libretube.dialogs.LogoutDialog
import com.github.libretube.extensions.await
import com.github.libretube.util.ImportHelper
import com.github.libretube.util.PermissionHelper
import com.github.libretube.views.MaterialPreferenceFragment

class InstanceSettings : MaterialPreferenceFragment() {

    /**
     * result listeners for importing and exporting subscriptions
     */
    private lateinit var getContent: ActivityResultLauncher<String>
    private lateinit var createFile: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        getContent =
            registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                ImportHelper(requireActivity()).importSubscriptions(uri)
            }
        createFile = registerForActivityResult(
            ActivityResultContracts.CreateDocument()
        ) { uri: Uri? ->
            ImportHelper(requireActivity()).exportSubscriptions(uri)
        }

        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.instance_settings, rootKey)

        val settingsActivity = activity as? SettingsActivity
        settingsActivity?.changeTopBarText(getString(R.string.instance))

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
            RetrofitInstance.authUrl = if (newValue == false) RetrofitInstance.url
            else authInstance.value
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
            Thread {
                DatabaseHolder.db.customInstanceDao().deleteAll()
            }.await()
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
        deleteAccount?.setOnPreferenceClickListener {
            if (PreferenceHelper.getToken() != "") {
                val newFragment = DeleteAccountDialog()
                newFragment.show(childFragmentManager, DeleteAccountDialog::class.java.name)
            } else {
                Toast.makeText(context, R.string.login_first, Toast.LENGTH_SHORT).show()
            }
            true
        }

        val importSubscriptions = findPreference<Preference>(PreferenceKeys.IMPORT_SUBS)
        importSubscriptions?.setOnPreferenceClickListener {
            // check StorageAccess
            val accessGranted =
                PermissionHelper.isStoragePermissionGranted(requireActivity())
            // import subscriptions
            if (accessGranted) getContent.launch("*/*")
            // request permissions if not granted
            else PermissionHelper.requestReadWrite(requireActivity())
            true
        }

        val exportSubscriptions = findPreference<Preference>(PreferenceKeys.EXPORT_SUBS)
        exportSubscriptions?.setOnPreferenceClickListener {
            createFile.launch("subscriptions.json")
            true
        }
    }

    private fun initCustomInstances(instancePref: ListPreference) {
        lifecycleScope.launchWhenCreated {
            var customInstances = listOf<CustomInstance>()
            Thread {
                customInstances = DatabaseHolder.db.customInstanceDao().getAll()
            }.await()

            val instanceNames = arrayListOf<String>()
            val instanceValues = arrayListOf<String>()

            // fetch official public instances

            val response = try {
                RetrofitInstance.api.getInstances("https://instances.tokhmi.xyz/")
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }

            response.forEach {
                if (it.name != null && it.api_url != null) {
                    instanceNames += it.name!!
                    instanceValues += it.api_url!!
                }
            }

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

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }
}
