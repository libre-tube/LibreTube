package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.libretube.R
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentChannelBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.ClipboardHelper
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.extensions.setupSubscriptionButton
import com.github.libretube.ui.models.ChannelViewModel
import com.github.libretube.ui.sheets.ChannelOptionsBottomSheet
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelFragment : Fragment(R.layout.fragment_channel) {
    private var _binding: FragmentChannelBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<ChannelFragmentArgs>()
    private val viewModel: ChannelViewModel by viewModels()

    private var channelId: String? = null
    private var channelName: String? = null
    private var isLoading = true

    private lateinit var channelContentAdapter: ChannelContentAdapter

    private var isAppBarFullyExpanded: Boolean = true

    private val tabNamesMap = mapOf(
        VIDEOS_TAB_KEY to R.string.videos,
        "shorts" to R.string.yt_shorts,
        "livestreams" to R.string.livestreams,
        "playlists" to R.string.playlists,
        "albums" to R.string.albums,
        "podcasts" to R.string.podcasts,
        "courses" to R.string.courses,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        channelName = args.channelName
            ?.replace("/c/", "")
            ?.replace("/user/", "")
        channelId = args.channelId
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentChannelBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
        // Check if the AppBarLayout is fully expanded
        binding.channelAppBar.addOnOffsetChangedListener { _, verticalOffset ->
            isAppBarFullyExpanded = verticalOffset == 0
        }

        binding.pager.reduceDragSensitivity()

        // Determine if the child can scroll up
        binding.channelRefresh.setOnChildScrollUpCallback { _, _ ->
            !isAppBarFullyExpanded
        }

        binding.channelRefresh.setOnRefreshListener {
            fetchChannel()
        }

        fetchChannel()
    }

    // adjust sensitivity due to the issue of viewpager2 with SwipeToRefresh https://issuetracker.google.com/issues/138314213
    private fun ViewPager2.reduceDragSensitivity() {
        val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
        recyclerViewField.isAccessible = true
        val recyclerView = recyclerViewField.get(this) as RecyclerView

        val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
        touchSlopField.isAccessible = true
        val touchSlop = touchSlopField.get(recyclerView) as Int
        touchSlopField.set(recyclerView, touchSlop * 3)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchChannel() = lifecycleScope.launch {
        isLoading = true
        _binding?.channelRefresh?.isRefreshing = true

        val channel = try {
            withContext(Dispatchers.IO) {
                if (channelId != null) {
                    MediaServiceRepository.instance.getChannel(channelId!!)
                } else {
                    MediaServiceRepository.instance.getChannelByName(channelName!!)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG(), e.stackTraceToString())
            context?.toastFromMainDispatcher(e.localizedMessage.orEmpty())
            return@launch
        } finally {
            _binding?.channelRefresh?.isRefreshing = false
            isLoading = false
        }
        val binding = _binding ?: return@launch

        // needed if the channel gets loaded by the ID
        channelId = channel.id
        channelName = channel.name

        val channelId = channelId ?: return@launch

        var isSubscribed = false
        binding.channelSubscribe.setupSubscriptionButton(
            channelId,
            channel.name.orEmpty(),
            channel.avatarUrl,
            channel.verified,
            binding.notificationBell
        ) {
            isSubscribed = it
        }

        binding.showMore.setOnClickListener {
            ChannelOptionsBottomSheet()
                .apply {
                    arguments = bundleOf(
                        IntentData.channelId to channelId,
                        IntentData.channelName to channelName,
                        IntentData.isSubscribed to isSubscribed
                    )
                }
                .show(childFragmentManager)
        }

        viewModel.relatedStreams = channel.relatedStreams
        viewModel.nextPage = channel.nextpage
        isLoading = false
        binding.channelRefresh.isRefreshing = false

        binding.channelCoordinator.isVisible = true

        binding.channelName.text = channelName
        binding.channelName.setOnLongClickListener {
            ClipboardHelper.save(requireContext(), text = channel.name.orEmpty())
            true
        }

        if (channel.verified) {
            binding.channelName
                .setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_verified, 0)
        }
        binding.channelSubs.text = resources.getString(
            R.string.subscribers,
            channel.subscriberCount.formatShort()
        )
        if (channel.description.orEmpty().isBlank()) {
            binding.channelDescription.isGone = true
        } else {
            binding.channelDescription.text = channel.description.orEmpty().trim()
        }

        ImageHelper.loadImage(channel.bannerUrl, binding.channelBanner)
        ImageHelper.loadImage(channel.avatarUrl, binding.channelImage, true)

        binding.channelImage.setOnClickListener {
            NavigationHelper.openImagePreview(
                requireContext(),
                channel.avatarUrl ?: return@setOnClickListener
            )
        }

        binding.channelBanner.setOnClickListener {
            NavigationHelper.openImagePreview(
                requireContext(),
                channel.bannerUrl ?: return@setOnClickListener
            )
        }

        val tabList = channel.tabs.filter { it.data.isNotEmpty() }.map {
            val tabName = tabNamesMap[it.name]?.let { getString(it) }
                ?: it.name.replaceFirstChar(Char::titlecase)
            ChannelTab(tabName, it.data)
        }

        val selectedTab = binding.pager.currentItem
        channelContentAdapter = ChannelContentAdapter(
            tabList,
            this@ChannelFragment
        )
        binding.pager.adapter = channelContentAdapter
        TabLayoutMediator(binding.tabParent, binding.pager) { tab, position ->
            tab.text = tabList[position].name
        }.attach()

        binding.pager.setCurrentItem(selectedTab, false)
    }

    companion object {
        private const val VIDEOS_TAB_KEY = "videos"
    }
}

class ChannelContentAdapter(
    private val list: List<ChannelTab>,
    fragment: Fragment
) : FragmentStateAdapter(fragment) {
    override fun getItemCount() = list.size

    override fun createFragment(position: Int) = ChannelContentFragment().apply {
        arguments = bundleOf(
            IntentData.tabData to list[position],
        )
    }
}
