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
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
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
import com.github.libretube.db.obj.Download
import com.github.libretube.db.obj.DownloadPlaylistWithDownload
import com.github.libretube.db.obj.DownloadWithItems
import com.github.libretube.db.obj.filterByTab
import com.github.libretube.extensions.ceilHalf
import com.github.libretube.extensions.dpToPx
import com.github.libretube.extensions.formatAsFileSize
import com.github.libretube.extensions.serializable
import com.github.libretube.extensions.setOnDismissListener
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.DownloadHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.DownloadStatus
import com.github.libretube.receivers.DownloadReceiver
import com.github.libretube.services.DownloadService
import com.github.libretube.ui.adapters.DownloadPlaylistAdapter
import com.github.libretube.ui.adapters.DownloadsAdapter
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.ui.extensions.setOnBackPressed
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
    AUDIO,
    PLAYLIST
}

enum class DownloadSortingOrder(@StringRes val stringId: Int) {
    OLDEST(R.string.least_recent),
    NEWEST(R.string.most_recent),
    ALPHABETIC(R.string.alphabetic),
    DURATION(R.string.duration),
    CHANNEL(R.string.sort_channel),
    SIZE(R.string.sort_size)
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
                DownloadTab.PLAYLIST.ordinal -> getString(R.string.playlists)
                else -> throw IllegalArgumentException()
            }
        }.attach()
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
        if (position == DownloadTab.PLAYLIST.ordinal) {
            return PlaylistDownloadsFragmentPage()
        }

        return DownloadsFragmentPage().apply {
            arguments = bundleOf(IntentData.downloadTab to DownloadTab.entries[position])
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

    // Either downloadTab or downloadPlaylistId are set, never both at the same time!
    private lateinit var downloadTab: DownloadTab
    private var downloadPlaylistId: String? = null

    private var selectedSortType
        get() = PreferenceHelper.getInt(
            PreferenceKeys.SELECTED_DOWNLOAD_SORT_TYPE,
            DownloadSortingOrder.OLDEST.ordinal
        )
        set(value) {
            PreferenceHelper.putInt(PreferenceKeys.SELECTED_DOWNLOAD_SORT_TYPE, value)
        }

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

        this.downloadTab = requireArguments().serializable(IntentData.downloadTab)!!
        this.downloadPlaylistId = requireArguments().getString(IntentData.playlistId)

        if (downloadTab == DownloadTab.PLAYLIST && downloadPlaylistId == null)
            throw IllegalArgumentException("downloadTab unspecified or missing playlist id")
    }

    override fun setLayoutManagers(gridItems: Int) {
        _binding?.downloadsRecView?.layoutManager = GridLayoutManager(context, gridItems.ceilHalf())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDownloadContentBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        adapter =
            DownloadsAdapter(
                requireContext(), downloadTab, downloadPlaylistId,
                currentSortOrder = {
                    DownloadSortingOrder.entries[selectedSortType]
                }
            ) {
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

        val filterOptions = DownloadSortingOrder.entries.map { getString(it.stringId) }
        binding.sortType.text = filterOptions[selectedSortType]

        lifecycleScope.launch(Dispatchers.Main) {
            val playlistItems = downloadPlaylistId?.let { playlistId ->
                val playlist = withContext(Dispatchers.IO) {
                    Database.downloadDao().getDownloadPlaylistById(playlistId)
                }

                binding.playlistName.text = playlist.downloadPlaylist.title

                playlist.downloadVideos.map { it.videoId }
            }

            val downloads = withContext(Dispatchers.IO) {
                Database.downloadDao().getAll()
            }.let { downloads ->
                if (downloadTab != DownloadTab.PLAYLIST) downloads.filterByTab(downloadTab)
                else downloads.filter { playlistItems.orEmpty().contains(it.download.videoId) }
            }

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
            if (downloadTab == DownloadTab.AUDIO) {
                BackgroundHelper.playOnBackgroundOffline(
                    requireContext(),
                    videoId = null,
                    playlistId = null,
                    downloadTab,
                    shuffle = true
                )

                NavigationHelper.openAudioPlayerFragment(requireContext(), offlinePlayer = true)
            } else {
                NavigationHelper.navigateVideo(
                    requireContext(),
                    videoId = null,
                    playlistId = downloadPlaylistId,
                    downloadTab = downloadTab,
                    shuffle = true,
                    isOffline = true
                )
            }
        }

        playerViewModel.isMiniPlayerVisible.observe(viewLifecycleOwner) { isMiniPlayerVisible ->
            binding.shuffleAll.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = (if (isMiniPlayerVisible) 64f else 16f).dpToPx()
            }
        }
    }

    private fun submitDownloadList(items: List<DownloadWithItems>) {
        val sortOrder = DownloadSortingOrder.entries[selectedSortType]
        val sortedItems = sortDownloadWithItemsList(items, sortOrder)

        adapter.submitList(sortedItems)
    }

    private fun toggleVisibilities() {
        val binding = _binding ?: return

        val isEmpty = adapter.itemCount == 0
        binding.downloadsEmpty.isVisible = isEmpty
        binding.downloadsContainer.isGone = isEmpty
        binding.deleteAll.isGone = isEmpty
        binding.shuffleAll.isGone = isEmpty
    }

    private fun showDeleteAllDialog(context: Context, adapter: DownloadsAdapter) {
        var onlyDeleteWatchedVideos = false

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.delete_all)
            .setMultiChoiceItems(
                arrayOf(getString(R.string.delete_only_watched_videos)),
                null
            ) { _, _, selected ->
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

    companion object {
        fun sortDownloadWithItemsList(
            items: List<DownloadWithItems>,
            selectedSortType: DownloadSortingOrder
        ): List<DownloadWithItems> {
            return when (selectedSortType) {
                DownloadSortingOrder.OLDEST -> items
                DownloadSortingOrder.NEWEST -> items.reversed()
                DownloadSortingOrder.ALPHABETIC -> items.sortedBy { it.download.title }
                DownloadSortingOrder.DURATION -> items.sortedBy { it.download.duration }
                DownloadSortingOrder.CHANNEL -> items.sortedBy { it.download.uploader }
                DownloadSortingOrder.SIZE -> items.sortedBy { it.downloadItems.sumOf { o -> o.downloadSize } }
            }
        }

        // ugly HACK: should probably be refactored in the future
        fun sortDownloadList(items: List<Download>, selectedSortType: DownloadSortingOrder): List<Download> {
            return when (selectedSortType) {
                DownloadSortingOrder.OLDEST -> items
                DownloadSortingOrder.NEWEST -> items.reversed()
                DownloadSortingOrder.ALPHABETIC -> items.sortedBy { it.title }
                DownloadSortingOrder.DURATION -> items.sortedBy { it.duration }
                DownloadSortingOrder.CHANNEL -> items.sortedBy { it.uploader }
                DownloadSortingOrder.SIZE -> items
            }
        }
    }
}

