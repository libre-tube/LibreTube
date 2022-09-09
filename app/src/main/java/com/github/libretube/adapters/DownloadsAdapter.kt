package com.github.libretube.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }
}

class DownloadsViewHolder(
    val binding: DownloadedMediaRowBinding
) : RecyclerView.ViewHolder(binding.root)
