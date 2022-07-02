package com.github.libretube.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.databinding.ChannelSubscriptionRowBinding
import com.github.libretube.obj.Subscribe
import com.github.libretube.obj.Subscription
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.util.RetrofitInstance
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class SubscriptionChannelAdapter(private val subscriptions: MutableList<Subscription>) :
    RecyclerView.Adapter<SubscriptionChannelViewHolder>() {
    val TAG = "SubChannelAdapter"

    private var subscribed = true
    private var isLoading = false

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
            Picasso.get().load(subscription.avatar).into(subscriptionChannelImage)
            root.setOnClickListener {
                val activity = root.context as MainActivity
                val bundle = bundleOf("channel_id" to subscription.url)
                activity.navController.navigate(R.id.channelFragment, bundle)
            }
            subscriptionSubscribe.setOnClickListener {
                if (!isLoading) {
                    isLoading = true
                    val channelId = subscription.url?.replace("/channel/", "")!!
                    if (subscribed) {
                        unsubscribe(root.context, channelId)
                        subscriptionSubscribe.text = root.context.getString(R.string.subscribe)
                    } else {
                        subscribe(root.context, channelId)
                        subscriptionSubscribe.text =
                            root.context.getString(R.string.unsubscribe)
                    }
                }
            }
        }
    }

    private fun subscribe(context: Context, channelId: String) {
        fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                val response = try {
                    val token = PreferenceHelper.getToken(context)
                    RetrofitInstance.api.subscribe(
                        token,
                        Subscribe(channelId)
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                }
                subscribed = true
                isLoading = false
            }
        }
        run()
    }

    private fun unsubscribe(context: Context, channelId: String) {
        fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                val response = try {
                    val token = PreferenceHelper.getToken(context)
                    RetrofitInstance.api.unsubscribe(
                        token,
                        Subscribe(channelId)
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                }
                subscribed = false
                isLoading = false
            }
        }
        run()
    }
}

class SubscriptionChannelViewHolder(val binding: ChannelSubscriptionRowBinding) : RecyclerView.ViewHolder(binding.root)
