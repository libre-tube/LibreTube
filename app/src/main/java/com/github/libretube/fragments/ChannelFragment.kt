package com.github.libretube.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.adapters.ChannelAdapter
import com.github.libretube.databinding.FragmentChannelBinding
import com.github.libretube.obj.Subscribe
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.formatShort
import retrofit2.HttpException
import java.io.IOException

class ChannelFragment : Fragment() {
    private val TAG = "ChannelFragment"
    private lateinit var binding: FragmentChannelBinding

    private var channelId: String? = null
    var nextPage: String? = null
    private var channelAdapter: ChannelAdapter? = null
    private var isLoading = true
    private var isSubscribed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            channelId = it.getString("channel_id")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChannelBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        channelId = channelId!!.replace("/channel/", "")
        binding.channelName.text = channelId
        binding.channelRecView.layoutManager = LinearLayoutManager(context)

        val refreshChannel = {
            binding.channelRefresh.isRefreshing = true
            fetchChannel()
            if (PreferenceHelper.getToken(requireContext()) != "") {
                isSubscribed()
            }
        }
        refreshChannel()
        binding.channelRefresh.setOnRefreshListener {
            refreshChannel()
        }

        binding.channelScrollView.viewTreeObserver
            .addOnScrollChangedListener {
                if (binding.channelScrollView.getChildAt(0).bottom
                    == (binding.channelScrollView.height + binding.channelScrollView.scrollY)
                ) {
                    // scroll view is at bottom
                    if (nextPage != null && !isLoading) {
                        isLoading = true
                        binding.channelRefresh.isRefreshing = true
                        fetchNextPage()
                    }
                }
            }
    }

    private fun isSubscribed() {
        @SuppressLint("ResourceAsColor")
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val token = PreferenceHelper.getToken(requireContext())
                    RetrofitInstance.authApi.isSubscribed(
                        channelId!!,
                        token
                    )
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                    return@launchWhenCreated
                }

                runOnUiThread {
                    if (response.subscribed == true) {
                        isSubscribed = true
                        binding.channelSubscribe.text = getString(R.string.unsubscribe)
                    }
                    if (response.subscribed != null) {
                        binding.channelSubscribe.apply {
                            setOnClickListener {
                                text = if (isSubscribed) {
                                    unsubscribe()
                                    getString(R.string.subscribe)
                                } else {
                                    subscribe()
                                    getString(R.string.unsubscribe)
                                }
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
                try {
                    val token = PreferenceHelper.getToken(requireContext())
                    RetrofitInstance.authApi.subscribe(
                        token,
                        Subscribe(channelId)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
                isSubscribed = true
            }
        }
        run()
    }

    private fun unsubscribe() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                try {
                    val token = PreferenceHelper.getToken(requireContext())
                    RetrofitInstance.authApi.unsubscribe(
                        token,
                        Subscribe(channelId)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
                isSubscribed = false
            }
        }
        run()
    }

    private fun fetchChannel() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getChannel(channelId!!)
                } catch (e: IOException) {
                    binding.channelRefresh.isRefreshing = false
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    binding.channelRefresh.isRefreshing = false
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                isLoading = false
                binding.channelRefresh.isRefreshing = false
                runOnUiThread {
                    binding.channelScrollView.visibility = View.VISIBLE
                    binding.channelName.text = response.name
                    if (response.verified) {
                        binding.channelName.setCompoundDrawablesWithIntrinsicBounds(
                            0,
                            0,
                            R.drawable.ic_verified,
                            0
                        )
                    }
                    binding.channelSubs.text = resources.getString(
                        R.string.subscribers,
                        response.subscriberCount.formatShort()
                    )
                    if (response.description?.trim() == "") {
                        binding.channelDescription.visibility = View.GONE
                    } else {
                        binding.channelDescription.text = response.description?.trim()
                    }

                    ConnectionHelper.loadImage(response.bannerUrl, binding.channelBanner)
                    ConnectionHelper.loadImage(response.avatarUrl, binding.channelImage)
                    channelAdapter = ChannelAdapter(
                        response.relatedStreams!!.toMutableList(),
                        childFragmentManager
                    )
                    binding.channelRecView.adapter = channelAdapter
                }
            }
        }
        run()
    }

    private fun fetchNextPage() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getChannelNextPage(channelId!!, nextPage!!)
                } catch (e: IOException) {
                    binding.channelRefresh.isRefreshing = false
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    binding.channelRefresh.isRefreshing = false
                    Log.e(TAG, "HttpException, unexpected response," + e.response())
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                channelAdapter?.updateItems(response.relatedStreams!!)
                isLoading = false
                binding.channelRefresh.isRefreshing = false
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
