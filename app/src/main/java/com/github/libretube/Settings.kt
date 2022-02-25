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
                newFragment.show(childFragmentManager, "Login")
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
                Log.e("settings", "HttpException, unexpected response ${e.toString()}")
                return@launchWhenCreated
            } catch (e: Exception){
                Log.e("settings",e.toString())
                return@launchWhenCreated
            }
            //println("dafaq $response")
            val listEntries: MutableList<String> = ArrayList()
            val listEntryValues: MutableList<String> = ArrayList()
            for(item in response){
                listEntries.add(item.name!!)
                listEntryValues.add(item.api_url!!)
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
