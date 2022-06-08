package com.github.libretube.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.libretube.R
import com.github.libretube.adapters.SubscriptionAdapter
import com.github.libretube.adapters.SubscriptionChannelAdapter
import com.github.libretube.util.RetrofitInstance
import java.io.IOException
import retrofit2.HttpException

class Subscriptions : Fragment() {
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
        refreshLayout = view.findViewById(R.id.sub_refresh)
        if (token != "") {
            view.findViewById<RelativeLayout>(R.id.loginOrRegister).visibility = View.GONE
            refreshLayout?.isEnabled = true

            var progressBar = view.findViewById<ProgressBar>(R.id.sub_progress)
            progressBar.visibility = View.VISIBLE

            var channelRecView = view.findViewById<RecyclerView>(R.id.sub_channels)

            var feedRecView = view.findViewById<RecyclerView>(R.id.sub_feed)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val grid = sharedPreferences.getString(
                "grid", resources.getInteger(R.integer.grid_items).toString()
            )!!
            feedRecView.layoutManager = GridLayoutManager(view.context, grid.toInt())
            fetchFeed(feedRecView, progressBar, view)

            refreshLayout?.setOnRefreshListener {
                fetchChannels(channelRecView)
                fetchFeed(feedRecView, progressBar, view)
            }

            var toggleSubs = view.findViewById<RelativeLayout>(R.id.toggle_subs)
            toggleSubs.visibility = View.VISIBLE
            var loadedSubbedChannels = false
            toggleSubs.setOnClickListener {
                if (!channelRecView.isVisible) {
                    if (!loadedSubbedChannels) {
                        channelRecView?.layoutManager = LinearLayoutManager(context)
                        fetchChannels(channelRecView)
                        loadedSubbedChannels = true
                    }
                    channelRecView.visibility = View.VISIBLE
                    feedRecView.visibility = View.GONE

                    // toggle button
                    val rotate = RotateAnimation(
                        0F,
                        180F,
                        Animation.RELATIVE_TO_SELF,
                        0.5f,
                        Animation.RELATIVE_TO_SELF,
                        0.5f
                    )
                    rotate.duration = 100
                    rotate.interpolator = LinearInterpolator()
                    rotate.fillAfter = true
                    val image = view.findViewById<ImageView>(R.id.toggle)
                    image.startAnimation(rotate)
                } else {
                    channelRecView.visibility = View.GONE
                    feedRecView.visibility = View.VISIBLE

                    // toggle button
                    val image = view.findViewById<ImageView>(R.id.toggle)
                    image.clearAnimation()
                }
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

    private fun fetchFeed(feedRecView: RecyclerView, progressBar: ProgressBar, view: View) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getFeed(token)
                } catch (e: IOException) {
                    Log.e(TAG, e.toString())
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                } finally {
                    refreshLayout?.isRefreshing = false
                }
                if (response.isNotEmpty()) {
                    subscriptionAdapter = SubscriptionAdapter(response, childFragmentManager)
                    feedRecView.adapter = subscriptionAdapter
                    subscriptionAdapter?.updateItems()
                } else {
                    runOnUiThread {
                        with(view.findViewById<ImageView>(R.id.boogh)) {
                            visibility = View.VISIBLE
                            setImageResource(R.drawable.ic_list)
                        }
                        with(view.findViewById<TextView>(R.id.textLike)) {
                            visibility = View.VISIBLE
                            text = getString(R.string.emptyList)
                        }
                        view.findViewById<RelativeLayout>(R.id.loginOrRegister)
                            .visibility = View.VISIBLE
                    }
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
                    Log.e(TAG, e.toString())
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

    override fun onDestroy() {
        Log.e(TAG, "Destroyed")
        super.onDestroy()
        subscriptionAdapter = null
        view?.findViewById<RecyclerView>(R.id.sub_feed)?.adapter = null
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }
}
