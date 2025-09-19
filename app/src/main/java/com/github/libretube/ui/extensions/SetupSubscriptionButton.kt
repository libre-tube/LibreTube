package com.github.libretube.ui.extensions

import android.widget.TextView
import androidx.core.view.isVisible
import com.github.libretube.R
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.PreferenceHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


fun TextView.setupSubscriptionButton(
    channelId: String?,
    channelName: String,
    channelAvatar: String?,
    channelVerified: Boolean,
    notificationBell: MaterialButton? = null,
    isSubscribed: Boolean? = null,
    onIsSubscribedChange: (Boolean) -> Unit = {}
) {
    if (channelId == null) return

    val notificationsEnabled = PreferenceHelper
        .getBoolean(PreferenceKeys.NOTIFICATION_ENABLED, true)
    var subscribed = false

    fun updateUIStateAndNotifyObservers() {
        onIsSubscribedChange(subscribed)

        this@setupSubscriptionButton.text =
            if (subscribed) context.getString(R.string.unsubscribe)
            else context.getString(R.string.subscribe)

        notificationBell?.isVisible = subscribed && notificationsEnabled
        this@setupSubscriptionButton.isVisible = true
    }

    CoroutineScope(Dispatchers.IO).launch {
        subscribed = isSubscribed ?: SubscriptionHelper.isSubscribed(channelId) ?: false

        withContext(Dispatchers.Main) {
            updateUIStateAndNotifyObservers()
        }
    }

    notificationBell?.setupNotificationBell(channelId)

    val setSubscriptionState : (Boolean) -> Unit = { subscribe ->
        CoroutineScope(Dispatchers.IO).launch {
            if (subscribe)
                SubscriptionHelper.subscribe(
                    channelId,
                    channelName,
                    channelAvatar,
                    channelVerified
                )
            else
                SubscriptionHelper.unsubscribe(channelId)
        }
        subscribed = subscribe

        updateUIStateAndNotifyObservers()
    }

    setOnClickListener {
        CoroutineScope(Dispatchers.Main).launch {
            if (subscribed) {
                Snackbar
                    .make(
                        rootView,
                        context.getString(R.string.unsubscribe_snackbar_message, channelName),
                        1000
                    )
                    .setAction(R.string.undo, {
                        setSubscriptionState(true)
                    }).show()
            }
            setSubscriptionState(!subscribed)
        }
    }
}