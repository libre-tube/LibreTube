package com.github.libretube.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.adapters.SubscriptionAdapter
import com.github.libretube.adapters.SubscriptionChannelAdapter
import com.github.libretube.databinding.FragmentSubscriptionsBinding
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import com.github.libretube.util.RetrofitInstance
import retrofit2.HttpException
import java.io.IOException

class SubscriptionsFragment : Fragment() {
    val TAG = "SubFragment"
    private lateinit var binding: FragmentSubscriptionsBinding

    lateinit var token: String
    private var isLoaded = false
    private var subscriptionAdapter: SubscriptionAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSubscriptionsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        token = PreferenceHelper.getToken()

        if (token != "") {
            binding.loginOrRegister.visibility = View.GONE
            binding.subRefresh.isEnabled = true

            binding.subProgress.visibility = View.VISIBLE

            val grid = PreferenceHelper.getString(
                PreferenceKeys.GRID_COLUMNS,
                resources.getInteger(R.integer.grid_items).toString()
            )!!
            binding.subFeed.layoutManager = GridLayoutManager(view.context, grid.toInt())
            fetchFeed(binding.subFeed, binding.subProgress)

            binding.subRefresh.setOnRefreshListener {
                fetchChannels(binding.subChannels)
                fetchFeed(binding.subFeed, binding.subProgress)
            }

            binding.toggleSubs.visibility = View.VISIBLE
            var loadedSubbedChannels = false

            binding.toggleSubs.setOnClickListener {
                if (!binding.subChannelsContainer.isVisible) {
                    if (!loadedSubbedChannels) {
                        binding.subChannels.layoutManager = LinearLayoutManager(context)
                        fetchChannels(binding.subChannels)
                        loadedSubbedChannels = true
                    }
                    binding.subChannelsContainer.visibility = View.VISIBLE
                    binding.subFeedContainer.visibility = View.GONE
                } else {
                    binding.subChannelsContainer.visibility = View.GONE
                    binding.subFeedContainer.visibility = View.VISIBLE
                }
            }

            binding.scrollviewSub.viewTreeObserver
                .addOnScrollChangedListener {
                    if (binding.scrollviewSub.getChildAt(0).bottom
                        == (binding.scrollviewSub.height + binding.scrollviewSub.scrollY)
                    ) {
                        // scroll view is at bottom
                        if (isLoaded) {
                            binding.subRefresh.isRefreshing = true
                            subscriptionAdapter?.updateItems()
                            binding.subRefresh.isRefreshing = false
                        }
                    }
                }
        } else {
            binding.subRefresh.isEnabled = false
        }
    }

    private fun fetchFeed(feedRecView: RecyclerView, progressBar: ProgressBar) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.authApi.getFeed(token)
                } catch (e: IOException) {
                    Log.e(TAG, e.toString())
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                } finally {
                    binding.subRefresh.isRefreshing = false
                }
                if (response.isNotEmpty()) {
                    subscriptionAdapter = SubscriptionAdapter(response, childFragmentManager)
                    feedRecView.adapter = subscriptionAdapter
                    subscriptionAdapter?.updateItems()
                } else {
                    runOnUiThread {
                        with(binding.boogh) {
                            visibility = View.VISIBLE
                            setImageResource(R.drawable.ic_list)
                        }
                        with(binding.textLike) {
                            visibility = View.VISIBLE
                            text = getString(R.string.emptyList)
                        }
                        binding.loginOrRegister.visibility = View.VISIBLE
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
                    RetrofitInstance.authApi.subscriptions(token)
                } catch (e: IOException) {
                    Log.e(TAG, e.toString())
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                } finally {
                    binding.subRefresh.isRefreshing = false
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

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }
}
