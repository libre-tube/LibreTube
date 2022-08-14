package com.github.libretube.api

import android.util.Log
import com.github.libretube.extensions.TAG
import com.github.libretube.obj.Subscribe
import com.github.libretube.preferences.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SubscriptionHelper {

    fun subscribe(channelId: String) {
        if (PreferenceHelper.getToken() != "") {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitInstance.authApi.subscribe(
                        PreferenceHelper.getToken(),
                        Subscribe(channelId)
                    )
                } catch (e: Exception) {
                    Log.e(TAG(), e.toString())
                }
            }
        } else {
            val channels = PreferenceHelper.getLocalSubscriptions().toMutableList()
            channels.add(channelId)
            PreferenceHelper.setLocalSubscriptions(channels)
        }
    }

    fun unsubscribe(channelId: String) {
        if (PreferenceHelper.getToken() != "") {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitInstance.authApi.unsubscribe(
                        PreferenceHelper.getToken(),
                        Subscribe(channelId)
                    )
                } catch (e: Exception) {
                    Log.e(TAG(), e.toString())
                }
            }
        } else {
            val channels = PreferenceHelper.getLocalSubscriptions().toMutableList()
            channels.remove(channelId)
            PreferenceHelper.setLocalSubscriptions(channels)
        }
    }

    suspend fun isSubscribed(channelId: String): Boolean? {
        if (PreferenceHelper.getToken() != "") {
            val isSubscribed = try {
                RetrofitInstance.authApi.isSubscribed(
                    channelId,
                    PreferenceHelper.getToken()
                )
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                return null
            }
            return isSubscribed.subscribed
        } else {
            return PreferenceHelper.getLocalSubscriptions().contains(channelId)
        }
    }

    suspend fun importSubscriptions(newChannels: List<String>) {
        if (PreferenceHelper.getToken() != "") {
            try {
                val token = PreferenceHelper.getToken()
                RetrofitInstance.authApi.importSubscriptions(
                    false,
                    token,
                    newChannels
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val channels = PreferenceHelper.getLocalSubscriptions().toMutableList()
            newChannels.forEach {
                if (!channels.contains(it)) channels += it
            }
            PreferenceHelper.setLocalSubscriptions(channels)
        }
    }

    fun getFormattedLocalSubscriptions(): String {
        return PreferenceHelper.getLocalSubscriptions().joinToString(",")
    }
}
