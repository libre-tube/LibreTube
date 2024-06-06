package com.github.libretube.ui.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.libretube.R
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.launch

@Composable
fun SubscribeButton(channelId: String) {
    val scope = rememberCoroutineScope()
    var isSubscribed by rememberSaveable { mutableStateOf(false) }
    var shouldNotify by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(true) {
        isSubscribed = SubscriptionHelper.isSubscribed(channelId) ?: false
        shouldNotify = PreferenceHelper.isChannelNotificationIgnorable(channelId)
    }

    Row {
        if (isSubscribed && PreferenceHelper.getBoolean(PreferenceKeys.NOTIFICATION_ENABLED, true)) {
            IconButton(onClick = {
                shouldNotify = !shouldNotify
                PreferenceHelper.toggleIgnorableNotificationChannel(channelId)
            }) {
                val icon = if (shouldNotify) R.drawable.ic_bell else R.drawable.ic_notification
                Icon(painter = painterResource(icon), contentDescription = null)
            }
        }
        Button(onClick = {
            scope.launch {
                if (isSubscribed) {
                    SubscriptionHelper.unsubscribe(channelId)
                } else {
                    SubscriptionHelper.subscribe(channelId)
                }
                isSubscribed = !isSubscribed
            }
        }) {
            Text(stringResource(if (isSubscribed) R.string.unsubscribe else R.string.subscribe))
        }
    }
}

@Preview
@Composable
fun PreviewSubscribeButton() {
    SubscribeButton("@CNN")
}
