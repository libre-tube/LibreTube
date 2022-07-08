package com.github.libretube.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.adapters.PlaylistAdapter
import com.github.libretube.databinding.FragmentPlaylistBinding
import com.github.libretube.dialogs.PlaylistOptionsDialog
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.util.RetrofitInstance
import retrofit2.HttpException
import java.io.IOException

class PlaylistFragment : Fragment() {
    private val TAG = "PlaylistFragment"
    private lateinit var binding: FragmentPlaylistBinding

    private var playlistId: String? = null
    var nextPage: String? = null
    private var playlistAdapter: PlaylistAdapter? = null
    private var isLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistId = it.getString("playlist_id")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = playlistId!!.replace("/playlist?list=", "")
        binding.playlistRecView.layoutManager = LinearLayoutManager(context)

        binding.playlistProgress.visibility = View.VISIBLE
        fetchPlaylist()
    }

    private fun fetchPlaylist() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getPlaylist(playlistId!!)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                isLoading = false
                runOnUiThread {
                    binding.playlistProgress.visibility = View.GONE
                    binding.playlistName.text = response.name
                    binding.playlistUploader.text = response.uploader
                    binding.playlistTotVideos.text =
                        getString(R.string.videoCount, response.videos.toString())

                    // show playlist options
                    binding.optionsMenu.setOnClickListener {
                        val newFragment = PlaylistOptionsDialog(playlistId!!, requireContext())
                        newFragment.show(childFragmentManager, "PlaylistOptionsDialog")
                    }
                    val user = PreferenceHelper.getUsername(requireContext())

                    // check whether the user owns the playlist
                    val isOwner = response.uploaderUrl == null &&
                    response.uploader.equals(user, true)

                    playlistAdapter = PlaylistAdapter(
                        response.relatedStreams!!.toMutableList(),
                        playlistId!!,
                        isOwner,
                        requireActivity(),
                        childFragmentManager
                    )
                    binding.playlistRecView.adapter = playlistAdapter
                    binding.playlistScrollview.viewTreeObserver
                        .addOnScrollChangedListener {
                            if (binding.playlistScrollview.getChildAt(0).bottom
                                == (binding.playlistScrollview.height + binding.playlistScrollview.scrollY)
                            ) {
                                // scroll view is at bottom
                                if (nextPage != null && !isLoading) {
                                    isLoading = true
                                    fetchNextPage()
                                }
                            } else {
                                // scroll view is not at bottom
                            }
                        }
                }
            }
        }
        run()
    }

    private fun fetchNextPage() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getPlaylistNextPage(playlistId!!, nextPage!!)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response," + e.response())
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                playlistAdapter?.updateItems(response.relatedStreams!!)
                isLoading = false
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
