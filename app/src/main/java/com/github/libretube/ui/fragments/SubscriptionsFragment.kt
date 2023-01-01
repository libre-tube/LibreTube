package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentSubscriptionsBinding
import com.github.libretube.ui.adapters.LegacySubscriptionAdapter
import com.github.libretube.ui.adapters.SubscriptionChannelAdapter
import com.github.libretube.ui.adapters.VideosAdapter
import com.github.libretube.ui.base.BaseFragment
import com.github.libretube.ui.models.SubscriptionsViewModel
import com.github.libretube.ui.sheets.BaseBottomSheet
import com.github.libretube.util.PreferenceHelper

class SubscriptionsFragment : BaseFragment() {
    private lateinit var binding: FragmentSubscriptionsBinding
    private val viewModel: SubscriptionsViewModel by activityViewModels()

    private var subscriptionAdapter: VideosAdapter? = null
    private var selectedSortOrder = PreferenceHelper.getInt(PreferenceKeys.FEED_SORT_ORDER, 0)
        set(value) {
            PreferenceHelper.putInt(PreferenceKeys.FEED_SORT_ORDER, value)
            field = value
        }
    private var selectedFilter = PreferenceHelper.getInt(PreferenceKeys.SELECTED_FEED_FILTER, 0)
        set(value) {
            PreferenceHelper.putInt(PreferenceKeys.SELECTED_FEED_FILTER, value)
            field = value
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

        val loadFeedInBackground = PreferenceHelper.getBoolean(
            PreferenceKeys.SAVE_FEED,
            false
        )

        // update the text according to the current order and filter
        binding.sortTV.text = resources.getStringArray(R.array.sortOptions)[selectedSortOrder]
        binding.filterTV.text = resources.getStringArray(R.array.filterOptions)[selectedFilter]

        binding.subRefresh.isEnabled = true

        binding.subProgress.visibility = View.VISIBLE

        binding.subFeed.layoutManager = VideosAdapter.getLayout(requireContext())

        if (viewModel.videoFeed.value == null || !loadFeedInBackground) {
            viewModel.videoFeed.value = null
            viewModel.fetchFeed()
        }

        // listen for error responses
        viewModel.errorResponse.observe(viewLifecycleOwner) {
            if (!it) return@observe
            Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
            viewModel.errorResponse.value = false
        }

        viewModel.videoFeed.observe(viewLifecycleOwner) {
            if (!isShowingFeed()) return@observe
            if (it == null) return@observe
            showFeed()
        }

        viewModel.subscriptions.observe(viewLifecycleOwner) {
            if (isShowingFeed()) return@observe
            if (it == null) return@observe
            showSubscriptions()
        }

        binding.subRefresh.setOnRefreshListener {
            viewModel.fetchSubscriptions()
            viewModel.fetchFeed()
        }

        binding.sortTV.setOnClickListener {
            val sortOptions = resources.getStringArray(R.array.sortOptions)

            BaseBottomSheet().apply {
                setSimpleItems(sortOptions.toList()) { index ->
                    binding.sortTV.text = sortOptions[index]
                    selectedSortOrder = index
                    showFeed()
                }
            }.show(childFragmentManager)
        }

        binding.filterTV.setOnClickListener {
            val filterOptions = resources.getStringArray(R.array.filterOptions)

            BaseBottomSheet().apply {
                setSimpleItems(filterOptions.toList()) { index ->
                    binding.filterTV.text = filterOptions[index]
                    selectedFilter = index
                    showFeed()
                }
            }.show(childFragmentManager)
        }

        binding.toggleSubs.visibility = View.VISIBLE

        binding.toggleSubs.setOnClickListener {
            if (isShowingFeed()) {
                if (viewModel.subscriptions.value == null) {
                    viewModel.fetchSubscriptions()
                } else {
                    showSubscriptions()
                }
                binding.subChannelsContainer.visibility = View.VISIBLE
                binding.subFeedContainer.visibility = View.GONE
            } else {
                showFeed()
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
                    if (viewModel.videoFeed.value == null) return@addOnScrollChangedListener
                    binding.subRefresh.isRefreshing = true
                    subscriptionAdapter?.updateItems()
                    binding.subRefresh.isRefreshing = false
                }
            }
    }

    private fun showFeed() {
        if (viewModel.videoFeed.value == null) return

        binding.subRefresh.isRefreshing = false
        val feed = viewModel.videoFeed.value!!.filter {
            // apply the selected filter
            when (selectedFilter) {
                0 -> true
                1 -> !it.isShort
                2 -> it.isShort
                else -> throw IllegalArgumentException()
            }
        }
        // sort the feed
        val sortedFeed = when (selectedSortOrder) {
            0 -> feed
            1 -> feed.reversed()
            2 -> feed.sortedBy { it.views }.reversed()
            3 -> feed.sortedBy { it.views }
            4 -> feed.sortedBy { it.uploaderName }
            5 -> feed.sortedBy { it.uploaderName }.reversed()
            else -> feed
        }.toMutableList()

        // add an "all caught up item"
        if (selectedSortOrder == 0) {
            val lastCheckedFeedTime = PreferenceHelper.getLastCheckedFeedTime()
            val caughtUpIndex = feed.indexOfFirst {
                (it.uploaded ?: 0L) / 1000 < lastCheckedFeedTime
            }
            if (caughtUpIndex > 0) {
                sortedFeed.add(caughtUpIndex, StreamItem(type = "caught"))
            }
        }

        binding.subChannelsContainer.visibility = View.GONE
        binding.subFeedContainer.visibility =
            if (viewModel.videoFeed.value!!.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyFeed.visibility =
            if (viewModel.videoFeed.value!!.isEmpty()) View.VISIBLE else View.GONE

        binding.subProgress.visibility = View.GONE
        subscriptionAdapter = VideosAdapter(
            sortedFeed.toMutableList(),
            showAllAtOnce = false,
            hideWatched = PreferenceHelper.getBoolean(PreferenceKeys.HIDE_WATCHED_FROM_FEED, false)
        )
        binding.subFeed.adapter = subscriptionAdapter

        PreferenceHelper.updateLastFeedWatchedTime()
    }

    private fun showSubscriptions() {
        if (viewModel.subscriptions.value == null) return

        binding.subRefresh.isRefreshing = false

        val legacySubscriptions = PreferenceHelper.getBoolean(
            PreferenceKeys.LEGACY_SUBSCRIPTIONS,
            false
        )

        binding.subChannels.layoutManager = if (legacySubscriptions) {
            GridLayoutManager(
                context,
                PreferenceHelper.getString(
                    PreferenceKeys.LEGACY_SUBSCRIPTIONS_COLUMNS,
                    "4"
                ).toInt()
            )
        } else {
            LinearLayoutManager(context)
        }

        // set the adapter of the subscribed channels
        binding.subChannels.adapter = if (legacySubscriptions) {
            LegacySubscriptionAdapter(viewModel.subscriptions.value!!)
        } else {
            SubscriptionChannelAdapter(
                viewModel.subscriptions.value!!.toMutableList()
            )
        }

        binding.subFeedContainer.visibility = View.GONE
        binding.subChannelsContainer.visibility =
            if (viewModel.subscriptions.value!!.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyFeed.visibility =
            if (viewModel.subscriptions.value!!.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun isShowingFeed(): Boolean {
        return !binding.subChannelsContainer.isVisible
    }
}
