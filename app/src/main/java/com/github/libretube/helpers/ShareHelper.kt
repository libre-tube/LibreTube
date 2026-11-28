package com.github.libretube.helpers

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.obj.ShareData
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

object ShareHelper {
    fun shareLink(
        fragmentManager: FragmentManager,
        context: Context,
        id: String,
        shareObjectType: ShareObjectType,
        shareData: ShareData,
    ) {
        if (shareObjectType == ShareObjectType.VIDEO) {
            ShareDialog().apply {
                arguments = bundleOf(
                    IntentData.id to id, IntentData.shareData to shareData
                )
            }.show(fragmentManager, ShareDialog::class.java.name)
            return
        }

        val url = when (shareObjectType) {
            ShareObjectType.PLAYLIST -> "$YOUTUBE_FRONTEND_URL/playlist?list=$id"
            ShareObjectType.CHANNEL -> "$YOUTUBE_FRONTEND_URL/channel/$id"
        }

        shareLink(
            context,
            url,
            shareData.title,
            shareData.previewImageUrl
        )
    }

    fun shareLink(context: Context, url: String, title: String?, previewImageUrl: String?) {
        val shareIntent = Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
                putExtra(Intent.EXTRA_TITLE, title)

                previewImageUrl?.let {
                    val uri = runBlocking { generatePreviewUri(context, it) }
                    clipData = ClipData.newRawUri(null, uri)
                    data = uri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            },
            context.getString(R.string.shareTo)
        )
        context.startActivity(shareIntent)
    }

    private suspend fun generatePreviewUri(context: Context, imageUrl: String): Uri? {
        val file = withContext(Dispatchers.IO) {
            val shareImageCacheDir = File(context.cacheDir, "share_image").also {
                it.mkdirs()
            }
            val file = File(shareImageCacheDir, "share_preview.png")
            val downloadSuccess = ImageHelper.downloadImage(context, imageUrl, file.toPath())
            file.takeIf { downloadSuccess }
        } ?: return null

        return FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
    }
}