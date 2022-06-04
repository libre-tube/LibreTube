package com.github.libretube.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.MainActivity
import com.github.libretube.R
import com.github.libretube.obj.Subscribe
import com.github.libretube.obj.Subscription
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
    override fun getItemCount(): Int {
        return subscriptions.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
        SubscriptionChannelViewHolder {
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
        val unsubscribeBtn = holder.v.findViewById<com.google.android.material.button.MaterialButton>(R.id.subs_subscribe)
        unsubscribeBtn.setOnClickListener {
            val channel_id = subscription.url?.replace("/channel/", "")!!
            unsubscribe(holder.v.context, channel_id)
        }
    }

    private fun unsubscribe(context: Context, channel_id: String) {
        fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                val response = try {
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    RetrofitInstance.api.unsubscribe(
                        sharedPref?.getString("token", "")!!,
                        Subscribe(channel_id)
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                }
            }
        }
        run()
    }
}

class SubscriptionChannelViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
