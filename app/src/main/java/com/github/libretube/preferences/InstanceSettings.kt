package com.github.libretube.preferences

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.github.libretube.R
import com.github.libretube.dialogs.CustomInstanceDialog
import com.github.libretube.dialogs.LoginDialog
import com.github.libretube.requireMainActivityRestart
import com.github.libretube.util.RetrofitInstance
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import org.json.JSONObject
import org.json.JSONTokener
import retrofit2.HttpException

class InstanceSettings : PreferenceFragmentCompat() {
    val TAG = "InstanceSettings"

    override fun onCreate(savedInstanceState: Bundle?) {
        MainSettings.getContent =
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
                                    if (channelId.length == 24)
                                        channels.add(channelId)
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

        val topBarTextView = activity?.findViewById<TextView>(R.id.topBar_textView)
        topBarTextView?.text = getString(R.string.instance)

        val instance = findPreference<ListPreference>("selectInstance")
        // fetchInstance()
        initCustomInstances()
        instance?.setOnPreferenceChangeListener { _, newValue ->
            requireMainActivityRestart = true
            RetrofitInstance.url = newValue.toString()
            RetrofitInstance.lazyMgr.reset()
            logout()
            true
        }

        val customInstance = findPreference<Preference>("customInstance")
        customInstance?.setOnPreferenceClickListener {
            val newFragment = CustomInstanceDialog()
            newFragment.show(childFragmentManager, "CustomInstanceDialog")
            true
        }

        val clearCustomInstances = findPreference<Preference>("clearCustomInstances")
        clearCustomInstances?.setOnPreferenceClickListener {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            sharedPreferences.edit()
                .remove("custom_instances_name")
                .remove("custom_instances_url")
                .commit()
            activity?.recreate()
            true
        }

        val login = findPreference<Preference>("login_register")
        login?.setOnPreferenceClickListener {
            requireMainActivityRestart = true
            val newFragment = LoginDialog()
            newFragment.show(childFragmentManager, "Login")
            true
        }

        val importFromYt = findPreference<Preference>("import_from_yt")
        importFromYt?.setOnPreferenceClickListener {
            val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
            val token = sharedPref?.getString("token", "")!!
            // check StorageAccess
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Log.d("myz", "" + Build.VERSION.SDK_INT)
                if (ContextCompat.checkSelfPermission(
                        this.requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this.requireActivity(),
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.MANAGE_EXTERNAL_STORAGE
                        ),
                        1
                    ) // permission request code is just an int
                } else if (token != "") {
                    MainSettings.getContent.launch("*/*")
                } else {
                    Toast.makeText(context, R.string.login_first, Toast.LENGTH_SHORT).show()
                }
            } else {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this.requireActivity(),
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        1
                    )
                } else if (token != "") {
                    MainSettings.getContent.launch("*/*")
                } else {
                    Toast.makeText(context, R.string.login_first, Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
    }

    private fun initCustomInstances() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // get the names of the custom instances
        val customInstancesNames = try {
            sharedPreferences
                .getStringSet("custom_instances_name", HashSet())!!.toList()
        } catch (e: Exception) {
            emptyList()
        }

        // get the urls of the custom instances
        val customInstancesUrls = try {
            sharedPreferences
                .getStringSet("custom_instances_url", HashSet())!!.toList()
        } catch (e: Exception) {
            emptyList()
        }

        val instanceNames = resources.getStringArray(R.array.instances) + customInstancesNames
        val instanceValues = resources.getStringArray(R.array.instancesValue) + customInstancesUrls

        // add custom instances to the list preference
        val instance = findPreference<ListPreference>("selectInstance")
        instance?.entries = instanceNames
        instance?.entryValues = instanceValues
        instance?.summaryProvider =
            Preference.SummaryProvider<ListPreference> { preference ->
                val text = preference.entry
                if (TextUtils.isEmpty(text)) {
                    "kavin.rocks (Official)"
                } else {
                    text
                }
            }
    }

    private fun logout() {
        val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
        val token = sharedPref?.getString("token", "")
        if (token != "") {
            with(sharedPref!!.edit()) {
                putString("token", "")
                apply()
            }
            Toast.makeText(context, R.string.loggedout, Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchInstance() {
        lifecycleScope.launchWhenCreated {
            val response = try {
                RetrofitInstance.api.getInstances("https://instances.tokhmi.xyz/")
            } catch (e: IOException) {
                println(e)
                Log.e("settings", "IOException, you might not have internet connection")
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e("settings", "HttpException, unexpected response $e")
                return@launchWhenCreated
            } catch (e: Exception) {
                Log.e("settings", e.toString())
                return@launchWhenCreated
            }
            val listEntries: MutableList<String> = ArrayList()
            val listEntryValues: MutableList<String> = ArrayList()
            for (item in response) {
                listEntries.add(item.name!!)
                listEntryValues.add(item.api_url!!)
            }

            // add custom instances to the list

            val entries = listEntries.toTypedArray<CharSequence>()
            val entryValues = listEntryValues.toTypedArray<CharSequence>()
            runOnUiThread {
                val instance = findPreference<ListPreference>("selectInstance")
                instance?.entries = entries
                instance?.entryValues = entryValues
                instance?.summaryProvider =
                    Preference.SummaryProvider<ListPreference> { preference ->
                        val text = preference.entry
                        if (TextUtils.isEmpty(text)) {
                            "kavin.rocks (Official)"
                        } else {
                            text
                        }
                    }
            }
        }
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }

    private fun subscribe(channels: List<String>) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref =
                        context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.importSubscriptions(
                        false,
                        sharedPref?.getString("token", "")!!,
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
