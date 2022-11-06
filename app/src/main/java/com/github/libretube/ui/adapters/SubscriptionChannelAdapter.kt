package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.Subscription
import com.github.libretube.databinding.ChannelSubscriptionRowBinding
import com.github.libretube.extensions.setupNotificationBell
import com.github.libretube.extensions.toID
import com.github.libretube.ui.viewholders.SubscriptionChannelViewHolder
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper

class SubscriptionChannelAdapter(
    private val subscriptions: MutableList<Subscription>
) : RecyclerView.Adapter<SubscriptionChannelViewHolder>() {

    override fun getItemCount(): Int {
        return subscriptions.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
        SubscriptionChannelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ChannelSubscriptionRowBinding.inflate(layoutInflater, parent, false)
        return SubscriptionChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubscriptionChannelViewHolder, position: Int) {
        val subscription = subscriptions[position]
        var isSubscribed = true

        holder.binding.apply {
            subscriptionChannelName.text = subscription.name
            ImageHelper.loadImage(subscription.avatar, subscriptionChannelImage)

            subscription.url?.toID()?.let { notificationBell.setupNotificationBell(it) }

            root.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, subscription.url)
            }
            subscriptionSubscribe.setOnClickListener {
                val channelId = subscription.url!!.toID()
                if (isSubscribed) {
                    SubscriptionHelper.handleUnsubscribe(root.context, channelId, subscription.name ?: "") {
                        subscriptionSubscribe.text = root.context.getString(R.string.subscribe)
                        notificationBell.visibility = View.GONE
                        isSubscribed = false
                    }
                } else {
                    SubscriptionHelper.subscribe(channelId)
                    subscriptionSubscribe.text = root.context.getString(R.string.unsubscribe)
                    notificationBell.visibility = View.VISIBLE
                    isSubscribed = true
                }
            }
        }
    }
}
