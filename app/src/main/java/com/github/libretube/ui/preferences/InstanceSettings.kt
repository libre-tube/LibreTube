package com.github.libretube.ui.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.PipedMediaServiceRepository
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.PipedInstance
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.SimpleOptionsRecyclerBinding
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.adapters.InstancesAdapter
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.CreateCustomInstanceDialog
import com.github.libretube.ui.dialogs.DeleteAccountDialog
import com.github.libretube.ui.dialogs.LoginDialog
import com.github.libretube.ui.dialogs.LogoutDialog
import com.github.libretube.ui.models.InstancesModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class InstanceSettings : BasePreferenceFragment() {
    private val token get() = PreferenceHelper.getToken()
    private var instances = mutableListOf<PipedInstance>()
    private val customInstancesModel: InstancesModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.instance_settings, rootKey)

        val instancePref = findPreference<ListPreference>(PreferenceKeys.FETCH_INSTANCE)!!
        val authInstance = findPreference<ListPreference>(PreferenceKeys.AUTH_INSTANCE)!!
        val instancePrefs = listOf(instancePref, authInstance)

        lifecycleScope.launch {
            customInstancesModel.customInstances.collect { updatedInstances ->
                instances =
                    updatedInstances.map { PipedInstance(it.name, it.apiUrl) }.toMutableList()
                // update the instances to also show custom ones
                initInstancesPref(instancePrefs)
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

        val youTubeDataSource = findPreference<ListPreference>(PreferenceKeys.YOUTUBE_DATA_SOURCE)!!
        val localReturnYouTubeDislike = findPreference<SwitchPreferenceCompat>(PreferenceKeys.LOCAL_RYD)!!
        val instanceCategory = findPreference<PreferenceCategory>("instance_category")!!

        localReturnYouTubeDislike.isVisible = youTubeDataSource.value != "piped"
        instanceCategory.isVisible = youTubeDataSource.value == "piped"
        youTubeDataSource.setOnPreferenceChangeListener { _, newValue ->
            localReturnYouTubeDislike.isVisible = newValue != "piped"
            instanceCategory.isVisible = newValue == "piped"

            true
        }

        val syncServerType = findPreference<ListPreference>(PreferenceKeys.SYNC_SERVER_TYPE)!!
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

    private fun initInstancesPref(instancePrefs: List<ListPreference>) = runCatching {
        // add the currently used instances to the list if they're currently down / not part
        // of the public instances list
        for (apiUrl in listOf(PipedMediaServiceRepository.apiUrl, RetrofitInstance.pipedAuthUrl)) {
            if (instances.none { it.apiUrl == apiUrl }) {
                val origin = apiUrl.toHttpUrl().host
                instances.add(PipedInstance(origin, apiUrl, isCurrentlyDown = true))
            }
        }

        instances.sortBy { it.name }

        // If any preference dialog is visible in this fragment, it's one of the instance selection
        // dialogs. In order to prevent UX issues, we don't update the instances list then.
        if (isDialogVisible) return@runCatching

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
        var selectedInstance = preference.value
        val selectedIndex = instances.indexOfFirst { it.apiUrl == selectedInstance }

        val layoutInflater = LayoutInflater.from(context)
        val binding = SimpleOptionsRecyclerBinding.inflate(layoutInflater)
        binding.optionsRecycler.layoutManager = LinearLayoutManager(context)

        val instances = ImmutableList.copyOf(this.instances)
        binding.optionsRecycler.adapter = InstancesAdapter(selectedIndex) {
            selectedInstance = instances[it].apiUrl
        }.also { it.submitList(instances) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(preference.title)
            .setView(binding.root)
            .setNeutralButton(R.string.addInstance) { _, _ ->
                CreateCustomInstanceDialog().show(childFragmentManager, null)
            }
            .setPositiveButton(R.string.okay) { _, _ ->
                preference.value = selectedInstance
                resetForNewInstance()
            }
            .show()
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
