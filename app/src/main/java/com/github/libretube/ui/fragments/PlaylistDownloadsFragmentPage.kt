package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentDownloadContentBinding
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.DownloadPlaylistWithDownload
import com.github.libretube.enums.DownloadSortingOrder
import com.github.libretube.enums.DownloadTab
import com.github.libretube.extensions.setOnDismissListener
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.adapters.DownloadPlaylistAdapter
import com.github.libretube.ui.extensions.setOnBackPressed
import com.github.libretube.ui.models.DownloadsViewModel
import com.github.libretube.ui.sheets.BaseBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistDownloadsFragmentPage : Fragment(R.layout.fragment_download_content) {

    private var selectedSortType
        get() = PreferenceHelper.getInt(PreferenceKeys.SELECTED_DOWNLOAD_PLAYLIST_SORT_TYPE, 0)
        set(value) {
            PreferenceHelper.putInt(PreferenceKeys.SELECTED_DOWNLOAD_PLAYLIST_SORT_TYPE, value)
        }
    private val downloadsModel: DownloadsViewModel by activityViewModels()

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
            adapter.restoreItem(position)
        }
        binding.downloadsRecView.adapter = adapter

        val filterOptions = DownloadSortingOrder.entries.map { getString(it.stringId) }
        binding.sortType.text = filterOptions[selectedSortType]

        lifecycleScope.launch(Dispatchers.Main) {
            val downloadPlaylists = withContext(Dispatchers.IO) {
                Database.downloadDao().getDownloadPlaylists()
            }

            binding.downloadsEmpty.isVisible = downloadPlaylists.isEmpty()
            if (downloadPlaylists.isNotEmpty()) {
                submitPlaylists(adapter, downloadPlaylists)

                binding.sortType.setOnClickListener {
                    BaseBottomSheet().setSimpleItems(filterOptions.toList()) { index ->
                        if (index == selectedSortType) return@setSimpleItems
                        selectedSortType = index
                        binding.sortType.text = filterOptions[index]
                        submitPlaylists(adapter, downloadPlaylists)
                    }.show(childFragmentManager)
                }

                downloadsModel.searchQuery.observe(viewLifecycleOwner) {
                    submitPlaylists(adapter, downloadPlaylists)
                }

                adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                        super.onItemRangeRemoved(positionStart, itemCount)
                        binding.downloadsEmpty.isVisible = adapter.itemCount == 0
                        binding.downloadsRecView.isVisible = adapter.itemCount != 0
                    }

                    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                        super.onItemRangeInserted(positionStart, itemCount)
                        binding.downloadsEmpty.isVisible = adapter.itemCount == 0
                        binding.downloadsRecView.isVisible = adapter.itemCount != 0
                    }
                })
            } else {
                binding.sortType.isGone = true
            }
        }
    }

    private fun submitPlaylists(
        adapter: DownloadPlaylistAdapter,
        playlists: List<DownloadPlaylistWithDownload>
    ) {
        var sorted = applySortOrder(playlists)
        val query = downloadsModel.searchQuery.value
        if (!query.isNullOrEmpty()) {
            sorted = sorted.filter {
                it.downloadPlaylist.title.contains(query, ignoreCase = true)
            }
        }
        adapter.submitList(sorted)
    }

    private fun applySortOrder(items: List<DownloadPlaylistWithDownload>): List<DownloadPlaylistWithDownload> {
        return when (selectedSortType) {
            0 -> items
            else -> items.reversed()
        }
    }
}
