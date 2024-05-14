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
import com.github.libretube.util.TextUtils.toTimeInSeconds

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

    /**
     * Resolve the uri and return a bundle with the arguments
     */
    fun resolveType(intent: Intent, uri: Uri) = with(intent) {
        val lastSegment = uri.lastPathSegment
        val secondLastSegment = uri.pathSegments.getOrNull(uri.pathSegments.size - 2)
        when {
            lastSegment == "results" -> {
                putExtra(IntentData.query, uri.getQueryParameter("search_query"))
            }
            secondLastSegment == "channel" -> {
                putExtra(IntentData.channelId, lastSegment)
            }
            secondLastSegment == "c" || secondLastSegment == "user" -> {
                putExtra(IntentData.channelName, lastSegment)
            }
            lastSegment == "playlist" -> {
                putExtra(IntentData.playlistId, uri.getQueryParameter("list"))
            }
            lastSegment == "watch_videos" -> {
                putExtra(IntentData.playlistName, uri.getQueryParameter("title"))
                val videoIds = uri.getQueryParameter("video_ids")?.split(",")
                putExtra(IntentData.videoIds, videoIds?.toTypedArray())
            }
            else -> {
                val id = if (lastSegment == "watch") uri.getQueryParameter("v") else lastSegment
                putExtra(IntentData.videoId, id)
                putExtra(IntentData.timeStamp, uri.getQueryParameter("t")?.toTimeInSeconds())
            }
        }
    }
}
