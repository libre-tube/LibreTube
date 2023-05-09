package com.github.libretube.helpers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import com.github.libretube.R
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.ui.sheets.IntentChooserSheet

object IntentHelper {
    fun openLinkFromHref(context: Context, fragmentManager: FragmentManager, link: String) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(link.toUri())
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        @Suppress("DEPRECATION")
        val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager
                .queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
                )
        } else {
            context.packageManager
                .queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }

        if (resolveInfoList.isEmpty()) {
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                context.toastFromMainThread(R.string.error)
            }
        } else {
            IntentChooserSheet(resolveInfoList, link)
                .show(fragmentManager)
        }
    }
}
