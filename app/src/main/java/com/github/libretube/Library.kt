package com.github.libretube

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.libretube.adapters.PlaylistsAdapter
import com.github.libretube.obj.Playlists
import retrofit2.HttpException
import java.io.IOException


class Library : Fragment() {

    private val TAG = "LibraryFragment"
    lateinit var token: String
    lateinit var playlistRecyclerView: RecyclerView
    lateinit var refreshLayout: SwipeRefreshLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playlistRecyclerView = view.findViewById(R.id.playlist_recView)
        playlistRecyclerView.layoutManager = LinearLayoutManager(view.context)
        val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
        token = sharedPref?.getString("token","")!!
        if(token!="") {
            refreshLayout = view.findViewById(R.id.playlist_refresh)
            view.findViewById<ImageView>(R.id.boogh2).visibility=View.GONE
            view.findViewById<TextView>(R.id.textLike2).visibility=View.GONE
            fetchPlaylists(view)
            refreshLayout?.isEnabled = true
            refreshLayout?.setOnRefreshListener {
                Log.d(TAG,"hmm")
                fetchPlaylists(view)
            }
            view.findViewById<Button>(R.id.create_playlist).setOnClickListener {
                val newFragment = CreatePlaylistDialog()
                newFragment.show(childFragmentManager, "Create Playlist")
            }
            childFragmentManager.setFragmentResultListener("key_parent", this) { _, result->
                val playlistName = result.getString("playlistName")
                createPlaylist("$playlistName", view);
            }
        } else{
            with(view.findViewById<ImageView>(R.id.boogh2)){
                visibility=View.VISIBLE
                setImageResource(R.drawable.ic_login)
            }
            with(view.findViewById<TextView>(R.id.textLike2)){
                visibility=View.VISIBLE
                text = getString(R.string.please_login)
            }
        }
    }

    private fun fetchPlaylists(view: View){
        fun run() {
            refreshLayout?.isRefreshing = true
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.playlists(token)
                }catch(e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    Toast.makeText(context,R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    Toast.makeText(context,R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                }finally {
                    refreshLayout?.isRefreshing = false
                }
                if (response.isNotEmpty()){
                    runOnUiThread {
                        with(view.findViewById<ImageView>(R.id.boogh2)){
                            visibility=View.GONE
                        }
                        with(view.findViewById<TextView>(R.id.textLike2)){
                            visibility=View.GONE
                        }
                    }
                    val playlistsAdapter = PlaylistsAdapter(response.toMutableList(),requireActivity())
                    playlistRecyclerView.adapter= playlistsAdapter
                }else{
                    runOnUiThread {
                        with(view.findViewById<ImageView>(R.id.boogh2)){
                            visibility=View.VISIBLE
                            setImageResource(R.drawable.ic_list)
                        }
                        with(view.findViewById<TextView>(R.id.textLike2)){
                            visibility=View.VISIBLE
                            text = getString(R.string.emptyList)
                        }
                    }
                }

            }
        }
        run()
    }
    private fun createPlaylist(name: String, view: View){
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.createPlaylist(token, Playlists(name = name))
                }catch(e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    Toast.makeText(context,R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response $e")
                    Toast.makeText(context,R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                }
                if (response != null){
                    Toast.makeText(context,R.string.playlistCreated, Toast.LENGTH_SHORT).show()
                    fetchPlaylists(view)
                }else{

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
