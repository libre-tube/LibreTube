package com.github.libretube.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.adapters.PlaylistAdapter
import com.github.libretube.util.RetrofitInstance
import retrofit2.HttpException
import java.io.IOException

class PlaylistFragment : Fragment() {
    private val TAG = "PlaylistFragment"

    private var playlistId: String? = null
    var nextPage: String? = null
    var playlistAdapter: PlaylistAdapter? = null
    var isLoading = true
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
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = playlistId!!.replace("/playlist?list=", "")
        val recyclerView = view.findViewById<RecyclerView>(R.id.playlist_recView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val progressBar = view.findViewById<ProgressBar>(R.id.playlist_progress)
        progressBar.visibility = View.VISIBLE
        fetchPlaylist(view)
    }

    private fun fetchPlaylist(view: View) {
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
                    view.findViewById<ProgressBar>(R.id.playlist_progress).visibility = View.GONE
                    view.findViewById<TextView>(R.id.playlist_name).text = response.name
                    view.findViewById<TextView>(R.id.playlist_uploader).text = response.uploader
                    view.findViewById<TextView>(R.id.playlist_totVideos).text =
                        getString(R.string.videoCount, response.videos.toString())
                    val sharedPref2 =
                        context?.getSharedPreferences("username", Context.MODE_PRIVATE)
                    val user = sharedPref2?.getString("username", "")
                    var isOwner = false
                    if (response.uploaderUrl == null && response.uploader.equals(user, true)) {
                        isOwner = true
                    }
                    playlistAdapter = PlaylistAdapter(
                        response.relatedStreams!!.toMutableList(),
                        playlistId!!,
                        isOwner,
                        requireActivity(),
                        childFragmentManager
                    )
                    view.findViewById<RecyclerView>(R.id.playlist_recView).adapter = playlistAdapter
                    val scrollView = view.findViewById<ScrollView>(R.id.playlist_scrollview)
                    scrollView.viewTreeObserver
                        .addOnScrollChangedListener {
                            if (scrollView.getChildAt(0).bottom
                                == (scrollView.height + scrollView.scrollY)
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
