package com.github.libretube

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.blankj.utilcode.util.UriUtils
import com.github.libretube.obj.Subscribe
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipFile


class Settings : PreferenceFragmentCompat() {

    companion object {
        lateinit var getContent: ActivityResultLauncher<String>
        lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            try {
                if (uri != null) {
                    var zipfile = ZipFile(UriUtils.uri2File(uri))

                    var zipentry =
                        zipfile.getEntry("Takeout/YouTube and YouTube Music/subscriptions/subscriptions.csv")

                    var inputStream = zipfile.getInputStream(zipentry)

                    val baos = ByteArrayOutputStream()

                    inputStream.use { it.copyTo(baos) }

                    var subscriptions = baos.toByteArray().decodeToString()

                    var subscribedCount = 0

                    for (text in subscriptions.lines()) {
                        if (text.take(24) != "Channel Id,Channel Url,C" && !text.take(24)
                                .isEmpty()
                        ) {
                            subscribe(text.take(24))
                            subscribedCount++
                            Log.d(TAG, "subscribed: " + text + " total: " + subscribedCount)
                        }
                    }

                    Toast.makeText(
                        context,
                        "Subscribed to " + subscribedCount + " channels.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: java.lang.Exception) {
                Toast.makeText(this.requireContext(), "Failed: " + e.message, Toast.LENGTH_SHORT)
                    .show()
            }

        }

        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Log.d(TAG, "onCreate: already granted")
                } else {
                    Log.d(TAG, "onCreate: failed")
                }
            }

        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        val instance = findPreference<ListPreference>("instance")
        fetchInstance()
        instance?.setOnPreferenceChangeListener { _, newValue ->
            RetrofitInstance.url = newValue.toString()
            RetrofitInstance.lazyMgr.reset()
            val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
            if (sharedPref?.getString("token", "") != "") {
                with(sharedPref!!.edit()) {
                    putString("token", "")
                    apply()
                }
                Toast.makeText(context, R.string.loggedout, Toast.LENGTH_SHORT).show()
            }
            true
        }

        val login = findPreference<Preference>("login_register")
        login?.setOnPreferenceClickListener {
            val newFragment = LoginDialog()
            newFragment.show(childFragmentManager, "Login")
            true
        }

        val importFromYt = findPreference<Preference>("import_from_yt")
        importFromYt?.setOnPreferenceClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    val intent = Intent()
                    intent.action =
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                    val uri =
                        Uri.fromParts("package", this.requireActivity().getPackageName(), null)
                    intent.data = uri
                    startActivity(intent)
                }
            } else {
                val intent = Intent()
                intent.action =
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                val uri =
                    Uri.fromParts("package", this.requireActivity().getPackageName(), null)
                intent.data = uri
                startActivity(intent)
            }


            ActivityCompat.requestPermissions(
                this.requireActivity(),
                arrayOf(
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 1
            )

            getContent.launch("application/zip")

            true
        }

        val themeToggle = findPreference<ListPreference>("theme_togglee")
        themeToggle?.setOnPreferenceChangeListener { _, newValue ->
            when (newValue.toString()) {
                "A" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                "L" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "D" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            true
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
            val entries = listEntries.toTypedArray<CharSequence>()
            val entryValues = listEntryValues.toTypedArray<CharSequence>()
            runOnUiThread {
                val instance = findPreference<ListPreference>("instance")
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

    private fun subscribe(channel_id: String) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.subscribe(
                        sharedPref?.getString("token", "")!!,
                        Subscribe(channel_id)
                    )
                } catch (e: IOException) {
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response$e")
                    return@launchWhenCreated
                }
            }
        }
        run()
    }
}

