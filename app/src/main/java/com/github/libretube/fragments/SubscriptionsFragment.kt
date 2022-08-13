package com.github.libretube.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.adapters.SubscriptionChannelAdapter
import com.github.libretube.adapters.TrendingAdapter
import com.github.libretube.databinding.FragmentSubscriptionsBinding
import com.github.libretube.extensions.BaseFragment
import com.github.libretube.obj.StreamItem
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.SubscriptionHelper
import com.github.libretube.util.toID
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.HttpException
import java.io.IOException

class SubscriptionsFragment : BaseFragment() {
    val TAG = "SubFragment"
    private lateinit var binding: FragmentSubscriptionsBinding

    lateinit var token: String
    private var isLoaded = false
    private var subscriptionAdapter: TrendingAdapter? = null
    private var feed: List<StreamItem> = listOf()
    private var sortOrder = "most_recent"

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

        binding.subRefresh.isEnabled = true

        binding.subProgress.visibility = View.VISIBLE

        val grid = PreferenceHelper.getString(
            PreferenceKeys.GRID_COLUMNS,
            resources.getInteger(R.integer.grid_items).toString()
        )
        binding.subFeed.layoutManager = GridLayoutManager(view.context, grid.toInt())
        fetchFeed()

        binding.subRefresh.setOnRefreshListener {
            fetchChannels()
            fetchFeed()
        }

        binding.sortTV.setOnClickListener {
            showSortDialog()
        }

        binding.toggleSubs.visibility = View.VISIBLE
        var loadedSubbedChannels = false

        binding.toggleSubs.setOnClickListener {
            if (!binding.subChannelsContainer.isVisible) {
                if (!loadedSubbedChannels) {
                    binding.subChannels.layoutManager = LinearLayoutManager(context)
                    fetchChannels()
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
    }

    private fun showSortDialog() {
        val sortOptions = resources.getStringArray(R.array.sortOptions)
        val sortOptionValues = resources.getStringArray(R.array.sortOptionsValues)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sort)
            .setItems(sortOptions) { _, index ->
                binding.sortTV.text = sortOptions[index]
                sortOrder = sortOptionValues[index]
                showFeed()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun fetchFeed() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                feed = try {
                    if (token != "") RetrofitInstance.authApi.getFeed(token)
                    else RetrofitInstance.authApi.getUnauthenticatedFeed(
                        SubscriptionHelper.getFormattedLocalSubscriptions()
                    )
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
                if (feed.isNotEmpty()) {
                    // save the last recent video to the prefs for the notification worker
                    PreferenceHelper.setLatestVideoId(feed[0].url.toID())
                    // show the feed
                    showFeed()
                } else {
                    runOnUiThread {
                        binding.emptyFeed.visibility = View.VISIBLE
                    }
                }
                binding.subProgress.visibility = View.GONE
                isLoaded = true
            }
        }
        run()
    }

    private fun showFeed() {
        // sort the feed
        val sortedFeed = when (sortOrder) {
            "most_recent" -> feed
            "least_recent" -> feed.reversed()
            "most_views" -> feed.sortedBy { it.views }.reversed()
            "least_views" -> feed.sortedBy { it.views }
            "channel_name_az" -> feed.sortedBy { it.uploaderName }
            "channel_name_za" -> feed.sortedBy { it.uploaderName }.reversed()
            else -> feed
        }
        subscriptionAdapter = TrendingAdapter(sortedFeed, childFragmentManager, false)
        binding.subFeed.adapter = subscriptionAdapter
    }

    private fun fetchChannels() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    if (token != "") RetrofitInstance.authApi.subscriptions(token)
                    else RetrofitInstance.authApi.unauthenticatedSubscriptions(
                        SubscriptionHelper.getFormattedLocalSubscriptions()
                    )
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
                    binding.subChannels.adapter =
                        SubscriptionChannelAdapter(response.toMutableList())
                } else {
                    Toast.makeText(context, R.string.subscribeIsEmpty, Toast.LENGTH_SHORT).show()
                }
            }
        }
        run()
    }
}
