package com.github.libretube.compat

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainThread

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
            try {
                activity.setPictureInPictureParams(params.toPictureInPictureParams())
            } catch (e: IllegalStateException) {
                // some devices claim to support PiP, but produce an exception when using PiP
                // https://github.com/libre-tube/LibreTube/issues/8163
                Log.e(TAG(), e.stackTraceToString())
                activity.toastFromMainThread(e.localizedMessage.orEmpty())
            }
        }
    }

    fun enterPictureInPictureMode(activity: Activity, params: PictureInPictureParamsCompat) {
        if (isPictureInPictureAvailable(activity)) {
            activity.enterPictureInPictureMode(params.toPictureInPictureParams())
        }
    }
}
