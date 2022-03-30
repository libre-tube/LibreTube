package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.obj.Subscription
import com.squareup.picasso.Picasso

class SubscriptionChannelAdapter(private val subscriptions: MutableList<Subscription>) : RecyclerView.Adapter<SubscriptionChannelViewHolder>() {
    override fun getItemCount(): Int {
        return subscriptions.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionChannelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(R.layout.channel_subscription_row, parent, false)
        return SubscriptionChannelViewHolder(cell)
    }

    override fun onBindViewHolder(holder: SubscriptionChannelViewHolder, position: Int) {
        val subscription = subscriptions[position]
        holder.v.findViewById<TextView>(R.id.subscription_channel_name).text = subscription.name
        val avatar = holder.v.findViewById<ImageView>(R.id.subscription_channel_image)
        Picasso.get().load(subscription.avatar).into(avatar)
        holder.v.setOnClickListener {
            val activity = holder.v.context as MainActivity
            val bundle = bundleOf("channel_id" to subscription.url)
            activity.navController.navigate(R.id.channel, bundle)
        }
    }
}
class SubscriptionChannelViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
