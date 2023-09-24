package com.github.libretube.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.obj.ChannelTabs
import com.github.libretube.obj.ShareData
import com.github.libretube.ui.activities.ZoomableImageActivity
import com.github.libretube.ui.adapters.SearchAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.extensions.setupSubscriptionButton
import com.github.libretube.util.deArrow
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class ChannelFragment : Fragment() {
    private var _binding: FragmentChannelBinding? = null
    private val binding get() = _binding!!

    private var channelId: String? = null
    private var channelName: String? = null
    private var nextPage: String? = null
    private var channelAdapter: VideosAdapter? = null
    private var isLoading = true
    private var isSubscribed: Boolean? = false

    private var onScrollEnd: () -> Unit = {}

    private val possibleTabs = listOf(
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
        _binding = FragmentChannelBinding.inflate(inflater, container, false)
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

        binding.channelScrollView.viewTreeObserver.addOnScrollChangedListener {
            if (_binding?.channelScrollView?.canScrollVertically(1) == false) {
                try {
                    onScrollEnd()
                } catch (e: Exception) {
                    Log.e("tabs failed", e.toString())
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchChannel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val response = try {
                    withContext(Dispatchers.IO) {
                        if (channelId != null) {
                            RetrofitInstance.api.getChannel(channelId!!)
                        } else {
                            RetrofitInstance.api.getChannelByName(channelName!!)
                        }.apply {
                            relatedStreams = relatedStreams.deArrow()
                        }
                    }
                } catch (e: IOException) {
                    _binding?.channelRefresh?.isRefreshing = false
                    Log.e(TAG(), "IOException, you might not have internet connection")
                    return@repeatOnLifecycle
                } catch (e: HttpException) {
                    _binding?.channelRefresh?.isRefreshing = false
                    Log.e(TAG(), "HttpException, unexpected response")
                    return@repeatOnLifecycle
                }
                val binding = _binding ?: return@repeatOnLifecycle

                // needed if the channel gets loaded by the ID
                channelId = response.id
                channelName = response.name
                val shareData = ShareData(currentChannel = response.name)

                onScrollEnd = {
                    fetchChannelNextPage()
                }

                val channelId = channelId ?: return@repeatOnLifecycle
                // fetch and update the subscription status
                isSubscribed = SubscriptionHelper.isSubscribed(channelId)
                if (isSubscribed == null) return@repeatOnLifecycle

                binding.channelSubscribe.setupSubscriptionButton(
                    channelId,
                    channelName,
                    binding.notificationBell
                )

                binding.channelShare.setOnClickListener {
                    val bundle = bundleOf(
                        IntentData.id to channelId.toID(),
                        IntentData.shareObjectType to ShareObjectType.CHANNEL,
                        IntentData.shareData to shareData
                    )
                    val newShareDialog = ShareDialog()
                    newShareDialog.arguments = bundle
                    newShareDialog.show(childFragmentManager, ShareDialog::class.java.name)
                }

                nextPage = response.nextpage
                isLoading = false
                binding.channelRefresh.isRefreshing = false

                binding.channelScrollView.isVisible = true
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
                if (response.description.orEmpty().isBlank()) {
                    binding.channelDescription.isGone = true
                } else {
                    binding.channelDescription.text = response.description.orEmpty().trim()
                }

                binding.channelDescription.setOnClickListener {
                    (it as TextView).apply {
                        it.maxLines = if (it.maxLines == Int.MAX_VALUE) 2 else Int.MAX_VALUE
                    }
                }

                ImageHelper.loadImage(response.bannerUrl, binding.channelBanner)
                ImageHelper.loadImage(response.avatarUrl, binding.channelImage)

                binding.channelImage.setOnClickListener {
                    lifecycleScope.launch(Dispatchers.IO) onclick@ {
                        val image = ImageHelper.getImage(requireContext(), response.avatarUrl) ?: return@onclick
                        val intent = Intent(context, ZoomableImageActivity::class.java)
                        intent.putExtra(IntentData.bitmap, image)
                        context?.startActivity(intent)
                    }
                }

                // recyclerview of the videos by the channel
                channelAdapter = VideosAdapter(
                    response.relatedStreams.toMutableList(),
                    forceMode = VideosAdapter.Companion.ForceMode.CHANNEL
                )
                binding.channelRecView.adapter = channelAdapter

                setupTabs(response.tabs)
            }
        }
    }

    private fun setupTabs(tabs: List<ChannelTab>) {
        binding.tabChips.children.forEach { chip ->
            val resourceTab = possibleTabs.firstOrNull { it.chipId == chip.id }
            resourceTab?.let { resTab ->
                if (tabs.any { it.name == resTab.identifierName }) chip.isVisible = true
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
        lifecycleScope.launch {
            val response = try {
                withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getChannelTab(tab.data)
                }.apply {
                    content = content.deArrow()
                }
            } catch (e: Exception) {
                return@launch
            }
            val binding = _binding ?: return@launch

            val adapter = SearchAdapter(true)
            binding.channelRecView.adapter = adapter
            adapter.submitList(response.content)

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
        if (nextPage == null || isLoading) return
        isLoading = true
        binding.channelRefresh.isRefreshing = true

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val response = try {
                    withContext(Dispatchers.IO) {
                        RetrofitInstance.api.getChannelNextPage(channelId!!, nextPage!!).apply {
                            relatedStreams = relatedStreams.deArrow()
                        }
                    }
                } catch (e: IOException) {
                    _binding?.channelRefresh?.isRefreshing = false
                    Log.e(TAG(), "IOException, you might not have internet connection")
                    return@repeatOnLifecycle
                } catch (e: HttpException) {
                    _binding?.channelRefresh?.isRefreshing = false
                    Log.e(TAG(), "HttpException, unexpected response," + e.response())
                    return@repeatOnLifecycle
                }
                val binding = _binding ?: return@repeatOnLifecycle

                nextPage = response.nextpage
                channelAdapter?.insertItems(response.relatedStreams)
                isLoading = false
                binding.channelRefresh.isRefreshing = false
            }
        }
    }

    private fun fetchTabNextPage(
        nextPage: String,
        tab: ChannelTab,
        adapter: SearchAdapter,
        onNewNextPage: (String?) -> Unit
    ) {
        lifecycleScope.launch {
            val newContent = try {
                withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getChannelTab(tab.data, nextPage)
                }.apply {
                    content = content.deArrow()
                }
            } catch (e: Exception) {
                Log.e(TAG(), "Exception: $e")
                null
            }
            onNewNextPage(newContent?.nextpage)
            newContent?.content?.let {
                adapter.submitList(adapter.currentList + it)
            }
        }
    }
}
