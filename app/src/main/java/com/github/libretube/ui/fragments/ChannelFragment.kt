package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentChannelBinding
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.ceilHalf
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.obj.ChannelTabs
import com.github.libretube.obj.ShareData
import com.github.libretube.ui.adapters.SearchAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.extensions.setupSubscriptionButton
import com.github.libretube.ui.sheets.AddChannelToGroupSheet
import com.github.libretube.util.deArrow
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class ChannelFragment : DynamicLayoutManagerFragment() {
    private var _binding: FragmentChannelBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<ChannelFragmentArgs>()

    private var channelId: String? = null
    private var channelName: String? = null
    private var channelAdapter: VideosAdapter? = null
    private var isLoading = true

    private val possibleTabs = arrayOf(
        ChannelTabs.Shorts,
        ChannelTabs.Livestreams,
        ChannelTabs.Playlists,
        ChannelTabs.Channels
    )
    private var channelTabs: List<ChannelTab> = emptyList()
    private var nextPages = Array<String?>(5) { null }
    private var searchAdapter: SearchAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        channelId = args.channelId?.toID()
        channelName = args.channelName?.replace("/c/", "")?.replace("/user/", "")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChannelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setLayoutManagers(gridItems: Int) {
        _binding?.channelRecView?.layoutManager = GridLayoutManager(
            context,
            gridItems.ceilHalf()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.channelRefresh.setOnRefreshListener {
            fetchChannel()
        }

        binding.channelScrollView.viewTreeObserver.addOnScrollChangedListener {
            val binding = _binding ?: return@addOnScrollChangedListener

            if (binding.channelScrollView.canScrollVertically(1) || isLoading) return@addOnScrollChangedListener

            loadNextPage()
        }

        fetchChannel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadNextPage() = lifecycleScope.launch {
        val binding = _binding ?: return@launch

        binding.channelRefresh.isRefreshing = true
        isLoading = true

        try {
            if (binding.tabChips.checkedChipId == binding.videos.id) {
                fetchChannelNextPage(nextPages[0] ?: return@launch).let {
                    nextPages[0] = it
                }
            } else {
                val currentTabIndex = binding.tabChips.children.indexOfFirst {
                    it.id == binding.tabChips.checkedChipId
                }
                val channelTab = channelTabs.first { tab ->
                    tab.name == possibleTabs[currentTabIndex - 1].identifierName
                }
                val nextPage = nextPages[currentTabIndex] ?: return@launch
                fetchTabNextPage(nextPage, channelTab).let {
                    nextPages[currentTabIndex] = it
                }
            }
        } catch (e: Exception) {
            Log.e("error fetching tabs", e.toString())
        }
    }.invokeOnCompletion {
        _binding?.channelRefresh?.isRefreshing = false
        isLoading = false
    }

    private fun fetchChannel() = lifecycleScope.launch {
        isLoading = true
        binding.channelRefresh.isRefreshing = true

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
            Log.e(TAG(), "IOException, you might not have internet connection")
            return@launch
        } catch (e: HttpException) {
            Log.e(TAG(), "HttpException, unexpected response")
            return@launch
        } finally {
            _binding?.channelRefresh?.isRefreshing = false
            isLoading = false
        }
        val binding = _binding ?: return@launch

        // needed if the channel gets loaded by the ID
        channelId = response.id
        channelName = response.name
        val shareData = ShareData(currentChannel = response.name)

        val channelId = channelId ?: return@launch

        binding.channelSubscribe.setupSubscriptionButton(
            channelId,
            channelName,
            binding.notificationBell
        )

        binding.channelSubscribe.setOnLongClickListener {
            AddChannelToGroupSheet().apply {
                arguments = bundleOf(IntentData.channelId to channelId)
            }.show(childFragmentManager)

            true
        }

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

        nextPages[0] = response.nextpage
        isLoading = false
        binding.channelRefresh.isRefreshing = false

        binding.channelScrollView.isVisible = true
        binding.channelName.text = response.name
        if (response.verified) {
            binding.channelName
                .setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_verified, 0)
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

        ImageHelper.loadImage(response.bannerUrl, binding.channelBanner)
        ImageHelper.loadImage(response.avatarUrl, binding.channelImage)

        binding.channelImage.setOnClickListener {
            NavigationHelper.openImagePreview(
                requireContext(),
                response.avatarUrl ?: return@setOnClickListener
            )
        }

        binding.channelBanner.setOnClickListener {
            NavigationHelper.openImagePreview(
                requireContext(),
                response.bannerUrl ?: return@setOnClickListener
            )
        }

        // recyclerview of the videos by the channel
        channelAdapter = VideosAdapter(
            response.relatedStreams.toMutableList(),
            forceMode = VideosAdapter.Companion.LayoutMode.CHANNEL_ROW
        )
        binding.channelRecView.adapter = channelAdapter

        setupTabs(response.tabs)
    }

    private fun setupTabs(tabs: List<ChannelTab>) {
        this.channelTabs = tabs

        val binding = _binding ?: return

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
                }

                else -> {
                    possibleTabs.first { binding.tabChips.checkedChipId == it.chipId }.let {
                        val tab = tabs.first { tab -> tab.name == it.identifierName }
                        loadChannelTab(tab)
                    }
                }
            }
        }

        // Load selected chip content if it's not videos tab.
        possibleTabs.firstOrNull { binding.tabChips.checkedChipId == it.chipId }?.let {
            val tab = tabs.first { tab -> tab.name == it.identifierName }
            loadChannelTab(tab)
        }
    }

    private fun loadChannelTab(tab: ChannelTab) = lifecycleScope.launch {
        binding.channelRefresh.isRefreshing = true
        isLoading = true

        val response = try {
            withContext(Dispatchers.IO) {
                RetrofitInstance.api.getChannelTab(tab.data)
            }.apply {
                content = content.deArrow()
            }
        } catch (e: Exception) {
            return@launch
        }
        nextPages[channelTabs.indexOf(tab) + 1] = response.nextpage

        val binding = _binding ?: return@launch

        searchAdapter = SearchAdapter(true)
        binding.channelRecView.adapter = searchAdapter
        searchAdapter?.submitList(response.content)

        binding.channelRefresh.isRefreshing = false
        isLoading = false
    }

    private suspend fun fetchChannelNextPage(nextPage: String): String? {
        val response = withContext(Dispatchers.IO) {
            RetrofitInstance.api.getChannelNextPage(channelId!!, nextPage).apply {
                relatedStreams = relatedStreams.deArrow()
            }
        }

        channelAdapter?.insertItems(response.relatedStreams)

        return response.nextpage
    }

    private suspend fun fetchTabNextPage(nextPage: String, tab: ChannelTab): String? {
        val newContent = withContext(Dispatchers.IO) {
            RetrofitInstance.api.getChannelTab(tab.data, nextPage)
        }.apply {
            content = content.deArrow()
        }

        searchAdapter?.let {
            it.submitList(it.currentList + newContent.content)
        }

        return newContent.nextpage
    }
}
