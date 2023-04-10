package com.github.libretube.ui.extensions

import androidx.core.view.isGone
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.PreferenceHelper
import com.google.android.material.button.MaterialButton

fun MaterialButton.setupNotificationBell(channelId: String) {
    if (!PreferenceHelper.getBoolean(PreferenceKeys.NOTIFICATION_ENABLED, true)) {
        isGone = true
        return
    }

    var isIgnorable = PreferenceHelper.isChannelNotificationIgnorable(channelId)
    setIconResource(if (isIgnorable) R.drawable.ic_bell else R.drawable.ic_notification)

    setOnClickListener {
        isIgnorable = !isIgnorable
        PreferenceHelper.toggleIgnorableNotificationChannel(channelId)
        setIconResource(if (isIgnorable) R.drawable.ic_bell else R.drawable.ic_notification)
    }
}
