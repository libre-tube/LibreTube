package com.github.libretube.util

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.R
import com.github.libretube.obj.NewPipeSubscriptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ImportHelper(
    private val activity: AppCompatActivity
) {
    private val TAG = "ImportHelper"

    fun importSubscriptions() {
        getContent.launch("*/*")
    }

    val getContent =
        activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                try {
                    val type = activity.contentResolver.getType(uri)

                    var inputStream: InputStream? = activity.contentResolver.openInputStream(uri)
                    var channels = ArrayList<String>()
                    if (type == "application/json") {
                        val mapper = ObjectMapper()
                        val json = readTextFromUri(uri)
                        val subscriptions = mapper.readValue(json, NewPipeSubscriptions::class.java)
                        channels = subscriptions.subscriptions?.map {
                            it.url.replace("https://www.youtube.com/channel/", "")
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
                    inputStream?.close()

                    CoroutineScope(Dispatchers.IO).launch {
                        SubscriptionHelper.importSubscriptions(channels)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                    Toast.makeText(
                        activity,
                        R.string.error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
}
