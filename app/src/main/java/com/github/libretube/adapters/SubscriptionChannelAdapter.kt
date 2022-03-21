package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.activity.MainActivity
import com.github.libretube.R
import com.github.libretube.databinding.ChannelSubscriptionRowBinding
import com.github.libretube.databinding.VideoChannelRowBinding
import com.github.libretube.fragment.KEY_CHANNEL_ID
import com.github.libretube.model.Subscription
import com.squareup.picasso.Picasso

class SubscriptionChannelAdapter(private val subscriptions: MutableList<Subscription>) :
    RecyclerView.Adapter<SubscriptionChannelViewHolder>() {
    override fun getItemCount(): Int {
        return subscriptions.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SubscriptionChannelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val channelSubscriptionRowBinding =
            ChannelSubscriptionRowBinding.inflate(layoutInflater, parent, false)
        return SubscriptionChannelViewHolder(channelSubscriptionRowBinding)
    }

    override fun onBindViewHolder(holder: SubscriptionChannelViewHolder, position: Int) =
        with(holder.channelSubscriptionRowBinding) {
            val subscription = subscriptions[position]
            Picasso.get().load(subscription.avatar).into(subscriptionChannelImage)
            subscriptionChannelName.text = subscription.name

            root.setOnClickListener {
                val activity = root.context as MainActivity
                val bundle = bundleOf(KEY_CHANNEL_ID to subscription.url)
                activity.navController.navigate(R.id.channelFragment, bundle)
            }
        }
}

class SubscriptionChannelViewHolder(val channelSubscriptionRowBinding: ChannelSubscriptionRowBinding) :
    RecyclerView.ViewHolder(channelSubscriptionRowBinding.root)