class PlaylistDownloadsFragmentPage : Fragment(R.layout.fragment_download_content) {
    private var selectedSortType
        get() = PreferenceHelper.getInt(PreferenceKeys.SELECTED_DOWNLOAD_PLAYLIST_SORT_TYPE, 0)
        set(value) {
            PreferenceHelper.putInt(PreferenceKeys.SELECTED_DOWNLOAD_PLAYLIST_SORT_TYPE, value)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentDownloadContentBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.shuffleAll.isGone = true

        var backPressedCallback: OnBackPressedCallback? = null
        backPressedCallback = setOnBackPressed {
            childFragmentManager.fragments.firstOrNull()?.let {
                childFragmentManager.commit { remove(it) }
            }
            backPressedCallback?.isEnabled = false
        }
        backPressedCallback.isEnabled = false

        val adapter = DownloadPlaylistAdapter { playlist ->
            childFragmentManager.commit {
                replace<DownloadsFragmentPage>(
                    binding.fragment.id,
                    args = bundleOf(
                        IntentData.downloadTab to DownloadTab.PLAYLIST,
                        IntentData.playlistId to playlist.downloadPlaylist.playlistId
                    )
                )
            }
            backPressedCallback.isEnabled = true
        }
        binding.downloadsRecView.setOnDismissListener { position ->
            adapter.showDeleteDialog(requireContext(), position)
            // put the item back to the center, as it's currently out of the screen
            adapter.restoreItem(position)
        }
        binding.downloadsRecView.adapter = adapter

        val filterOptions = DownloadSortingOrder.entries.map { getString(it.stringId) }
        binding.sortType.text = filterOptions[selectedSortType]

        lifecycleScope.launch(Dispatchers.Main) {
            val downloadPlaylists = withContext(Dispatchers.IO) {
                Database.downloadDao().getDownloadPlaylists()
            }

            if (downloadPlaylists.isNotEmpty()) {
                adapter.submitList(applySortOrder(downloadPlaylists))
                binding.downloadsEmpty.isGone = true
                binding.downloadsRecView.isVisible = true

                binding.sortType.setOnClickListener {
                    BaseBottomSheet().setSimpleItems(filterOptions.toList()) { index ->
                        if (index == selectedSortType) return@setSimpleItems
                        selectedSortType = index

                        binding.sortType.text = filterOptions[index]
                        adapter.submitList(applySortOrder(downloadPlaylists))
                    }.show(childFragmentManager)
                }
            } else {
                binding.sortType.isGone = true
            }
        }
    }

    fun applySortOrder(items: List<DownloadPlaylistWithDownload>): List<DownloadPlaylistWithDownload> {
        return when (selectedSortType) {
            0 -> items
            else -> items.reversed()
        }
    }
}