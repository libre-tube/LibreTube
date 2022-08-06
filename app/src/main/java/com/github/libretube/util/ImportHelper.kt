package com.github.libretube.util

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.R
import com.github.libretube.obj.NewPipeSubscription
import com.github.libretube.obj.NewPipeSubscriptions
import com.github.libretube.preferences.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ImportHelper(
    private val activity: Activity
) {
    private val TAG = "ImportHelper"

    fun importSubscriptions(uri: Uri?) {
        if (uri == null) return
        try {
            val type = activity.contentResolver.getType(uri)

            var inputStream: InputStream? = activity.contentResolver.openInputStream(uri)
            var channels = ArrayList<String>()
            if (type == "application/json") {
                val mapper = ObjectMapper()
                val json = readTextFromUri(uri)
                val subscriptions = mapper.readValue(json, NewPipeSubscriptions::class.java)
                channels = subscriptions.subscriptions?.map {
                    it.url?.replace("https://www.youtube.com/channel/", "")!!
                } as ArrayList<String>
            } else if (type == "application/zip") {
                val zis = ZipInputStream(inputStream)
                var entry: ZipEntry? = zis.nextEntry

                while (entry != null) {
                    if (entry.name.endsWith(".csv")) {
                        inputStream = zis
                        break
                    }
                    entry = zis.nextEntry
                    inputStream?.bufferedReader()?.readLines()?.forEach {
                        if (it.isNotBlank()) {
                            val channelId = it.substringBefore(",")
                            if (channelId.length == 24) {
                                channels.add(channelId)
                            }
                        }
                    }
                    inputStream?.close()
                }
            } else {
                throw IllegalArgumentException("unsupported type")
            }

            CoroutineScope(Dispatchers.IO).launch {
                SubscriptionHelper.importSubscriptions(channels)
            }

            Toast.makeText(activity, R.string.importsuccess, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
            Toast.makeText(
                activity,
                R.string.error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        activity.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

    /**
     * write the text to the document
     */
    fun exportSubscriptions(uri: Uri?) {
        if (uri == null) return
        try {
            val mapper = ObjectMapper()
            val token = PreferenceHelper.getToken()
            runBlocking {
                val subs = if (token != "") RetrofitInstance.authApi.subscriptions(token)
                else RetrofitInstance.authApi.unauthenticatedSubscriptions(
                    SubscriptionHelper.getFormattedLocalSubscriptions()
                )
                val newPipeChannels = mutableListOf<NewPipeSubscription>()
                subs.forEach {
                    newPipeChannels += NewPipeSubscription(
                        name = it.name,
                        service_id = 0,
                        url = "https://youtube.com" + it.url
                    )
                }

                val newPipeSubscriptions = NewPipeSubscriptions(
                    subscriptions = newPipeChannels
                )

                val data = mapper.writeValueAsBytes(newPipeSubscriptions)

                activity.contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).use { fileOutputStream ->
                        fileOutputStream.write(data)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
