package com.github.libretube.compat

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object PictureInPictureCompat {
    fun isPictureInPictureAvailable(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

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
