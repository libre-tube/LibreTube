package com.github.libretube

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.github.libretube.adapters.TrendingAdapter
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException

class Settings : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        val instance = findPreference<ListPreference>("instance")
        fetchInstance()
        instance?.setOnPreferenceChangeListener { preference, newValue ->
            RetrofitInstance.url = newValue.toString()
            RetrofitInstance.lazyMgr.reset()
            val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
            if(sharedPref?.getString("token","")!="") {
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
                newFragment.show(childFragmentManager, "fuck")
                true
            }

        }

    private fun fetchInstance() {
        val api: PipedApi by lazy{
            Retrofit.Builder()
                .baseUrl("https://raw.githubusercontent.com/wiki/TeamPiped/Piped-Frontend/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(PipedApi::class.java)
        }
        lifecycleScope.launchWhenCreated {
            val response = try {
                api.getInstances()
            } catch (e: IOException) {
                println(e)
                Log.e("settings", "IOException, you might not have internet connection")
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e("settings", "HttpException, unexpected response ${e.toString()}")
                return@launchWhenCreated
            } catch (e: Exception){
                Log.e("settings",e.toString())
                return@launchWhenCreated
            }
            //println("dafaq $response")
            val listEntries: MutableList<String> = ArrayList()
            val listEntryValues: MutableList<String> = ArrayList()
            var skipped = 0
            val lines = response.split("\n")
            for(line in lines) {
                val split = line.split("|")
                if (split.size == 5) {
                    if (skipped < 2) {
                        skipped++
                    }else{
                        println("dafaq $line")
                        listEntries.add(split[0])
                        listEntryValues.add(split[1])
                    }
                }

            }
            val entries = listEntries.toTypedArray<CharSequence>()
            val entryValues = listEntryValues.toTypedArray<CharSequence>()
            runOnUiThread {
                val instance = findPreference<ListPreference>("instance")
                instance?.entries = entries
                instance?.entryValues = entryValues
                instance?.summaryProvider = Preference.SummaryProvider<ListPreference> { preference ->
                    val text = preference.entry
                    if (TextUtils.isEmpty(text)) {
                        "Not set"
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
}
