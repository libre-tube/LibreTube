package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.activity.MainActivity
import com.github.libretube.R
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
        viewType: Int
    ): SubscriptionChannelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(R.layout.channel_subscription_row, parent, false)
        return SubscriptionChannelViewHolder(cell)
    }

    override fun onBindViewHolder(holder: SubscriptionChannelViewHolder, position: Int) {
        val subscription = subscriptions[position]
        val avatar = holder.view.findViewById<ImageView>(R.id.subscription_channel_image)

        Picasso.get().load(subscription.avatar).into(avatar)

        holder.view.findViewById<TextView>(R.id.subscription_channel_name).text = subscription.name
        holder.view.setOnClickListener {
            val activity = holder.view.context as MainActivity
            val bundle = bundleOf(KEY_CHANNEL_ID to subscription.url)
            activity.navController.navigate(R.id.channelFragment, bundle)
        }
    }
}

class SubscriptionChannelViewHolder(val view: View) : RecyclerView.ViewHolder(view)
