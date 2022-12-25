package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DownloadedMediaRowBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.extensions.formatAsFileSize
import com.github.libretube.extensions.query
import com.github.libretube.ui.activities.OfflinePlayerActivity
import com.github.libretube.ui.viewholders.DownloadsViewHolder
import com.github.libretube.util.ImageHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class DownloadsAdapter(
    private val context: Context,
    private val downloads: MutableList<DownloadWithItems>,
    private val toogleDownload: (DownloadWithItems) -> Boolean
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
            videoInfo.text = download.uploadDate

            val downloadSize = items.sumOf { it.downloadSize }
            val currentSize = items.sumOf { File(it.path).length() }

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
                downloadOverlay.visibility = View.VISIBLE
                resumePauseBtn.setImageResource(R.drawable.ic_download)
                fileSize.text = "${currentSize.formatAsFileSize()} / $totalSizeInfo"
            } else {
                downloadOverlay.visibility = View.GONE
                fileSize.text = totalSizeInfo
            }

            download.thumbnailPath?.let { path ->
                thumbnailImage.setImageBitmap(ImageHelper.getDownloadedImage(context, path))
            }

            progressBar.setOnClickListener {
                val isDownloading = toogleDownload(downloads[position])

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
                MaterialAlertDialogBuilder(root.context)
                    .setItems(
                        arrayOf(
                            root.context.getString(R.string.delete)
                        )
                    ) { _, index ->
                        when (index) {
                            0 -> {
                                items.map { File(it.path) }.forEach { file ->
                                    if (file.exists()) {
                                        try {
                                            file.delete()
                                        } catch (_: Exception) { }
                                    }
                                }

                                query {
                                    DatabaseHolder.Database.downloadDao().deleteDownload(download)
                                }
                                downloads.removeAt(position)
                                notifyItemRemoved(position)
                                notifyItemRangeChanged(position, itemCount)
                            }
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                true
            }
        }
    }

    override fun getItemCount(): Int {
        return downloads.size
    }
}
