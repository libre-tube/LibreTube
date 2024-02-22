package com.github.libretube.helpers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.ui.sheets.IntentChooserSheet

object IntentHelper {
    private fun getResolveIntent(link: String) = Intent(Intent.ACTION_VIEW)
        .setData(link.toUri())
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun getResolveInfo(context: Context, link: String): List<ResolveInfo> {
        val intent = getResolveIntent(link)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager
                .queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
        } else {
            context.packageManager
                .queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
    }

    fun openLinkFromHref(context: Context, fragmentManager: FragmentManager, link: String) {
        val resolveInfoList = getResolveInfo(context, link)

        if (resolveInfoList.isEmpty()) {
            try {
                context.startActivity(getResolveIntent(link))
            } catch (e: Exception) {
                context.toastFromMainThread(R.string.error)
            }
        } else {
            IntentChooserSheet()
                .apply { arguments = bundleOf(IntentData.url to link) }
                .show(fragmentManager)
        }
    }

    fun openWithExternalPlayer(context: Context, uri: Uri, title: String?, uploader: String?) {
        // start an intent with video as mimetype using the hls stream
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra("title", title)
            putExtra("artist", uploader)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.no_player_found, Toast.LENGTH_SHORT).show()
        }
    }
}
