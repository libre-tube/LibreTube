package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DownloadedMediaRowBinding
import com.github.libretube.obj.DownloadedFile
import com.github.libretube.ui.activities.OfflinePlayerActivity
import com.github.libretube.ui.viewholders.DownloadsViewHolder
import com.github.libretube.util.DownloadHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class DownloadsAdapter(
    private val files: MutableList<DownloadedFile>
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
        val file = files[position]
        holder.binding.apply {
            fileName.text = file.name
            fileSize.text = "${file.size / (1024 * 1024)} MiB"

            root.setOnClickListener {
                val intent = Intent(root.context, OfflinePlayerActivity::class.java).also {
                    it.putExtra(IntentData.fileName, file.name)
                }
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
                                val audioDir = DownloadHelper.getAudioDir(root.context)
                                val videoDir = DownloadHelper.getVideoDir(root.context)

                                listOf(audioDir, videoDir).forEach {
                                    val f = File(it, file.name)
                                    if (f.exists()) {
                                        try {
                                            f.delete()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }

                                files.removeAt(position)
                                notifyItemRemoved(position)
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
        return files.size
    }
}
