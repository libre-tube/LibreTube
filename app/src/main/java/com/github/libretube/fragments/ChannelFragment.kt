package com.github.libretube.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.libretube.R
import com.github.libretube.adapters.ChannelAdapter
import com.github.libretube.obj.Subscribe
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.formatShort
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso
import java.io.IOException
import retrofit2.HttpException

class ChannelFragment : Fragment() {

    private var channel_id: String? = null
    private val TAG = "ChannelFragment"
    var nextPage: String? = null
    var channelAdapter: ChannelAdapter? = null
    var isLoading = true
    var isSubscribed: Boolean = false
    private var refreshLayout: SwipeRefreshLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            channel_id = it.getString("channel_id")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_channel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        channel_id = channel_id!!.replace("/channel/", "")
        view.findViewById<TextView>(R.id.channel_name).text = channel_id
        val recyclerView = view.findViewById<RecyclerView>(R.id.channel_recView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        refreshLayout = view.findViewById(R.id.channel_refresh)

        val refreshChannel = {
            refreshLayout?.isRefreshing = true
            fetchChannel(view)
            val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
            val subButton = view.findViewById<MaterialButton>(R.id.channel_subscribe)
            if (sharedPref?.getString("token", "") != "") {
                isSubscribed(subButton)
            }
        }
        refreshChannel()
        refreshLayout?.setOnRefreshListener {
            refreshChannel()
        }

        val scrollView = view.findViewById<ScrollView>(R.id.channel_scrollView)
        scrollView.viewTreeObserver
            .addOnScrollChangedListener {
                if (scrollView.getChildAt(0).bottom
                    == (scrollView.height + scrollView.scrollY)
                ) {
                    // scroll view is at bottom
                    if (nextPage != null && !isLoading) {
                        isLoading = true
                        refreshLayout?.isRefreshing = true
                        fetchNextPage()
                    }
                }
            }
    }

    private fun isSubscribed(button: MaterialButton) {
        @SuppressLint("ResourceAsColor")
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.isSubscribed(
                        channel_id!!,
                        sharedPref?.getString("token", "")!!
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }

                runOnUiThread {
                    if (response.subscribed == true) {
                        isSubscribed = true
                        button.text = getString(R.string.unsubscribe)
                    }
                    if (response.subscribed != null) {
                        button.setOnClickListener {
                            if (isSubscribed) {
                                unsubscribe()
                                button.text = getString(R.string.subscribe)
                            } else {
                                subscribe()
                                button.text = getString(R.string.unsubscribe)
                            }
                        }
                    }
                }
            }
        }
        run()
    }

    private fun subscribe() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.subscribe(
                        sharedPref?.getString("token", "")!!,
                        Subscribe(channel_id)
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response$e")
                    return@launchWhenCreated
                }
                isSubscribed = true
            }
        }
        run()
    }

    private fun unsubscribe() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.unsubscribe(
                        sharedPref?.getString("token", "")!!,
                        Subscribe(channel_id)
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                isSubscribed = false
            }
        }
        run()
    }

    private fun fetchChannel(view: View) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getChannel(channel_id!!)
                } catch (e: IOException) {
                    refreshLayout?.isRefreshing = false
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    refreshLayout?.isRefreshing = false
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                isLoading = false
                refreshLayout?.isRefreshing = false
                runOnUiThread {
                    view.findViewById<ScrollView>(R.id.channel_scrollView).visibility = View.VISIBLE
                    val channelName = view.findViewById<TextView>(R.id.channel_name)
                    channelName.text = response.name
                    if (response.verified) {
                        channelName.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_verified, 0
                        )
                    }
                    view.findViewById<TextView>(R.id.channel_subs).text = resources.getString(
                        R.string.subscribers,
                        response.subscriberCount.formatShort()
                    )
                    val channelDescription = view.findViewById<TextView>(R.id.channel_description)
                    if (response.description?.trim() == "") {
                        channelDescription.visibility = View.GONE
                    } else {
                        channelDescription.text = response.description?.trim()
                    }
                    val bannerImage = view.findViewById<ImageView>(R.id.channel_banner)
                    val channelImage = view.findViewById<ImageView>(R.id.channel_image)
                    Picasso.get().load(response.bannerUrl).into(bannerImage)
                    Picasso.get().load(response.avatarUrl).into(channelImage)
                    channelAdapter = ChannelAdapter(
                        response.relatedStreams!!.toMutableList(),
                        childFragmentManager
                    )
                    view.findViewById<RecyclerView>(R.id.channel_recView).adapter = channelAdapter
                }
            }
        }
        run()
    }

    private fun fetchNextPage() {
        fun run() {

            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getChannelNextPage(channel_id!!, nextPage!!)
                } catch (e: IOException) {
                    refreshLayout?.isRefreshing = false
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    refreshLayout?.isRefreshing = false
                    Log.e(TAG, "HttpException, unexpected response," + e.response())
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                channelAdapter?.updateItems(response.relatedStreams!!)
                isLoading = false
                refreshLayout?.isRefreshing = false
            }
        }
        run()
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }

    override fun onDestroyView() {
        val scrollView = view?.findViewById<ScrollView>(R.id.channel_scrollView)
        scrollView?.viewTreeObserver?.removeOnScrollChangedListener {
        }
        channelAdapter = null
        view?.findViewById<RecyclerView>(R.id.channel_recView)?.adapter = null
        super.onDestroyView()
    }
}
