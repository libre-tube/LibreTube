package com.github.libretube.ui.fragments

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentDownloadContentBinding
import com.github.libretube.databinding.FragmentDownloadsBinding
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.db.obj.filterByTab
import com.github.libretube.extensions.ceilHalf
import com.github.libretube.extensions.dpToPx
import com.github.libretube.extensions.formatAsFileSize
import com.github.libretube.extensions.serializable
import com.github.libretube.extensions.setOnDismissListener
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.DownloadHelper
import com.github.libretube.helpers.NavBarHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.DownloadStatus
import com.github.libretube.receivers.DownloadReceiver
import com.github.libretube.services.DownloadService
import com.github.libretube.ui.adapters.DownloadsAdapter
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.ui.extensions.setupFragmentAnimation
import com.github.libretube.ui.models.CommonPlayerViewModel
import com.github.libretube.ui.sheets.BaseBottomSheet
import com.github.libretube.ui.viewholders.DownloadsViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.path.fileSize

enum class DownloadTab {
    VIDEO,
    AUDIO
}

class DownloadsFragment : Fragment(R.layout.fragment_downloads) {
    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDownloadsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.downloadsPager.adapter = DownloadsFragmentAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.downloadsPager) { tab, position ->
            tab.text = when (position) {
                DownloadTab.VIDEO.ordinal -> getString(R.string.video)
                DownloadTab.AUDIO.ordinal -> getString(R.string.audio)
                else -> throw IllegalArgumentException()
            }
        }.attach()

        if (NavBarHelper.getStartFragmentId(requireContext()) != R.id.downloadsFragment) {
            setupFragmentAnimation(binding.root)
        }
    }

    fun bindDownloadService() {
        childFragmentManager.fragments.filterIsInstance<DownloadsFragmentPage>().forEach {
            it.bindDownloadService()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class DownloadsFragmentAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount() = DownloadTab.entries.size

    override fun createFragment(position: Int): Fragment {
        return DownloadsFragmentPage().apply {
            arguments = bundleOf(IntentData.currentPosition to DownloadTab.entries[position])
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
class DownloadsFragmentPage : DynamicLayoutManagerFragment(R.layout.fragment_download_content) {
    private lateinit var adapter: DownloadsAdapter
    private var _binding: FragmentDownloadContentBinding? = null
    private val binding get() = _binding!!

    private val playerViewModel: CommonPlayerViewModel by activityViewModels()

    private var binder: DownloadService.LocalBinder? = null
    private val downloadReceiver = DownloadReceiver()
    private lateinit var downloadTab: DownloadTab

    private var selectedSortType
        get() = PreferenceHelper.getInt(PreferenceKeys.SELECTED_DOWNLOAD_SORT_TYPE, 0)
        set(value) {PreferenceHelper.putInt(PreferenceKeys.SELECTED_DOWNLOAD_SORT_TYPE, value) }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.downloadTab = requireArguments().serializable(IntentData.currentPosition)!!
    }

    override fun setLayoutManagers(gridItems: Int) {
        _binding?.downloadsRecView?.layoutManager = GridLayoutManager(context, gridItems.ceilHalf())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDownloadContentBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
        adapter = DownloadsAdapter(requireContext(), downloadTab) {
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
        binding.downloadsRecView.adapter = adapter

        val filterOptions = resources.getStringArray(R.array.downloadSortOptions)
        binding.sortType.text = filterOptions[selectedSortType]

        lifecycleScope.launch {
            val downloads = withContext(Dispatchers.IO) {
                Database.downloadDao().getAll()
            }.filterByTab(downloadTab)

            submitDownloadList(downloads)

            binding.sortType.setOnClickListener {
                BaseBottomSheet().setSimpleItems(filterOptions.toList()) { index ->
                    if (index == selectedSortType) return@setSimpleItems
                    selectedSortType = index

                    binding.sortType.text = filterOptions[index]
                    submitDownloadList(downloads)
                }.show(childFragmentManager)
            }

            binding.downloadsRecView.setOnDismissListener { position ->
                adapter.showDeleteDialog(requireContext(), position)
                // put the item back to the center, as it's currently out of the screen
                adapter.restoreItem(position)
            }

            binding.downloadsRecView.adapter?.registerAdapterDataObserver(
                object : RecyclerView.AdapterDataObserver() {
                    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                        super.onItemRangeRemoved(positionStart, itemCount)
                        toggleVisibilities()
                    }
                }
            )

            toggleVisibilities()
        }

        binding.deleteAll.setOnClickListener {
            showDeleteAllDialog(binding.root.context, adapter)
        }

        binding.shuffleAll.setOnClickListener {
            BackgroundHelper.playOnBackgroundOffline(
                requireContext(),
                null,
                downloadTab,
                shuffle = true
            )

            NavigationHelper.openAudioPlayerFragment(requireContext(), offlinePlayer = true)
        }

        playerViewModel.isMiniPlayerVisible.observe(viewLifecycleOwner) { isMiniPlayerVisible ->
            binding.fabContainer.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = (if (isMiniPlayerVisible) 64f else 16f).dpToPx()
            }
        }
    }

    private fun submitDownloadList(items: List<DownloadWithItems>) {
        val sortedItems = when (selectedSortType) {
            0 -> items
            else -> items.reversed()
        }

        adapter.submitList(sortedItems)
    }

    private fun toggleVisibilities() {
        val binding = _binding ?: return

        val isEmpty = adapter.itemCount == 0
        binding.downloadsEmpty.isVisible = isEmpty
        binding.downloadsContainer.isGone = isEmpty
        binding.deleteAll.isGone = isEmpty
        binding.shuffleAll.isGone = isEmpty || downloadTab != DownloadTab.AUDIO
    }

    private fun showDeleteAllDialog(context: Context, adapter: DownloadsAdapter) {
        var onlyDeleteWatchedVideos = false

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.delete_all)
            .setMultiChoiceItems(arrayOf(getString(R.string.delete_only_watched_videos)), null) { _, _, selected ->
                onlyDeleteWatchedVideos = selected
            }
            .setPositiveButton(R.string.okay) { _, _ ->
                adapter.deleteAllDownloads(onlyDeleteWatchedVideos)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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

        val filter = IntentFilter().apply {
            addAction(DownloadService.ACTION_SERVICE_STARTED)
            addAction(DownloadService.ACTION_SERVICE_STOPPED)
        }
        ContextCompat.registerReceiver(
            requireContext(),
            downloadReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun bindDownloadService(ids: IntArray? = null) {
        if (serviceConnection.isBound) return

        val intent = Intent(context, DownloadService::class.java)
        intent.putExtra("ids", ids)
        context?.bindService(intent, serviceConnection, 0)
    }

    fun updateProgress(id: Int, status: DownloadStatus) {
        val index = adapter.currentList.indexOfFirst {
            it.downloadItems.any { item -> item.id == id }
        }
        val view =
            _binding?.downloadsRecView?.findViewHolderForAdapterPosition(index) as? DownloadsViewHolder

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