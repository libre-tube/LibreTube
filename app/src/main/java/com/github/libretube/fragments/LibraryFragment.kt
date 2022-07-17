package com.github.libretube.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.Globals
import com.github.libretube.R
import com.github.libretube.adapters.PlaylistsAdapter
import com.github.libretube.databinding.FragmentLibraryBinding
import com.github.libretube.dialogs.CreatePlaylistDialog
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import com.github.libretube.util.RetrofitInstance
import retrofit2.HttpException
import java.io.IOException

class LibraryFragment : Fragment() {

    private val TAG = "LibraryFragment"
    lateinit var token: String
    private lateinit var binding: FragmentLibraryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

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
        binding.playlistRecView.layoutManager = LinearLayoutManager(view.context)
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

        if (token != "") {
            binding.boogh.visibility = View.GONE
            binding.textLike.visibility = View.GONE
            fetchPlaylists()
            binding.playlistRefresh.isEnabled = true
            binding.playlistRefresh.setOnRefreshListener {
                fetchPlaylists()
            }
            binding.createPlaylist.setOnClickListener {
                val newFragment = CreatePlaylistDialog()
                newFragment.show(childFragmentManager, "Create Playlist")
            }
        } else {
            binding.playlistRefresh.isEnabled = false
            binding.createPlaylist.visibility = View.GONE
        }
    }

    override fun onResume() {
        // optimize CreatePlaylistFab bottom margin if miniPlayer active
        val layoutParams = binding.createPlaylist.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = if (Globals.isMiniPlayerVisible) 180 else 64
        binding.createPlaylist.layoutParams = layoutParams
        super.onResume()
    }

    fun fetchPlaylists() {
        fun run() {
            binding.playlistRefresh.isRefreshing = true
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.authApi.playlists(token)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } finally {
                    binding.playlistRefresh.isRefreshing = false
                }
                if (response.isNotEmpty()) {
                    runOnUiThread {
                        binding.boogh.visibility = View.GONE
                        binding.textLike.visibility = View.GONE
                    }
                    val playlistsAdapter = PlaylistsAdapter(
                        response.toMutableList(),
                        requireActivity()
                    )
                    binding.playlistRecView.adapter = playlistsAdapter
                } else {
                    runOnUiThread {
                        binding.boogh.apply {
                            visibility = View.VISIBLE
                            setImageResource(R.drawable.ic_list)
                        }
                        binding.textLike.apply {
                            visibility = View.VISIBLE
                            text = getString(R.string.emptyList)
                        }
                    }
                }
            }
        }
        run()
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }
}
