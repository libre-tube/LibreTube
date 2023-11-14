package com.github.libretube.api

import android.content.Context
import android.util.Log
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Subscribe
import com.github.libretube.api.obj.Subscription
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.LocalSubscription
import com.github.libretube.extensions.TAG
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.util.deArrow
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.runBlocking

object SubscriptionHelper {
    /**
     * The maximum number of channel IDs that can be passed via a GET request for fetching
     * the subscriptions list and the feed
     */
    private const val GET_SUBSCRIPTIONS_LIMIT = 100
    private val token get() = PreferenceHelper.getToken()

    suspend fun subscribe(channelId: String) {
        if (token.isNotEmpty()) {
            runCatching {
                RetrofitInstance.authApi.subscribe(token, Subscribe(channelId))
            }
        } else {
            Database.localSubscriptionDao().insert(LocalSubscription(channelId))
        }
    }

    suspend fun unsubscribe(channelId: String) {
        if (token.isNotEmpty()) {
            runCatching {
                RetrofitInstance.authApi.unsubscribe(token, Subscribe(channelId))
            }
        } else {
            Database.localSubscriptionDao().delete(LocalSubscription(channelId))
        }
    }

    fun handleUnsubscribe(
        context: Context,
        channelId: String,
        channelName: String?,
        onUnsubscribe: () -> Unit
    ) {
        if (!PreferenceHelper.getBoolean(PreferenceKeys.CONFIRM_UNSUBSCRIBE, false)) {
            runBlocking {
                unsubscribe(channelId)
                onUnsubscribe()
            }
            return
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.unsubscribe)
            .setMessage(context.getString(R.string.confirm_unsubscribe, channelName))
            .setPositiveButton(R.string.unsubscribe) { _, _ ->
                runBlocking {
                    unsubscribe(channelId)
                    onUnsubscribe()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    suspend fun isSubscribed(channelId: String): Boolean? {
        if (token.isNotEmpty()) {
            val isSubscribed = try {
                RetrofitInstance.authApi.isSubscribed(channelId, token)
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                return null
            }
            return isSubscribed.subscribed
        } else {
            return Database.localSubscriptionDao().includes(channelId)
        }
    }

    suspend fun importSubscriptions(newChannels: List<String>) {
        if (token.isNotEmpty()) {
            runCatching {
                RetrofitInstance.authApi.importSubscriptions(false, token, newChannels)
            }
        } else {
            Database.localSubscriptionDao().insertAll(newChannels.map { LocalSubscription(it) })
        }
    }

    suspend fun getSubscriptions(): List<Subscription> {
        return if (token.isNotEmpty()) {
            RetrofitInstance.authApi.subscriptions(token)
        } else {
            val subscriptions = Database.localSubscriptionDao().getAll().map { it.channelId }
            when {
                subscriptions.size > GET_SUBSCRIPTIONS_LIMIT ->
                    RetrofitInstance.authApi
                        .unauthenticatedSubscriptions(subscriptions)

                else -> RetrofitInstance.authApi.unauthenticatedSubscriptions(
                    subscriptions.joinToString(",")
                )
            }
        }
    }

    suspend fun getFeed(): List<StreamItem> {
        return if (token.isNotEmpty()) {
            RetrofitInstance.authApi.getFeed(token)
        } else {
            val subscriptions = Database.localSubscriptionDao().getAll().map { it.channelId }
            when {
                subscriptions.size > GET_SUBSCRIPTIONS_LIMIT ->
                    RetrofitInstance.authApi
                        .getUnauthenticatedFeed(subscriptions)

                else -> RetrofitInstance.authApi.getUnauthenticatedFeed(
                    subscriptions.joinToString(",")
                )
            }
        }.deArrow()
    }
}
