package com.github.libretube.api

import android.content.Context
import android.util.Log
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Subscribe
import com.github.libretube.api.obj.Subscription
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.db.obj.LocalSubscription
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.awaitQuery
import com.github.libretube.extensions.query
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
            query {
                Database.localSubscriptionDao().insertAll(
                    LocalSubscription(channelId)
                )
            }
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
            query {
                Database.localSubscriptionDao().delete(
                    LocalSubscription(channelId)
                )
            }
        }
    }

    fun handleUnsubscribe(
        context: Context,
        channelId: String,
        channelName: String?,
        onUnsubscribe: () -> Unit
    ) {
        if (!PreferenceHelper.getBoolean(PreferenceKeys.CONFIRM_UNSUBSCRIBE, false)) {
            unsubscribe(channelId)
            onUnsubscribe.invoke()
            return
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.unsubscribe)
            .setMessage(context.getString(R.string.confirm_unsubscribe, channelName))
            .setPositiveButton(R.string.unsubscribe) { _, _ ->
                unsubscribe(channelId)
                onUnsubscribe.invoke()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
            return awaitQuery {
                Database.localSubscriptionDao().includes(channelId)
            }
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
            val newLocalSubscriptions = mutableListOf<LocalSubscription>()
            newChannels.forEach {
                newLocalSubscriptions += LocalSubscription(channelId = it)
            }
            query {
                Database.localSubscriptionDao().insertAll(
                    *newChannels.map { LocalSubscription(it) }.toTypedArray()
                )
            }
        }
    }

    private fun getLocalSubscriptions(): List<LocalSubscription> {
        return awaitQuery {
            Database.localSubscriptionDao().getAll()
        }
    }

    fun getFormattedLocalSubscriptions(): String {
        val localSubscriptions = getLocalSubscriptions()
        return localSubscriptions.joinToString(",") { it.channelId }
    }

    suspend fun getSubscriptions(): List<Subscription> {
        return if (PreferenceHelper.getToken() != "") {
            RetrofitInstance.authApi.subscriptions(
                PreferenceHelper.getToken()
            )
        } else {
            RetrofitInstance.authApi.unauthenticatedSubscriptions(
                getFormattedLocalSubscriptions()
            )
        }
    }

    suspend fun getFeed(): List<StreamItem> {
        return if (PreferenceHelper.getToken() != "") {
            RetrofitInstance.authApi.getFeed(
                PreferenceHelper.getToken()
            )
        } else {
            RetrofitInstance.authApi.getUnauthenticatedFeed(
                getFormattedLocalSubscriptions()
            )
        }
    }
}
