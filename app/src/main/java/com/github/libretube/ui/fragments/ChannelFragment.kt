package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.ShareObjectType
import com.github.libretube.databinding.FragmentChannelBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.toID
import com.github.libretube.ui.adapters.ChannelAdapter
import com.github.libretube.ui.base.BaseFragment
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.util.ImageHelper
import retrofit2.HttpException
import java.io.IOException

class ChannelFragment : BaseFragment() {
    private lateinit var binding: FragmentChannelBinding

    private var channelId: String? = null
    private var channelName: String? = null

    var nextPage: String? = null
    private var channelAdapter: ChannelAdapter? = null
    private var isLoading = true
    private var isSubscribed: Boolean? = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            channelId = it.getString(IntentData.channelId)?.toID()
            channelName = it.getString(IntentData.channelName)
                ?.replace("/c/", "")
                ?.replace("/user/", "")
            Log.e(TAG(), channelName.toString())
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

        binding.channelName.text = channelId
        binding.channelRecView.layoutManager = LinearLayoutManager(context)

        val refreshChannel = {
            binding.channelRefresh.isRefreshing = true
            fetchChannel()
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
                        fetchChannelNextPage()
                    }
                }
            }
    }

    private fun fetchChannel() {
        lifecycleScope.launchWhenCreated {
            val response = try {
                if (channelId != null) {
                    RetrofitInstance.api.getChannel(channelId!!)
                } else {
                    RetrofitInstance.api.getChannelByName(channelName!!)
                }
            } catch (e: IOException) {
                binding.channelRefresh.isRefreshing = false
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection")
                return@launchWhenCreated
            } catch (e: HttpException) {
                binding.channelRefresh.isRefreshing = false
                Log.e(TAG(), "HttpException, unexpected response")
                return@launchWhenCreated
            }
            // needed if the channel gets loaded by the ID
            channelId = response.id

            // fetch and update the subscription status
            isSubscribed = SubscriptionHelper.isSubscribed(channelId!!)
            if (isSubscribed == null) return@launchWhenCreated

            runOnUiThread {
                if (isSubscribed == true) {
                    binding.channelSubscribe.text = getString(R.string.unsubscribe)
                }

                binding.channelSubscribe.setOnClickListener {
                    binding.channelSubscribe.text = if (isSubscribed == true) {
                        SubscriptionHelper.unsubscribe(channelId!!)
                        isSubscribed = false
                        getString(R.string.subscribe)
                    } else {
                        SubscriptionHelper.subscribe(channelId!!)
                        isSubscribed = true
                        getString(R.string.unsubscribe)
                    }
                }

                binding.channelShare.setOnClickListener {
                    val shareDialog = ShareDialog(response.name!!, ShareObjectType.CHANNEL)
                    shareDialog.show(childFragmentManager, ShareDialog::class.java.name)
                }
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

                ImageHelper.loadImage(response.bannerUrl, binding.channelBanner)
                ImageHelper.loadImage(response.avatarUrl, binding.channelImage)

                // recyclerview of the videos by the channel
                channelAdapter = ChannelAdapter(
                    response.relatedStreams!!.toMutableList(),
                    childFragmentManager
                )
                binding.channelRecView.adapter = channelAdapter
            }
        }
    }

    private fun fetchChannelNextPage() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getChannelNextPage(channelId!!, nextPage!!)
                } catch (e: IOException) {
                    binding.channelRefresh.isRefreshing = false
                    println(e)
                    Log.e(TAG(), "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    binding.channelRefresh.isRefreshing = false
                    Log.e(TAG(), "HttpException, unexpected response," + e.response())
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
}
