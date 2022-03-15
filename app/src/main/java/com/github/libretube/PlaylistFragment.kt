package com.github.libretube

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.adapters.PlaylistAdapter
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "PlaylistFragment"

class PlaylistFragment : Fragment() {
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = playlistId!!.replace("/playlist?list=", "")
        view.findViewById<TextView>(R.id.playlist_name).text = playlistId
        val recyclerView = view.findViewById<RecyclerView>(R.id.playlist_recView)
        recyclerView.layoutManager = LinearLayoutManager(context)

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
                    view.findViewById<TextView>(R.id.playlist_name).text = response.name
                    view.findViewById<TextView>(R.id.playlist_uploader).text = response.uploader
                    view.findViewById<TextView>(R.id.playlist_totVideos).text =
                        response.videos.toString() + " Videos"
                    playlistAdapter = PlaylistAdapter(response.relatedStreams!!.toMutableList())
                    view.findViewById<RecyclerView>(R.id.playlist_recView).adapter = playlistAdapter
                    val scrollView = view.findViewById<ScrollView>(R.id.playlist_scrollview)
                    scrollView.viewTreeObserver
                        .addOnScrollChangedListener {
                            if (scrollView.getChildAt(0).bottom
                                == (scrollView.height + scrollView.scrollY)
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

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }
}