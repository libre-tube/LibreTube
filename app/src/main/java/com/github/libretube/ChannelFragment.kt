package com.github.libretube

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.github.libretube.adapters.TrendingAdapter
import com.squareup.picasso.Picasso
import retrofit2.HttpException
import java.io.IOException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

/**
 * A simple [Fragment] subclass.
 * Use the [ChannelFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChannelFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var channel_id: String? = null
    private val TAG = "ChannelFragment"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            channel_id = it.getString("channel_id")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_channel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        channel_id = channel_id!!.replace("/channel/","")
        view.findViewById<TextView>(R.id.channel_name).text=channel_id
        fetchChannel(view)
    }

    private fun fetchChannel(view: View){
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getChannel(channel_id!!)
                }catch(e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                runOnUiThread {
                    view.findViewById<TextView>(R.id.channel_name).text=response.name
                    view.findViewById<TextView>(R.id.channel_subs).text=response.subscriberCount.videoViews() + " subscribers"
                    view.findViewById<TextView>(R.id.channel_description).text=response.description
                    val bannerImage = view.findViewById<ImageView>(R.id.channel_banner)
                    val channelImage = view.findViewById<ImageView>(R.id.channel_image)
                    Picasso.get().load(response.bannerUrl).into(bannerImage)
                    Picasso.get().load(response.avatarUrl).into(channelImage)
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