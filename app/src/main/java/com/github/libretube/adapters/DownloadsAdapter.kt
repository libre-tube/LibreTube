package com.github.libretube.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.activities.OfflinePlayerActivity
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DownloadedMediaRowBinding
import java.io.File

class DownloadsAdapter(
    private val files: List<File>
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
            fileSize.text = "${file.length() / (1024 * 1024)} MiB"
            root.setOnClickListener {
                val intent = Intent(root.context, OfflinePlayerActivity::class.java).also {
                    it.putExtra(IntentData.fileName, file.name)
                }
                root.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }
}

class DownloadsViewHolder(
    val binding: DownloadedMediaRowBinding
) : RecyclerView.ViewHolder(binding.root)
