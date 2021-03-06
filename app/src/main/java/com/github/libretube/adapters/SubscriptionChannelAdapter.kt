package com.github.libretube.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.ChannelSubscriptionRowBinding
import com.github.libretube.obj.Subscribe
import com.github.libretube.obj.Subscription
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.NavigationHelper
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.toID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SubscriptionChannelAdapter(private val subscriptions: MutableList<Subscription>) :
    RecyclerView.Adapter<SubscriptionChannelViewHolder>() {
    val TAG = "SubChannelAdapter"

    private var subscribed = true

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
        holder.binding.apply {
            subscriptionChannelName.text = subscription.name
            ConnectionHelper.loadImage(subscription.avatar, subscriptionChannelImage)
            root.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, subscription.url)
            }
            subscriptionSubscribe.setOnClickListener {
                val channelId = subscription.url.toID()
                if (subscribed) {
                    subscriptionSubscribe.text = root.context.getString(R.string.subscribe)
                    unsubscribe(channelId)
                    subscribed = false
                } else {
                    subscriptionSubscribe.text =
                        root.context.getString(R.string.unsubscribe)
                    subscribe(channelId)
                    subscribed = true
                }
            }
        }
    }

    private fun subscribe(channelId: String) {
        fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val token = PreferenceHelper.getToken()
                    RetrofitInstance.authApi.subscribe(
                        token,
                        Subscribe(channelId)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
            }
        }
        run()
    }

    private fun unsubscribe(channelId: String) {
        fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val token = PreferenceHelper.getToken()
                    RetrofitInstance.authApi.unsubscribe(
                        token,
                        Subscribe(channelId)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
            }
        }
        run()
    }
}

class SubscriptionChannelViewHolder(val binding: ChannelSubscriptionRowBinding) :
    RecyclerView.ViewHolder(binding.root)
