package com.github.libretube

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.blankj.utilcode.util.UriUtils
import retrofit2.HttpException
import java.io.*
import java.util.zip.ZipFile


class Settings : PreferenceFragmentCompat() {
    val TAG = "Settings"
    companion object {
        lateinit var getContent: ActivityResultLauncher<String>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

            if (uri != null) {
                try{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                        // Open a specific media item using ParcelFileDescriptor.
                        val resolver: ContentResolver =
                            requireActivity()
                                .contentResolver

                        // "rw" for read-and-write;
                        // "rwt" for truncating or overwriting existing file contents.
                        val readOnlyMode = "r"
                        // uri - I have got from onActivityResult
                        //uri = data.getData();
                        val parcelFile = resolver.openFileDescriptor(uri, readOnlyMode)
                        val fileReader = FileReader(parcelFile!!.fileDescriptor)
                        val reader = BufferedReader(fileReader)
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            Log.d(TAG,reader.readLine())
                        }
                        reader.close()
                        fileReader.close()
                    }else{
                        Log.d(TAG,UriUtils.uri2File(uri).toString())
                        val file = UriUtils.uri2File(uri)
                        var inputStream: InputStream? = null
                        if (file.extension == "zip") {
                            var zipfile = ZipFile(file)

                            var zipentry =
                                zipfile.getEntry("Takeout/YouTube and YouTube Music/subscriptions/subscriptions.csv")

                            inputStream = zipfile.getInputStream(zipentry)
                        }else if(file.extension == "csv"){
                            inputStream = file.inputStream()
                        }
                        val baos = ByteArrayOutputStream()

                        inputStream?.use { it.copyTo(baos) }

                        var subscriptions = baos.toByteArray().decodeToString()
                        var channels: MutableList<String> = emptyList<String>().toMutableList()
                        var subscribedCount = 0
                        for (text in subscriptions.lines().subList(1,subscriptions.lines().size)) {
                            if (text.replace(" ","") != "") {
                                val channel = text.split(",")[0]
                                channels.add(channel)
                                subscribedCount++
                                Log.d(TAG, "subscribed: " + text + " total: " + subscribedCount)
                            }
                        }
                        subscribe(channels)
                    }
            }catch (e: Exception){
                    Log.e(TAG,e.toString())
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
            val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
            val token = sharedPref?.getString("token","")!!
            //check StorageAccess
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Log.d("myz", "" + Build.VERSION.SDK_INT)
                if (ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this.requireActivity(), arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.MANAGE_EXTERNAL_STORAGE
                        ), 1
                    ) //permission request code is just an int
                }else if (token != ""){
                    getContent.launch("*/*")
                }else{
                    Toast.makeText(context, R.string.login_first, Toast.LENGTH_SHORT).show()
                }
            } else {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
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
                }else if (token != ""){
                    getContent.launch("*/*")
                }else{
                    Toast.makeText(context, R.string.login_first, Toast.LENGTH_SHORT).show()
                }
            }
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


    private fun subscribe(channels: List<String>) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.importSubscriptions(sharedPref?.getString("token", "")!!,channels)
                } catch (e: IOException) {
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response$e")
                    return@launchWhenCreated
                }
                if(response.message == "ok"){
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


