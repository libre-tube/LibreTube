package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DownloadedMediaRowBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.extensions.formatAsFileSize
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.ui.activities.OfflinePlayerActivity
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.sheets.DownloadOptionsBottomSheet
import com.github.libretube.ui.sheets.DownloadOptionsBottomSheet.Companion.DELETE_DOWNLOAD_REQUEST_KEY
import com.github.libretube.ui.viewholders.DownloadsViewHolder
import com.github.libretube.util.TextUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class DownloadsAdapter(
    private val context: Context,
    private val downloads: MutableList<DownloadWithItems>,
    private val toggleDownload: (DownloadWithItems) -> Boolean
) : RecyclerView.Adapter<DownloadsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadsViewHolder {
        val binding = DownloadedMediaRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DownloadsViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DownloadsViewHolder, position: Int) {
        val download = downloads[position].download
        val items = downloads[position].downloadItems
        holder.binding.apply {
            title.text = download.title
            uploaderName.text = download.uploader
            videoInfo.text = download.uploadDate?.let { TextUtils.localizeDate(it) }

            val downloadSize = items.sumOf { it.downloadSize }
            val currentSize = items.filter { it.path.exists() }.sumOf { it.path.fileSize() }

            if (downloadSize == -1L) {
                progressBar.isIndeterminate = true
            } else {
                progressBar.max = downloadSize.toInt()
                progressBar.progress = currentSize.toInt()
            }

            val totalSizeInfo = if (downloadSize > 0) {
                downloadSize.formatAsFileSize()
            } else {
                context.getString(R.string.unknown)
            }
            if (downloadSize > currentSize) {
                downloadOverlay.isVisible = true
                resumePauseBtn.setImageResource(R.drawable.ic_download)
                fileSize.text = "${currentSize.formatAsFileSize()} / $totalSizeInfo"
            } else {
                downloadOverlay.isGone = true
                fileSize.text = totalSizeInfo
            }

            download.thumbnailPath?.let { path ->
                thumbnailImage.setImageBitmap(ImageHelper.getDownloadedImage(context, path))
            }

            progressBar.setOnClickListener {
                val isDownloading = toggleDownload(downloads[position])

                resumePauseBtn.setImageResource(
                    if (isDownloading) {
                        R.drawable.ic_pause
                    } else {
                        R.drawable.ic_download
                    }
                )
            }

            root.setOnClickListener {
                val intent = Intent(root.context, OfflinePlayerActivity::class.java)
                intent.putExtra(IntentData.videoId, download.videoId)
                root.context.startActivity(intent)
            }

            root.setOnLongClickListener {
                val activity = root.context as BaseActivity
                val fragmentManager = activity.supportFragmentManager
                fragmentManager.setFragmentResultListener(
                    DELETE_DOWNLOAD_REQUEST_KEY,
                    activity
                ) { _, _ ->
                    showDeleteDialog(root.context, position)
                }
                DownloadOptionsBottomSheet()
                    .apply {
                        arguments = bundleOf(
                            IntentData.videoId to download.videoId,
                            IntentData.channelName to download.uploader
                        )
                    }
                    .show(fragmentManager)
                true
            }
        }
    }

    fun showDeleteDialog(context: Context, position: Int) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.delete)
            .setMessage(R.string.irreversible)
            .setPositiveButton(R.string.okay) { _, _ ->
                deleteDownload(position)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteDownload(position: Int) {
        val download = downloads[position].download
        val items = downloads[position].downloadItems

        items.forEach {
            it.path.deleteIfExists()
        }
        download.thumbnailPath?.deleteIfExists()

        runBlocking(Dispatchers.IO) {
            DatabaseHolder.Database.downloadDao().deleteDownload(download)
        }
        downloads.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount)
    }

    fun restoreItem(position: Int) {
        // moves the item back to its initial horizontal position
        notifyItemRemoved(position)
        notifyItemInserted(position)
    }

    override fun getItemCount() = downloads.size
}
