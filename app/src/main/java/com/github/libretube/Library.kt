package com.github.libretube

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.adapters.ChannelAdapter
import com.squareup.picasso.Picasso
import retrofit2.HttpException
import java.io.IOException


class Library : Fragment() {

    private val TAG = "LibraryFragment"
    lateinit var token: String
    lateinit var playlistRecyclerView: RecyclerView
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
        val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
        token = sharedPref?.getString("token","")!!
        if(token!="") {
            fetchPlaylists(view)
        }
    }

    private fun fetchPlaylists(view: View){
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.playlists(token)
                }catch(e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                runOnUiThread {

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