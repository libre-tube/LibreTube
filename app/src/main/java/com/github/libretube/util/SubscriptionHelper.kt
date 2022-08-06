package com.github.libretube.util

import android.util.Log
import com.github.libretube.obj.Subscribe
import com.github.libretube.preferences.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SubscriptionHelper {
    val TAG = "SubscriptionHelper"

    fun subscribe(channelId: String) {
        if (PreferenceHelper.getToken() != "") {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitInstance.authApi.subscribe(
                        PreferenceHelper.getToken(),
                        Subscribe(channelId)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
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
                    Log.e(TAG, e.toString())
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
                Log.e(TAG, e.toString())
                return null
            }
            return isSubscribed.subscribed
        } else {
            return PreferenceHelper.getLocalSubscriptions().contains(channelId)
        }
    }

    suspend fun importSubscriptions(channels: List<String>) {
        if (PreferenceHelper.getToken() != "") {
            val response = try {
                val token = PreferenceHelper.getToken()
                RetrofitInstance.authApi.importSubscriptions(
                    false,
                    token,
                    channels
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val newChannels = PreferenceHelper.getLocalSubscriptions().toMutableList() + channels
            PreferenceHelper.setLocalSubscriptions(newChannels)
        }
    }

    fun getFormattedLocalSubscriptions(): String {
        return PreferenceHelper.getLocalSubscriptions().joinToString(",")
    }
}
