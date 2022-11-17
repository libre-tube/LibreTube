package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentLibraryBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toDp
import com.github.libretube.ui.adapters.PlaylistsAdapter
import com.github.libretube.ui.base.BaseFragment
import com.github.libretube.ui.dialogs.CreatePlaylistDialog
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.util.PreferenceHelper
import retrofit2.HttpException
import java.io.IOException

class LibraryFragment : BaseFragment() {

    lateinit var token: String
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

        // listen for the mini player state changing
        playerViewModel.isMiniPlayerVisible.observe(viewLifecycleOwner) {
            updateFABMargin(it)
        }

        binding.playlistRecView.layoutManager = LinearLayoutManager(requireContext())
        token = PreferenceHelper.getToken()

        // hide watch history button of history disabled
        val watchHistoryEnabled =
            PreferenceHelper.getBoolean(PreferenceKeys.WATCH_HISTORY_TOGGLE, true)
        if (!watchHistoryEnabled) {
            binding.showWatchHistory.visibility = View.GONE
        } else {
            binding.showWatchHistory.setOnClickListener {
                findNavController().navigate(R.id.watchHistoryFragment)
            }
        }

        binding.downloads.setOnClickListener {
            findNavController().navigate(R.id.downloadsFragment)
        }

        if (token != "") {
            binding.boogh.setImageResource(R.drawable.ic_list)
            binding.textLike.text = getString(R.string.emptyList)

            binding.loginOrRegister.visibility = View.GONE
            fetchPlaylists()

            binding.playlistRefresh.isEnabled = true
            binding.playlistRefresh.setOnRefreshListener {
                fetchPlaylists()
            }
            binding.createPlaylist.setOnClickListener {
                val newFragment = CreatePlaylistDialog()
                newFragment.show(childFragmentManager, CreatePlaylistDialog::class.java.name)
            }
        } else {
            binding.playlistRefresh.isEnabled = false
            binding.createPlaylist.visibility = View.GONE
        }
    }

    private fun updateFABMargin(isMiniPlayerVisible: Boolean) {
        // optimize CreatePlaylistFab bottom margin if miniPlayer active
        val bottomMargin = if (isMiniPlayerVisible) 64 else 16
        val layoutParams = binding.createPlaylist.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = bottomMargin.toDp(resources).toInt()
        binding.createPlaylist.layoutParams = layoutParams
    }

    fun fetchPlaylists() {
        binding.playlistRefresh.isRefreshing = true
        lifecycleScope.launchWhenCreated {
            var playlists = try {
                RetrofitInstance.authApi.getUserPlaylists(token)
            } catch (e: IOException) {
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection")
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response")
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } finally {
                binding.playlistRefresh.isRefreshing = false
            }
            if (playlists.isNotEmpty()) {
                binding.loginOrRegister.visibility = View.GONE

                playlists = when (
                    PreferenceHelper.getString(
                        PreferenceKeys.PLAYLISTS_ORDER,
                        "recent"
                    )
                ) {
                    "recent" -> playlists
                    "recent_reversed" -> playlists.reversed()
                    "name" -> playlists.sortedBy { it.name }
                    "name_reversed" -> playlists.sortedBy { it.name }.reversed()
                    else -> playlists
                }

                val playlistsAdapter = PlaylistsAdapter(
                    playlists.toMutableList(),
                    childFragmentManager
                )

                // listen for playlists to become deleted
                playlistsAdapter.registerAdapterDataObserver(object :
                        RecyclerView.AdapterDataObserver() {
                        override fun onChanged() {
                            if (playlistsAdapter.itemCount == 0) {
                                binding.loginOrRegister.visibility = View.VISIBLE
                            }
                            super.onChanged()
                        }
                    })

                binding.playlistRecView.adapter = playlistsAdapter
            } else {
                runOnUiThread {
                    binding.loginOrRegister.visibility = View.VISIBLE
                }
            }
        }
    }
}
