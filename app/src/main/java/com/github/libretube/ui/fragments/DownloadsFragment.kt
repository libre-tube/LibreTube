package com.github.libretube.ui.fragments

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.FragmentDownloadsBinding
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.extensions.formatAsFileSize
import com.github.libretube.helpers.DownloadHelper
import com.github.libretube.obj.DownloadStatus
import com.github.libretube.receivers.DownloadReceiver
import com.github.libretube.services.DownloadService
import com.github.libretube.ui.adapters.DownloadsAdapter
import com.github.libretube.ui.viewholders.DownloadsViewHolder
import kotlin.io.path.fileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DownloadsFragment : Fragment() {
    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!

    private var binder: DownloadService.LocalBinder? = null
    private val downloads = mutableListOf<DownloadWithItems>()
    private val downloadReceiver = DownloadReceiver()

    private val serviceConnection = object : ServiceConnection {
        var isBound = false
        var job: Job? = null

        override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
            binder = iBinder as DownloadService.LocalBinder
            isBound = true
            job?.cancel()
            job = lifecycleScope.launch {
                binder?.getService()?.downloadFlow?.collectLatest {
                    updateProgress(it.first, it.second)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
            isBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dbDownloads = runBlocking(Dispatchers.IO) {
            Database.downloadDao().getAll()
        }.takeIf { it.isNotEmpty() } ?: return

        downloads.clear()
        downloads.addAll(dbDownloads)

        binding.downloadsEmpty.isGone = true
        binding.downloads.isVisible = true

        binding.downloads.layoutManager = LinearLayoutManager(context)

        val adapter = DownloadsAdapter(requireContext(), downloads) {
            var isDownloading = false
            val ids = it.downloadItems
                .filter { item -> item.path.fileSize() < item.downloadSize }
                .map { item -> item.id }

            if (!serviceConnection.isBound) {
                DownloadHelper.startDownloadService(requireContext())
                bindDownloadService(ids.toIntArray())
                return@DownloadsAdapter true
            }

            binder?.getService()?.let { service ->
                isDownloading = ids.any { id -> service.isDownloading(id) }

                ids.forEach { id ->
                    if (isDownloading) {
                        service.pause(id)
                    } else {
                        service.resume(id)
                    }
                }
            }
            return@DownloadsAdapter isDownloading.not()
        }

        binding.downloads.adapter = adapter

        val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int = makeMovementFlags(0, ItemTouchHelper.LEFT)

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                adapter.showDeleteDialog(requireContext(), viewHolder.absoluteAdapterPosition)
                // put the item back to the center, as it's currently out of the screen
                adapter.restoreItem(viewHolder.absoluteAdapterPosition)
            }
        }
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.downloads)

        binding.downloads.adapter?.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                    super.onItemRangeRemoved(positionStart, itemCount)
                    val binding = _binding ?: return
                    if (binding.downloads.adapter?.itemCount == 0) {
                        binding.downloads.isGone = true
                        binding.downloadsEmpty.isVisible = true
                    }
                }
            }
        )
    }

    override fun onStart() {
        if (DownloadService.IS_DOWNLOAD_RUNNING) {
            val intent = Intent(requireContext(), DownloadService::class.java)
            context?.bindService(intent, serviceConnection, 0)
        }
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(DownloadService.ACTION_SERVICE_STARTED)
        filter.addAction(DownloadService.ACTION_SERVICE_STOPPED)
        context?.registerReceiver(downloadReceiver, filter)
    }

    fun bindDownloadService(ids: IntArray? = null) {
        if (serviceConnection.isBound) return

        val intent = Intent(context, DownloadService::class.java)
        intent.putExtra("ids", ids)
        context?.bindService(intent, serviceConnection, 0)
    }

    fun updateProgress(id: Int, status: DownloadStatus) {
        val index = downloads.indexOfFirst {
            it.downloadItems.any { item -> item.id == id }
        }
        val view =
            _binding?.downloads?.findViewHolderForAdapterPosition(index) as? DownloadsViewHolder

        view?.binding?.apply {
            when (status) {
                DownloadStatus.Paused -> {
                    resumePauseBtn.setImageResource(R.drawable.ic_download)
                }

                DownloadStatus.Completed -> {
                    downloadOverlay.isGone = true
                }

                DownloadStatus.Stopped -> Unit

                is DownloadStatus.Progress -> {
                    downloadOverlay.isVisible = true
                    resumePauseBtn.setImageResource(R.drawable.ic_pause)
                    if (progressBar.isIndeterminate) return
                    progressBar.incrementProgressBy(status.progress.toInt())
                    val progressInfo = progressBar.progress.formatAsFileSize() +
                        " /\n" + progressBar.max.formatAsFileSize()
                    fileSize.text = progressInfo
                }

                is DownloadStatus.Error -> {
                    resumePauseBtn.setImageResource(R.drawable.ic_restart)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        context?.unregisterReceiver(downloadReceiver)
    }

    override fun onStop() {
        super.onStop()
        runCatching {
            context?.unbindService(serviceConnection)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
