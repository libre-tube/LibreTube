package com.github.libretube.fragments

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
import com.github.libretube.adapters.LegacySubscriptionAdapter
import com.github.libretube.adapters.SubscriptionChannelAdapter
import com.github.libretube.adapters.TrendingAdapter
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.FragmentSubscriptionsBinding
import com.github.libretube.extensions.BaseFragment
import com.github.libretube.models.SubscriptionsViewModel
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.views.BottomSheet

class SubscriptionsFragment : BaseFragment() {
    private lateinit var binding: FragmentSubscriptionsBinding
    private val viewModel: SubscriptionsViewModel by activityViewModels()

    private var subscriptionAdapter: TrendingAdapter? = null
    private var sortOrder = "most_recent"

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

        binding.subRefresh.isEnabled = true

        binding.subProgress.visibility = View.VISIBLE

        val grid = PreferenceHelper.getString(
            PreferenceKeys.GRID_COLUMNS,
            resources.getInteger(R.integer.grid_items).toString()
        )

        binding.subFeed.layoutManager = GridLayoutManager(view.context, grid.toInt())

        if (viewModel.videoFeed.value == null || !loadFeedInBackground) {
            viewModel.videoFeed.value = null
            viewModel.fetchFeed()
        }

        // listen for error responses
        viewModel.errorResponse.observe(viewLifecycleOwner) {
            if (it) Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
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
            showSortDialog()
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

    private fun showSortDialog() {
        val sortOptions = resources.getStringArray(R.array.sortOptions)
        val sortOptionValues = resources.getStringArray(R.array.sortOptionsValues)

        val bottomSheet = BottomSheet().apply {
            setSimpleItems(sortOptions.toList()) { index ->
                binding.sortTV.text = sortOptions[index]
                sortOrder = sortOptionValues[index]
                showFeed()
            }
        }

        bottomSheet.show(childFragmentManager, null)
    }

    private fun showFeed() {
        if (viewModel.videoFeed.value == null) return

        binding.subRefresh.isRefreshing = false
        val feed = viewModel.videoFeed.value!!
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

        binding.subChannelsContainer.visibility = View.GONE
        binding.subFeedContainer.visibility =
            if (viewModel.videoFeed.value!!.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyFeed.visibility =
            if (viewModel.videoFeed.value!!.isEmpty()) View.VISIBLE else View.GONE

        binding.subProgress.visibility = View.GONE
        subscriptionAdapter = TrendingAdapter(sortedFeed, childFragmentManager, false)
        binding.subFeed.adapter = subscriptionAdapter
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
