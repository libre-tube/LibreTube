package com.github.libretube.util

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.extensions.TAG
import com.github.libretube.obj.NewPipeSubscription
import com.github.libretube.obj.NewPipeSubscriptions
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ImportHelper(private val activity: Activity) {
    /**
     * Import subscriptions by a file uri
     */
    fun importSubscriptions(uri: Uri?) {
        if (uri == null) return
        try {
            val channels = when (activity.contentResolver.getType(uri)) {
                "application/json" -> {
                    // NewPipe subscriptions format
                    val mapper = ObjectMapper()
                    val json = activity.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader().use { reader -> reader.readText() }
                    }.orEmpty()

                    val subscriptions = mapper.readValue(json, NewPipeSubscriptions::class.java)
                    subscriptions.subscriptions.orEmpty().map {
                        it.url!!.replace("https://www.youtube.com/channel/", "")
                    }
                }
                "text/csv", "text/comma-separated-values" -> {
                    // import subscriptions from Google/YouTube Takeout
                    activity.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader().useLines { lines ->
                            lines.map { line -> line.substringBefore(",") }
                                .filter { channelId -> channelId.length == 24 }
                                .toList()
                        }
                    }.orEmpty()
                }
                else -> throw IllegalArgumentException("Unsupported file type")
            }

            CoroutineScope(Dispatchers.IO).launch {
                SubscriptionHelper.importSubscriptions(channels)
            }

            Toast.makeText(activity, R.string.importsuccess, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
            Toast.makeText(
                activity,
                R.string.error,
                Toast.LENGTH_SHORT
            ).show()
        }
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
                val subs = if (token != "") {
                    RetrofitInstance.authApi.subscriptions(token)
                } else {
                    RetrofitInstance.authApi.unauthenticatedSubscriptions(
                        SubscriptionHelper.getFormattedLocalSubscriptions()
                    )
                }
                val newPipeChannels = mutableListOf<NewPipeSubscription>()
                subs.forEach {
                    newPipeChannels += NewPipeSubscription(
                        name = it.name,
                        service_id = 0,
                        url = "https://www.youtube.com" + it.url
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
