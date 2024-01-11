package com.github.libretube.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentSubscriptionsBinding
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.enums.ContentFilter
import com.github.libretube.extensions.dpToPx
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.SelectableOption
import com.github.libretube.ui.adapters.LegacySubscriptionAdapter
import com.github.libretube.ui.adapters.SubscriptionChannelAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.base.DynamicLayoutManagerFragment
import com.github.libretube.ui.models.EditChannelGroupsModel
import com.github.libretube.ui.models.PlayerViewModel
import com.github.libretube.ui.models.SubscriptionsViewModel
import com.github.libretube.ui.sheets.ChannelGroupsSheet
import com.github.libretube.ui.sheets.FilterSortBottomSheet
import com.github.libretube.util.PlayingQueue
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SubscriptionsFragment : DynamicLayoutManagerFragment() {
    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubscriptionsViewModel by activityViewModels()
    private val playerModel: PlayerViewModel by activityViewModels()
    private val channelGroupsModel: EditChannelGroupsModel by activityViewModels()
    private var selectedFilterGroup = 0
    private var isCurrentTabSubChannels = false

    var feedAdapter: VideosAdapter? = null
    private var channelsAdapter: SubscriptionChannelAdapter? = null
    private var selectedSortOrder = PreferenceHelper.getInt(PreferenceKeys.FEED_SORT_ORDER, 0)
        set(value) {
            PreferenceHelper.putInt(PreferenceKeys.FEED_SORT_ORDER, value)
            field = value
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setLayoutManagers(gridItems: Int) {
        _binding?.subFeed?.layoutManager = VideosAdapter.getLayout(requireContext(), gridItems)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loadFeedInBackground = PreferenceHelper.getBoolean(
            PreferenceKeys.SAVE_FEED,
            false
        )

        setupSortAndFilter()

        binding.subRefresh.isEnabled = true
        binding.subProgress.isVisible = true

        if (!isCurrentTabSubChannels && (viewModel.videoFeed.value == null || !loadFeedInBackground)) {
            viewModel.videoFeed.value = null
            viewModel.fetchFeed(requireContext())
        }

        viewModel.videoFeed.observe(viewLifecycleOwner) {
            if (!isCurrentTabSubChannels && it != null) showFeed()
        }

        viewModel.subscriptions.observe(viewLifecycleOwner) {
            if (isCurrentTabSubChannels && it != null) showSubscriptions()
        }

        binding.subRefresh.setOnRefreshListener {
            viewModel.fetchSubscriptions(requireContext())
            viewModel.fetchFeed(requireContext())
        }

        binding.toggleSubs.isVisible = true

        binding.toggleSubs.setOnClickListener {
            binding.subProgress.isVisible = true
            binding.subRefresh.isRefreshing = true
            isCurrentTabSubChannels = !isCurrentTabSubChannels

            if (isCurrentTabSubChannels) {
                if (viewModel.subscriptions.value == null) {
                    viewModel.fetchSubscriptions(requireContext())
                } else {
                    showSubscriptions()
                }
            } else {
                showFeed()
            }
            binding.subChannelsContainer.isVisible = isCurrentTabSubChannels
            binding.subFeedContainer.isGone = isCurrentTabSubChannels
        }

        binding.scrollviewSub.viewTreeObserver.addOnScrollChangedListener {
            val binding = _binding
            if (binding?.scrollviewSub?.canScrollVertically(1) == false &&
                viewModel.videoFeed.value != null // scroll view is at bottom
            ) {
                binding.subRefresh.isRefreshing = true
                if (isCurrentTabSubChannels) {
                    channelsAdapter?.updateItems()
                } else {
                    feedAdapter?.updateItems()
                }
                binding.subRefresh.isRefreshing = false
            }
        }

        // add some extra margin to the subscribed channels while the mini player is visible
        // otherwise the last channel would be invisible
        playerModel.isMiniPlayerVisible.observe(viewLifecycleOwner) {
            binding.subChannelsContainer.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = (if (it) 64f else 0f).dpToPx()
            }
        }

        binding.channelGroups.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedFilterGroup = group.children.indexOfFirst { it.id == checkedIds.first() }
            showFeed()
        }

        channelGroupsModel.groups.observe(viewLifecycleOwner) {
            lifecycleScope.launch { initChannelGroups() }
        }

        binding.editGroups.setOnClickListener {
            ChannelGroupsSheet().show(childFragmentManager, null)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val groups = DatabaseHolder.Database.subscriptionGroupsDao().getAll()
                .sortedBy { it.index }
            channelGroupsModel.groups.postValue(groups)
        }
    }

    private fun setupSortAndFilter() {
        binding.filterSort.setOnClickListener  {
            val sortOptions = resources.getStringArray(R.array.sortOptions)

            FilterSortBottomSheet.createWith(
                sortOptions = sortOptions.mapIndexed { index, option ->
                    SelectableOption(isSelected = index == selectedSortOrder, name = option)
                },
                sortListener = { index, _ ->
                    selectedSortOrder = index
                    showFeed()
                },
                filtersListener = ::showFeed
            ).show(childFragmentManager)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun playByGroup(groupIndex: Int) {
        val streams = viewModel.videoFeed.value.orEmpty()
            .filterByGroup(groupIndex)
            .filterByStatusAndWatchPosition()
            .sortedBySelectedOrder()

        if (streams.isEmpty()) return

        PlayingQueue.clear()
        PlayingQueue.add(*streams.toTypedArray())

        NavigationHelper.navigateVideo(requireContext(), videoUrlOrId = streams.first().url, keepQueue = true)
    }

    @SuppressLint("InflateParams")
    private fun initChannelGroups() {
        val binding = _binding ?: return

        binding.chipAll.isChecked = true
        binding.chipAll.setOnLongClickListener {
            playByGroup(0)
            true
        }

        binding.channelGroups.removeAllViews()
        binding.channelGroups.addView(binding.chipAll)

        channelGroupsModel.groups.value?.forEachIndexed { index, group ->
            val chip = layoutInflater.inflate(R.layout.filter_chip, null) as Chip
            chip.apply {
                id = View.generateViewId()
                isCheckable = true
                text = group.name
                setOnLongClickListener {
                    // the index must be increased by one to skip the "all channels" group button
                    playByGroup(index + 1)
                    true
                }
            }

            binding.channelGroups.addView(chip)

            if (index + 1 == selectedFilterGroup) binding.channelGroups.check(chip.id)
        }
    }

    private fun List<StreamItem>.filterByGroup(groupIndex: Int): List<StreamItem> {
        if (groupIndex == 0) return this

        val group = channelGroupsModel.groups.value?.getOrNull(groupIndex - 1)
        return filter {
            val channelId = it.uploaderUrl.orEmpty().toID()
            group?.channels?.contains(channelId) != false
        }
    }

    private fun List<StreamItem>.filterByStatusAndWatchPosition(): List<StreamItem> {

        val streamItems = this.filter {
            val isLive = (it.duration ?: -1L) < 0L
            val isVideo = !it.isShort && !isLive

            return@filter when {
                !ContentFilter.SHORTS.isEnabled() && it.isShort  -> false
                !ContentFilter.VIDEOS.isEnabled() && isVideo     -> false
                !ContentFilter.LIVESTREAMS.isEnabled() && isLive -> false
                else                                             -> true
            }

        }

        if (!PreferenceHelper.getBoolean(
                PreferenceKeys.HIDE_WATCHED_FROM_FEED,
                false
            )
        ) {
            return streamItems
        }

        return runBlocking { DatabaseHelper.filterUnwatched(streamItems) }
    }

    private fun List<StreamItem>.sortedBySelectedOrder() = when (selectedSortOrder) {
        0 -> this
        1 -> this.reversed()
        2 -> this.sortedBy { it.views }.reversed()
        3 -> this.sortedBy { it.views }
        4 -> this.sortedBy { it.uploaderName }
        5 -> this.sortedBy { it.uploaderName }.reversed()
        else -> this
    }

    private fun showFeed() {
        val videoFeed = viewModel.videoFeed.value ?: return

        binding.subRefresh.isRefreshing = false
        val feed = videoFeed
            .filterByGroup(selectedFilterGroup)
            .filterByStatusAndWatchPosition()

        val sortedFeed = feed
            .sortedBySelectedOrder()
            .toMutableList()

        // add an "all caught up item"
        if (selectedSortOrder == 0) {
            val lastCheckedFeedTime = PreferenceHelper.getLastCheckedFeedTime()
            val caughtUpIndex = feed.indexOfFirst { it.uploaded / 1000 < lastCheckedFeedTime }
            if (caughtUpIndex > 0) {
                sortedFeed.add(
                    caughtUpIndex,
                    StreamItem(type = VideosAdapter.CAUGHT_UP_STREAM_TYPE)
                )
            }
        }

        binding.subChannelsContainer.isGone = true
        binding.subProgress.isGone = true

        val notLoaded = viewModel.videoFeed.value.isNullOrEmpty()
        binding.subFeedContainer.isGone = notLoaded
        binding.emptyFeed.isVisible = notLoaded

        feedAdapter = VideosAdapter(
            sortedFeed.toMutableList(),
            showAllAtOnce = false
        )
        binding.subFeed.adapter = feedAdapter
        binding.toggleSubs.text = getString(R.string.subscriptions)

        PreferenceHelper.updateLastFeedWatchedTime()
    }

    @SuppressLint("SetTextI18n")
    private fun showSubscriptions() {
        val subscriptions = viewModel.subscriptions.value ?: return

        val legacySubscriptions = PreferenceHelper.getBoolean(
            PreferenceKeys.LEGACY_SUBSCRIPTIONS,
            false
        )

        if (legacySubscriptions) {
            binding.subChannels.layoutManager = GridLayoutManager(
                context,
                PreferenceHelper.getString(
                    PreferenceKeys.LEGACY_SUBSCRIPTIONS_COLUMNS,
                    "4"
                ).toInt()
            )
            binding.subChannels.adapter = LegacySubscriptionAdapter(subscriptions)
        } else {
            binding.subChannels.layoutManager = LinearLayoutManager(context)
            channelsAdapter = SubscriptionChannelAdapter(subscriptions.toMutableList())
            binding.subChannels.adapter = channelsAdapter
        }

        binding.subRefresh.isRefreshing = false
        binding.subProgress.isGone = true
        binding.subFeedContainer.isGone = true

        val notLoaded = viewModel.subscriptions.value.isNullOrEmpty()
        binding.subChannelsContainer.isGone = notLoaded
        binding.emptyFeed.isVisible = notLoaded

        val subCount = subscriptions.size.toLong().formatShort()
        binding.toggleSubs.text = "${getString(R.string.subscriptions)} ($subCount)"
    }
}
