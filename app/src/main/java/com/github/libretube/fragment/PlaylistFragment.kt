package com.github.libretube.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.RetrofitInstance
import com.github.libretube.adapters.PlaylistAdapter
import com.github.libretube.databinding.FragmentPlaylistBinding
import com.github.libretube.utils.runOnUiThread
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "PlaylistFragment"

class PlaylistFragment : Fragment() {
    private lateinit var binding: FragmentPlaylistBinding

    private var playlistId: String? = null
    private var nextPage: String? = null
    private var playlistAdapter: PlaylistAdapter? = null
    private var isLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistId = it.getString("playlist_id")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPlaylistBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playlistId = playlistId!!.replace("/playlist?list=", "")
        binding.tvPlaylistName.text = playlistId
        binding.rvPlaylist.layoutManager = LinearLayoutManager(context)
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
                    binding.tvPlaylistName.text = response.name
                    binding.tvPlaylistUploader.text = response.uploader
                    binding.tvPlaylistTotVideos.text = response.videos.toString() + " Videos"
                    playlistAdapter = PlaylistAdapter(response.relatedStreams!!.toMutableList())
                    binding.rvPlaylist.adapter = playlistAdapter

                    binding.svPlaylist.viewTreeObserver
                        .addOnScrollChangedListener {
                            if (binding.svPlaylist.getChildAt(0).bottom
                                == (binding.svPlaylist.height + binding.svPlaylist.scrollY)
                            ) {
                                //scroll view is at bottom
                                if (nextPage != null && !isLoading) {
                                    isLoading = true
                                    fetchNextPage()
                                }
                            } else {
                                //scroll view is not at bottom
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
}
