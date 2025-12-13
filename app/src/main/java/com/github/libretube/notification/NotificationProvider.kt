package com.github.libretube.notification

import android.app.Notification
import com.github.libretube.enums.ImportState
import java.util.UUID

interface NotificationProvider {
    fun createNotification(): Notification
    fun updateState(
        currentState: Int = 0,
        finalState: Int = 0,
        importState: ImportState = ImportState.RESUME
    ): Notification
}