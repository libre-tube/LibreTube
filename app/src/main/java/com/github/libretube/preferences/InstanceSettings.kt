package com.github.libretube.preferences

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.dialogs.CustomInstanceDialog
import com.github.libretube.dialogs.DeleteAccountDialog
import com.github.libretube.dialogs.LoginDialog
import com.github.libretube.dialogs.LogoutDialog
import com.github.libretube.dialogs.RequireRestartDialog
import com.github.libretube.util.PermissionHelper
import com.github.libretube.util.RetrofitInstance
import org.json.JSONObject
import org.json.JSONTokener
import retrofit2.HttpException
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class InstanceSettings : PreferenceFragmentCompat() {
    val TAG = "InstanceSettings"

    companion object {
        lateinit var getContent: ActivityResultLauncher<String>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        getContent =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri != null) {
                    try {
                        // Open a specific media item using ParcelFileDescriptor.
                        val resolver: ContentResolver =
                            requireActivity()
                                .contentResolver

                        // "rw" for read-and-write;
                        // "rwt" for truncating or overwriting existing file contents.
                        // val readOnlyMode = "r"
                        // uri - I have got from onActivityResult
                        val type = resolver.getType(uri)

                        var inputStream: InputStream? = resolver.openInputStream(uri)
                        val channels = ArrayList<String>()
                        if (type == "application/json") {
                            val json = inputStream?.bufferedReader()?.readLines()?.get(0)
                            val jsonObject = JSONTokener(json).nextValue() as JSONObject
                            Log.e(TAG, jsonObject.getJSONArray("subscriptions").toString())
                            for (
                            i in 0 until jsonObject.getJSONArray("subscriptions")
                                .length()
                            ) {
                                var url =
                                    jsonObject.getJSONArray("subscriptions").getJSONObject(i)
                                        .getString("url")
                                url = url.replace("https://www.youtube.com/channel/", "")
                                Log.e(TAG, url)
                                channels.add(url)
                            }
                        } else {
                            if (type == "application/zip") {
                                val zis = ZipInputStream(inputStream)
                                var entry: ZipEntry? = zis.nextEntry
                                while (entry != null) {
                                    if (entry.name.endsWith(".csv")) {
                                        inputStream = zis
                                        break
                                    }
                                    entry = zis.nextEntry
                                }
                            }

                            inputStream?.bufferedReader()?.readLines()?.forEach {
                                if (it.isNotBlank()) {
                                    val channelId = it.substringBefore(",")
                                    if (channelId.length == 24) {
                                        channels.add(channelId)
                                    }
                                }
                            }
                        }
                        inputStream?.close()

                        subscribe(channels)
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                        Toast.makeText(
                            context,
                            R.string.error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.instance_settings, rootKey)

        val settingsActivity = activity as SettingsActivity
        settingsActivity.changeTopBarText(getString(R.string.instance))

        val instance = findPreference<ListPreference>(PreferenceKeys.FETCH_INSTANCE)
        // fetchInstance()
        initCustomInstances(instance!!)
        instance.setOnPreferenceChangeListener { _, newValue ->
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
            RetrofitInstance.url = newValue.toString()
            if (!PreferenceHelper.getBoolean(PreferenceKeys.AUTH_INSTANCE_TOGGLE, false)) {
                RetrofitInstance.authUrl = newValue.toString()
                logout()
            }
            RetrofitInstance.lazyMgr.reset()
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
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
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
            val restartDialog = RequireRestartDialog()
            restartDialog.show(childFragmentManager, "RequireRestartDialog")
            true
        }

        val customInstance = findPreference<Preference>(PreferenceKeys.CUSTOM_INSTANCE)
        customInstance?.setOnPreferenceClickListener {
            val newFragment = CustomInstanceDialog()
            newFragment.show(childFragmentManager, "CustomInstanceDialog")
            true
        }

        val clearCustomInstances = findPreference<Preference>(PreferenceKeys.CLEAR_CUSTOM_INSTANCES)
        clearCustomInstances?.setOnPreferenceClickListener {
            PreferenceHelper.removePreference("customInstances")
            val intent = Intent(context, SettingsActivity::class.java)
            startActivity(intent)
            true
        }

        val login = findPreference<Preference>(PreferenceKeys.LOGIN_REGISTER)
        val token = PreferenceHelper.getToken()
        if (token != "") login?.setTitle(R.string.logout)
        login?.setOnPreferenceClickListener {
            if (token == "") {
                val newFragment = LoginDialog()
                newFragment.show(childFragmentManager, "Login")
            } else {
                val newFragment = LogoutDialog()
                newFragment.show(childFragmentManager, "Logout")
            }

            true
        }

        val deleteAccount = findPreference<Preference>(PreferenceKeys.DELETE_ACCOUNT)
        deleteAccount?.setOnPreferenceClickListener {
            val token = PreferenceHelper.getToken()
            if (token != "") {
                val newFragment = DeleteAccountDialog()
                newFragment.show(childFragmentManager, "DeleteAccountDialog")
            } else {
                Toast.makeText(context, R.string.login_first, Toast.LENGTH_SHORT).show()
            }
            true
        }

        val importFromYt = findPreference<Preference>(PreferenceKeys.IMPORT_SUBS)
        importFromYt?.setOnPreferenceClickListener {
            importSubscriptions()
            true
        }
    }

    private fun initCustomInstances(instancePref: ListPreference) {
        lifecycleScope.launchWhenCreated {
            val customInstances = PreferenceHelper.getCustomInstances()

            var instanceNames = arrayListOf<String>()
            var instanceValues = arrayListOf<String>()

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

    private fun importSubscriptions() {
        val token = PreferenceHelper.getToken()
        if (token != "") {
            // check StorageAccess
            val accessGranted =
                PermissionHelper.isStoragePermissionGranted(activity as AppCompatActivity)
            if (accessGranted) getContent.launch("*/*")
            else PermissionHelper.requestReadWrite(activity as AppCompatActivity)
        } else {
            Toast.makeText(context, R.string.login_first, Toast.LENGTH_SHORT).show()
        }
    }

    private fun subscribe(channels: List<String>) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val token = PreferenceHelper.getToken()
                    RetrofitInstance.authApi.importSubscriptions(
                        false,
                        token,
                        channels
                    )
                } catch (e: IOException) {
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response$e")
                    return@launchWhenCreated
                }
                if (response.message == "ok") {
                    Toast.makeText(
                        context,
                        R.string.importsuccess,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        run()
    }
}
