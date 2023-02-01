package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentLibraryBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.awaitQuery
import com.github.libretube.extensions.dpToPx
import com.github.libretube.helpers.NavBarHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.adapters.PlaylistBookmarkAdapter
import com.github.libretube.ui.adapters.PlaylistsAdapter
import com.github.libretube.ui.base.BaseFragment
import com.github.libretube.ui.dialogs.CreatePlaylistDialog
import com.github.libretube.ui.models.PlayerViewModel

class LibraryFragment : BaseFragment() {

    private lateinit var binding: FragmentLibraryBinding
    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLibraryBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // initialize the layout managers
        binding.bookmarksRecView.layoutManager = LinearLayoutManager(context)
        binding.playlistRecView.layoutManager = LinearLayoutManager(context)

        // listen for the mini player state changing
        playerViewModel.isMiniPlayerVisible.observe(viewLifecycleOwner) {
            updateFABMargin(it)
        }

        // hide watch history button of history disabled
        val watchHistoryEnabled =
            PreferenceHelper.getBoolean(PreferenceKeys.WATCH_HISTORY_TOGGLE, true)
        if (!watchHistoryEnabled) {
            binding.watchHistory.visibility = View.GONE
        } else {
            binding.watchHistory.setOnClickListener {
                findNavController().navigate(R.id.watchHistoryFragment)
            }
        }

        binding.downloads.setOnClickListener {
            findNavController().navigate(R.id.downloadsFragment)
        }

        val navBarItems = NavBarHelper.getNavBarItems(requireContext())
        if (navBarItems.filter { it.isVisible }.any { it.itemId == R.id.downloadsFragment }) {
            binding.downloads.visibility = View.GONE
        }

        fetchPlaylists()
        initBookmarks()

        binding.playlistRefresh.isEnabled = true
        binding.playlistRefresh.setOnRefreshListener {
            fetchPlaylists()
            initBookmarks()
        }
        binding.createPlaylist.setOnClickListener {
            CreatePlaylistDialog {
                fetchPlaylists()
            }.show(childFragmentManager, CreatePlaylistDialog::class.java.name)
        }
    }

    private fun initBookmarks() {
        val bookmarks = awaitQuery {
            DatabaseHolder.Database.playlistBookmarkDao().getAll()
        }

        binding.bookmarksCV.visibility = if (bookmarks.isEmpty()) View.GONE else View.VISIBLE
        if (bookmarks.isEmpty()) return

        binding.bookmarksRecView.adapter = PlaylistBookmarkAdapter(bookmarks)
    }

    private fun updateFABMargin(isMiniPlayerVisible: Boolean) {
        // optimize CreatePlaylistFab bottom margin if miniPlayer active
        val bottomMargin = if (isMiniPlayerVisible) 64 else 16
        binding.createPlaylist.updateLayoutParams<MarginLayoutParams> {
            this.bottomMargin = bottomMargin.dpToPx().toInt()
        }
    }

    private fun fetchPlaylists() {
        binding.playlistRefresh.isRefreshing = true
        lifecycleScope.launchWhenCreated {
            var playlists = try {
                PlaylistsHelper.getPlaylists()
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } finally {
                binding.playlistRefresh.isRefreshing = false
            }
            if (playlists.isNotEmpty()) {
                playlists = when (
                    PreferenceHelper.getString(
                        PreferenceKeys.PLAYLISTS_ORDER,
                        "recent"
                    )
                ) {
                    "recent" -> playlists
                    "recent_reversed" -> playlists.reversed()
                    "name" -> playlists.sortedBy { it.name?.lowercase() }
                    "name_reversed" -> playlists.sortedBy { it.name?.lowercase() }.reversed()
                    else -> playlists
                }

                val playlistsAdapter = PlaylistsAdapter(
                    playlists.toMutableList(),
                    PlaylistsHelper.getPrivatePlaylistType()
                )

                // listen for playlists to become deleted
                playlistsAdapter.registerAdapterDataObserver(object :
                    RecyclerView.AdapterDataObserver() {
                    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                        binding.nothingHere.visibility =
                            if (playlistsAdapter.itemCount == 0) View.VISIBLE else View.GONE
                        super.onItemRangeRemoved(positionStart, itemCount)
                    }
                })

                binding.nothingHere.visibility = View.GONE
                binding.playlistRecView.adapter = playlistsAdapter
            } else {
                runOnUiThread {
                    binding.nothingHere.visibility = View.VISIBLE
                }
            }
        }
    }
}
