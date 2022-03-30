package com.github.libretube.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.libretube.R
import com.github.libretube.RetrofitInstance
import com.github.libretube.adapters.SubscriptionAdapter
import com.github.libretube.adapters.SubscriptionChannelAdapter
import retrofit2.HttpException
import java.io.IOException

class SubscriptionsFragment : Fragment() {
    val TAG = "SubFragment"
    lateinit var token: String
    var isLoaded = false
    private var subscriptionAdapter: SubscriptionAdapter? = null
    private var refreshLayout: SwipeRefreshLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_subscriptions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
        token = sharedPref?.getString("token", "")!!
        Log.e(TAG, token)
        refreshLayout = view.findViewById(R.id.sub_refresh)
        if (token != "") {
            view.findViewById<RelativeLayout>(R.id.loginOrRegister).visibility = View.GONE
            refreshLayout?.isEnabled = true

            val progressBar = view.findViewById<ProgressBar>(R.id.sub_progress)
            progressBar.visibility = View.VISIBLE

            val channelRecView = view.findViewById<RecyclerView>(R.id.sub_channels)
            channelRecView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            fetchChannels(channelRecView)

            val feedRecView = view.findViewById<RecyclerView>(R.id.sub_feed)
            feedRecView.layoutManager = GridLayoutManager(view.context, resources.getInteger(R.integer.grid_items))
            fetchFeed(feedRecView, progressBar)

            refreshLayout?.setOnRefreshListener {
                fetchChannels(channelRecView)
                fetchFeed(feedRecView, progressBar)
            }

            val scrollView = view.findViewById<ScrollView>(R.id.scrollview_sub)
            scrollView.viewTreeObserver
                .addOnScrollChangedListener {
                    if (scrollView.getChildAt(0).bottom
                        == (scrollView.height + scrollView.scrollY)
                    ) {
                        // scroll view is at bottom
                        if (isLoaded) {
                            refreshLayout?.isRefreshing = true
                            subscriptionAdapter?.updateItems()
                            refreshLayout?.isRefreshing = false
                        }
                    }
                }
        } else {
            refreshLayout?.isEnabled = false
        }
    }

    private fun fetchFeed(feedRecView: RecyclerView, progressBar: ProgressBar) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getFeed(token)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                } finally {
                    refreshLayout?.isRefreshing = false
                }
                if (response.isNotEmpty()) {
                    subscriptionAdapter = SubscriptionAdapter(response)
                    feedRecView.adapter = subscriptionAdapter
                    subscriptionAdapter?.updateItems()
                }
                progressBar.visibility = View.GONE
                isLoaded = true
            }
        }
        run()
    }

    private fun fetchChannels(channelRecView: RecyclerView) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.subscriptions(token)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                } finally {
                    refreshLayout?.isRefreshing = false
                }
                if (response.isNotEmpty()) {
                    channelRecView.adapter = SubscriptionChannelAdapter(response.toMutableList())
                } else {
                    Toast.makeText(context, R.string.subscribeIsEmpty, Toast.LENGTH_SHORT).show()
                }
            }
        }
        run()
    }

    override fun onStop() {
        Log.e(TAG, "Stopped")
        subscriptionAdapter = null
        view?.findViewById<RecyclerView>(R.id.sub_feed)?.adapter = null
        super.onStop()
    }
    override fun onDestroy() {
        Log.e(TAG, "Destroyed")
        super.onDestroy()
    }
}
