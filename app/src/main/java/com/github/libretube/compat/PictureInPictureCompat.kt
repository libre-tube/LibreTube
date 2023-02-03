package com.github.libretube.compat

import android.app.Activity
import android.os.Build

object PictureInPictureCompat {
    fun isInPictureInPictureMode(activity: Activity): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity.isInPictureInPictureMode
    }

    fun setPictureInPictureParams(activity: Activity, params: PictureInPictureParamsCompat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.setPictureInPictureParams(params.toPictureInPictureParams())
        }
    }

    fun enterPictureInPictureMode(activity: Activity, params: PictureInPictureParamsCompat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.enterPictureInPictureMode(params.toPictureInPictureParams())
        }
    }
}
