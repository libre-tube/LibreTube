package com.github.libretube.compat

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager

object PictureInPictureCompat {
    /**
     * Returns whether the system supports Picture-in-Picture mode.
     */
    fun isPictureInPictureAvailable(context: Context) =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    /**
     * Returns whether the user has enabled Picture-in-Picture mode.
     */
    fun isPictureInPictureEnabled(context: Context) =
        (context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager?)?.checkOpNoThrow(
            AppOpsManager.OPSTR_PICTURE_IN_PICTURE, android.os.Process.myUid(), context.packageName
        ) == AppOpsManager.MODE_ALLOWED


    fun isInPictureInPictureMode(activity: Activity) = activity.isInPictureInPictureMode

    fun setPictureInPictureParams(activity: Activity, params: PictureInPictureParamsCompat) {
        if (isPictureInPictureAvailable(activity)) {
            activity.setPictureInPictureParams(params.toPictureInPictureParams())
        }
    }

    fun enterPictureInPictureMode(activity: Activity, params: PictureInPictureParamsCompat) {
        if (isPictureInPictureAvailable(activity)) {
            activity.enterPictureInPictureMode(params.toPictureInPictureParams())
        }
    }
}
