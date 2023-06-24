package com.github.libretube.ui.adapters

import android.content.Intent
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.IntentChooserItemBinding
import com.github.libretube.ui.viewholders.IntentChooserViewHolder

/**
 * An adapter for opening an intent chooser inside the app, example-wise for urls
 * @param packages A list of resolved packages found by a package query
 */
class IntentChooserAdapter(
    private val packages: List<ResolveInfo>,
    private val queryUrl: String
) : RecyclerView.Adapter<IntentChooserViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntentChooserViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = IntentChooserItemBinding.inflate(layoutInflater, parent, false)
        return IntentChooserViewHolder(binding)
    }

    override fun getItemCount() = packages.size

    override fun onBindViewHolder(holder: IntentChooserViewHolder, position: Int) {
        val currentPackage = packages[position]
        holder.binding.apply {
            val drawable = currentPackage.loadIcon(root.context.packageManager)
            appIconIV.setImageDrawable(drawable)
            val appLabel = currentPackage.loadLabel(root.context.packageManager)
            appNameTV.text = appLabel
            root.setOnClickListener {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, queryUrl.toUri())
                        .setPackage(currentPackage.activityInfo.packageName)
                    root.context.startActivity(intent)
                }
            }
        }
    }
}
