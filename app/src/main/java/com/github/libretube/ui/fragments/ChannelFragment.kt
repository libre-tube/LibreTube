package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentChannelBinding
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.toID
import com.github.libretube.obj.ChannelTabs
import com.github.libretube.obj.ShareData
import com.github.libretube.ui.adapters.SearchAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.base.BaseFragment
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.extensions.setupSubscriptionButton
import com.github.libretube.helpers.ImageHelper
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ChannelFragment : BaseFragment() {
    private lateinit var binding: FragmentChannelBinding

    private var channelId: String? = null
    private var channelName: String? = null
    private var nextPage: String? = null
    private var channelAdapter: VideosAdapter? = null
    private var isLoading = true
    private var isSubscribed: Boolean? = false

    private var onScrollEnd: () -> Unit = {}

    private val scope = CoroutineScope(Dispatchers.IO)

    val possibleTabs = listOf(
        ChannelTabs.Channels,
        ChannelTabs.Playlists,
        ChannelTabs.Livestreams,
        ChannelTabs.Shorts
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            channelId = it.getString(IntentData.channelId)?.toID()
            channelName = it.getString(IntentData.channelName)
                ?.replace("/c/", "")
                ?.replace("/user/", "")
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
                if (!binding.channelScrollView.canScrollVertically(1)) {
                    try {
                        onScrollEnd.invoke()
                    } catch (e: Exception) {
                        Log.e("tabs failed", e.toString())
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
                Log.e(TAG(), "IOException, you might not have internet connection")
                return@launchWhenCreated
            } catch (e: HttpException) {
                binding.channelRefresh.isRefreshing = false
                Log.e(TAG(), "HttpException, unexpected response")
                return@launchWhenCreated
            }
            // needed if the channel gets loaded by the ID
            channelId = response.id
            channelName = response.name
            val shareData = ShareData(currentChannel = response.name)

            onScrollEnd = {
                fetchChannelNextPage()
            }

            // fetch and update the subscription status
            isSubscribed = SubscriptionHelper.isSubscribed(channelId!!)
            if (isSubscribed == null) return@launchWhenCreated

            runOnUiThread {
                binding.channelSubscribe.setupSubscriptionButton(
                    channelId,
                    channelName,
                    binding.notificationBell
                )

                binding.channelShare.setOnClickListener {
                    val shareDialog = ShareDialog(
                        response.id!!.toID(),
                        ShareObjectType.CHANNEL,
                        shareData
                    )
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
                if (response.description.isBlank()) {
                    binding.channelDescription.visibility = View.GONE
                } else {
                    binding.channelDescription.text = response.description.trim()
                }

                binding.channelDescription.setOnClickListener {
                    (it as TextView).apply {
                        it.maxLines = if (it.maxLines == Int.MAX_VALUE) 2 else Int.MAX_VALUE
                    }
                }

                ImageHelper.loadImage(response.bannerUrl, binding.channelBanner)
                ImageHelper.loadImage(response.avatarUrl, binding.channelImage)

                // recyclerview of the videos by the channel
                channelAdapter = VideosAdapter(
                    response.relatedStreams.toMutableList(),
                    forceMode = VideosAdapter.Companion.ForceMode.CHANNEL
                )
                binding.channelRecView.adapter = channelAdapter
            }

            setupTabs(response.tabs)
        }
    }

    private fun setupTabs(tabs: List<ChannelTab>) {
        binding.tabChips.children.forEach { chip ->
            val resourceTab = possibleTabs.firstOrNull { it.chipId == chip.id }
            resourceTab?.let { resTab ->
                if (tabs.any { it.name == resTab.identifierName }) chip.visibility = View.VISIBLE
            }
        }

        binding.tabChips.setOnCheckedStateChangeListener { _, _ ->
            when (binding.tabChips.checkedChipId) {
                binding.videos.id -> {
                    binding.channelRecView.adapter = channelAdapter
                    onScrollEnd = {
                        fetchChannelNextPage()
                    }
                }
                else -> {
                    possibleTabs.first { binding.tabChips.checkedChipId == it.chipId }.let {
                        val tab = tabs.first { tab -> tab.name == it.identifierName }
                        loadTab(tab)
                    }
                }
            }
        }

        // Load selected chip content if it's not videos tab.
        possibleTabs.firstOrNull { binding.tabChips.checkedChipId == it.chipId }?.let {
            val tab = tabs.first { tab -> tab.name == it.identifierName }
            loadTab(tab)
        }
    }

    private fun loadTab(tab: ChannelTab) {
        scope.launch {
            val response = try {
                RetrofitInstance.api.getChannelTab(tab.data)
            } catch (e: Exception) {
                return@launch
            }

            val adapter = SearchAdapter(response.content.toMutableList())

            runOnUiThread {
                binding.channelRecView.adapter = adapter
            }

            var tabNextPage = response.nextpage
            onScrollEnd = {
                tabNextPage?.let {
                    fetchTabNextPage(it, tab, adapter) { nextPage ->
                        tabNextPage = nextPage
                    }
                }
            }
        }
    }

    private fun fetchChannelNextPage() {
        fun run() {
            if (nextPage == null || isLoading) return
            isLoading = true
            binding.channelRefresh.isRefreshing = true

            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getChannelNextPage(channelId!!, nextPage!!)
                } catch (e: IOException) {
                    binding.channelRefresh.isRefreshing = false
                    Log.e(TAG(), "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    binding.channelRefresh.isRefreshing = false
                    Log.e(TAG(), "HttpException, unexpected response," + e.response())
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                channelAdapter?.insertItems(response.relatedStreams)
                isLoading = false
                binding.channelRefresh.isRefreshing = false
            }
        }
        run()
    }

    private fun fetchTabNextPage(
        nextPage: String,
        tab: ChannelTab,
        adapter: SearchAdapter,
        onNewNextPage: (String?) -> Unit
    ) {
        scope.launch {
            val newContent = try {
                RetrofitInstance.api.getChannelTab(tab.data, nextPage)
            } catch (e: Exception) {
                Log.e(TAG(), "Exception: $e")
                null
            }
            onNewNextPage.invoke(newContent?.nextpage)
            runOnUiThread {
                newContent?.content?.let {
                    adapter.updateItems(it)
                }
            }
        }
    }
}
