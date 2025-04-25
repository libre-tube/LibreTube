package com.github.libretube.ui.extensions

import android.content.Context
import android.widget.TextView
import androidx.core.view.isVisible
import com.github.libretube.R
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.PreferenceHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    var subscribed: Boolean? = false

    fun updateUIStateAndNotifyObservers() {
        subscribed?.let { subscribed -> onIsSubscribedChange(subscribed) }

        this@setupSubscriptionButton.text =
            if (subscribed == true) context.getString(R.string.unsubscribe)
            else context.getString(R.string.subscribe)

        notificationBell?.isVisible = subscribed == true && notificationsEnabled
        this@setupSubscriptionButton.isVisible = true
    }

    CoroutineScope(Dispatchers.IO).launch {
        subscribed = isSubscribed ?: SubscriptionHelper.isSubscribed(channelId)

        withContext(Dispatchers.Main) {
            updateUIStateAndNotifyObservers()
        }
    }

    notificationBell?.setupNotificationBell(channelId)

    setOnClickListener {
        CoroutineScope(Dispatchers.Main).launch {
            if (subscribed == true) {
                val unsubscribeAction: (suspend () -> Unit) = {
                    withContext(Dispatchers.IO) {
                        SubscriptionHelper.unsubscribe(channelId)
                    }
                    subscribed = false

                    updateUIStateAndNotifyObservers()
                }

                if (PreferenceHelper.getBoolean(PreferenceKeys.CONFIRM_UNSUBSCRIBE, false)) {
                    showUnsubscribeDialog(context, channelName) {
                        CoroutineScope(Dispatchers.Main).launch { unsubscribeAction() }
                    }
                } else {
                    unsubscribeAction()
                }
            } else {
                withContext(Dispatchers.IO) {
                    SubscriptionHelper.subscribe(
                        channelId,
                        channelName,
                        channelAvatar,
                        channelVerified
                    )
                }
                subscribed = true

                updateUIStateAndNotifyObservers()
            }
        }
    }
}

fun showUnsubscribeDialog(
    context: Context,
    channelName: String?,
    onUnsubscribe: () -> Unit
) {
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.unsubscribe)
        .setMessage(context.getString(R.string.confirm_unsubscribe, channelName))
        .setPositiveButton(R.string.unsubscribe) { _, _ ->
            onUnsubscribe()
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
}