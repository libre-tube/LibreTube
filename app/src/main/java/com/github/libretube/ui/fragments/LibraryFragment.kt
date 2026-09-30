package com.github.libretube.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.obj.Playlists
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentLibraryBinding
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.dpToPx
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.extensions.move
import com.github.libretube.extensions.setOnDraggedListener
import com.github.libretube.helpers.NavBarHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.repo.UserDataRepositoryHelper
import com.github.libretube.ui.adapters.PlaylistBookmarkAdapter
import com.github.libretube.ui.adapters.PlaylistsAdapter
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.ui.dialogs.CreatePlaylistDialog
import com.github.libretube.ui.dialogs.CreatePlaylistDialog.Companion.CREATE_PLAYLIST_DIALOG_REQUEST_KEY
import com.github.libretube.ui.models.CommonPlayerViewModel
import com.github.libretube.ui.sheets.BaseBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryFragment : DynamicLayoutManagerFragment(R.layout.fragment_library) {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val commonPlayerViewModel: CommonPlayerViewModel by activityViewModels()

    private val playlistsAdapter = PlaylistsAdapter(PlaylistType.PRIVATE)
    private val playlistBookmarkAdapter = PlaylistBookmarkAdapter()

    private val dragScrollHandler = Handler(Looper.getMainLooper())
    private var dragAutoScrollRunnable: Runnable? = null
    private var draggedViewHolder: RecyclerView.ViewHolder? = null

    override fun setLayoutManagers(gridItems: Int) {
        _binding?.bookmarksRecView?.layoutManager = GridLayoutManager(context, gridItems)
        _binding?.playlistRecView?.layoutManager = GridLayoutManager(context, gridItems)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentLibraryBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.bookmarksRecView.adapter = playlistBookmarkAdapter
        // listen for playlists to become deleted
        playlistsAdapter.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                _binding?.nothingHere?.isVisible = playlistsAdapter.itemCount == 0
                _binding?.sortTV?.isVisible = playlistsAdapter.itemCount > 0
                super.onItemRangeRemoved(positionStart, itemCount)
            }
        })
        binding.playlistRecView.adapter = playlistsAdapter

        // listen for the mini player state changing
        commonPlayerViewModel.isMiniPlayerVisible.observe(viewLifecycleOwner) {
            updateFABMargin(it)
        }

        // hide watch history button of history disabled
        val watchHistoryEnabled =
            PreferenceHelper.getBoolean(PreferenceKeys.WATCH_HISTORY_TOGGLE, true)
        if (!watchHistoryEnabled) {
            binding.watchHistory.isGone = true
        } else {
            binding.watchHistory.setOnClickListener {
                findNavController().navigate(R.id.action_libraryFragment_to_watchHistoryFragment)
            }
        }

        binding.downloads.setOnClickListener {
            findNavController().navigate(R.id.action_libraryFragment_to_downloadsFragment)
        }

        val navBarItems = NavBarHelper.getNavBarItemPreference(requireContext())
        if (navBarItems.any { (itemId, isVisible) -> isVisible && itemId == R.id.downloadsFragment }) {
            binding.downloads.isGone = true
        }

        fetchPlaylists()
        initBookmarks()

        binding.playlistRefresh.isEnabled = true
        binding.playlistRefresh.setOnRefreshListener {
            fetchPlaylists()
            initBookmarks()
        }

        childFragmentManager.setFragmentResultListener(
            CREATE_PLAYLIST_DIALOG_REQUEST_KEY,
            this
        ) { _, resultBundle ->
            val isPlaylistCreated = resultBundle.getBoolean(IntentData.playlistTask)
            if (isPlaylistCreated) {
                fetchPlaylists()
            }
        }
        binding.createPlaylist.setOnClickListener {
            CreatePlaylistDialog()
                .show(childFragmentManager, CreatePlaylistDialog::class.java.name)
        }

        val sortOptions = resources.getStringArray(R.array.playlistSortingOptions)
        val sortOptionValues = resources.getStringArray(R.array.playlistSortingOptionsValues)
        val order = PreferenceHelper.getString(
          PreferenceKeys.PLAYLISTS_ORDER,
          sortOptionValues.first()
        )
        val orderIndex = sortOptionValues.indexOf(order)
        binding.sortTV.text = sortOptions.getOrNull(orderIndex)

        binding.sortTV.setOnClickListener {
            BaseBottomSheet().apply {
                setSimpleItems(sortOptions.toList()) { index ->
                    binding.sortTV.text = sortOptions[index]
                    val value = sortOptionValues[index]
                    PreferenceHelper.putString(PreferenceKeys.PLAYLISTS_ORDER, value)
                    fetchPlaylists()
                    setupManualSorting()
                }
            }.show(childFragmentManager)
        }

        setupManualSorting()
    }

    override fun onDestroyView() {
        stopDragAutoScroll()
        draggedViewHolder = null
        super.onDestroyView()
        _binding = null
    }

    // Scroll the playlist item being dragged
    // if it is near the top or bottom of the scroll view
    private fun startDragAutoScroll() {
        dragAutoScrollRunnable?.let { dragScrollHandler.removeCallbacks(it) }
        val runnable = object : Runnable {
            override fun run() {
                val binding = _binding ?: return
                val viewHolder = draggedViewHolder ?: return
                val scrollView = binding.playlistScrollView
                val threshold = (64f).dpToPx()
                val scrollSpeed = (18f).dpToPx()

                val itemLocation = IntArray(2)
                viewHolder.itemView.getLocationOnScreen(itemLocation)
                val scrollLocation = IntArray(2)
                scrollView.getLocationOnScreen(scrollLocation)

                val itemTop = itemLocation[1]
                val itemBottom = itemTop + viewHolder.itemView.height
                val viewportTop = scrollLocation[1]
                val viewportBottom = viewportTop + scrollView.height

                when {
                    itemTop < viewportTop + threshold ->
                        scrollView.scrollBy(0, -scrollSpeed)
                    itemBottom > viewportBottom - threshold ->
                        scrollView.scrollBy(0, scrollSpeed)
                }
                dragScrollHandler.postDelayed(this, 16)
            }
        }
        dragAutoScrollRunnable = runnable
        dragScrollHandler.post(runnable)
    }

    private fun stopDragAutoScroll() {
        dragAutoScrollRunnable?.let { dragScrollHandler.removeCallbacks(it) }
        dragAutoScrollRunnable = null
    }

    private fun initBookmarks() {
        lifecycleScope.launch {
            val bookmarks = try {
                withContext(Dispatchers.IO) {
                  PlaylistsHelper.getPlaylistBookmarks()
                }
            } catch (e: Exception) {
                context?.toastFromMainDispatcher(e.message.orEmpty())
                return@launch
            }

            val binding = _binding ?: return@launch

            binding.bookmarksContainer.isVisible = bookmarks.isNotEmpty()
            if (bookmarks.isNotEmpty()) {
                playlistBookmarkAdapter.submitList(bookmarks)
            }
        }
    }

    private fun updateFABMargin(isMiniPlayerVisible: Boolean) {
        // optimize CreatePlaylistFab bottom margin if miniPlayer active
        binding.createPlaylist.updateLayoutParams<MarginLayoutParams> {
            bottomMargin = (if (isMiniPlayerVisible) 64f else 16f).dpToPx()
        }
    }

    private fun setupManualSorting() {
        val defaultSortOrder = resources.getStringArray(R.array.playlistSortingOptionsValues).first()
        val manualSortingEnabled = PreferenceHelper.getString( PreferenceKeys.PLAYLISTS_ORDER,
            defaultSortOrder) == "manual"

        val itemTouchHelper = binding.playlistRecView.setOnDraggedListener(
            onDragListener = { from, to ->
                val playlists = playlistsAdapter.currentList.toMutableList()
                playlists.move(from, to)
                playlistsAdapter.submitList(playlists)

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                      PlaylistsHelper.reorderPlaylists(playlists)
                    }
                }
            },
            isDragEnabled = {
                PreferenceHelper.getString(PreferenceKeys.PLAYLISTS_ORDER, defaultSortOrder) == "manual"
            },
            isLongPressDragEnabled = false,
            onDragStateChanged = { isDragging, viewHolder ->
                draggedViewHolder = if (isDragging) viewHolder else null
                if (isDragging) {
                    startDragAutoScroll()
                } else {
                    stopDragAutoScroll()
                }
            }
        )

        playlistsAdapter.onStartDrag = if (manualSortingEnabled) {
            { itemTouchHelper.startDrag(it) }
        } else {
            null
        }
        playlistsAdapter.notifyDataSetChanged()
    }

    private fun fetchPlaylists() {
        _binding?.playlistRefresh?.isRefreshing = true
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val playlists = try {
                    withContext(Dispatchers.IO) {
                        PlaylistsHelper.getPlaylists()
                    }
                } catch (e: Exception) {
                    Log.e(TAG(), e.toString())
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@repeatOnLifecycle
                }

                val binding = _binding ?: return@repeatOnLifecycle
                binding.playlistRefresh.isRefreshing = false

                // also update playlists recycler when the playlists are empty in order to remove
                // playlists that were removed by the user
                showPlaylists(playlists)
                if (playlists.isEmpty()) {
                    binding.sortTV.isVisible = false
                    binding.nothingHere.isVisible = true
                }
            }
        }
    }

    private fun showPlaylists(playlists: List<Playlists>) {
        val binding = _binding ?: return

        binding.nothingHere.isGone = true
        binding.sortTV.isVisible = true
        playlistsAdapter.submitList(playlists)
    }
}
