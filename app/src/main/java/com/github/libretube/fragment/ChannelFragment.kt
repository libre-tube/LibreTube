package com.github.libretube.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.RetrofitInstance
import com.github.libretube.adapters.ChannelAdapter
import com.github.libretube.databinding.FragmentChannelBinding
import com.github.libretube.formatShort
import com.github.libretube.model.Subscribe
import com.github.libretube.utils.runOnUiThread
import com.squareup.picasso.Picasso
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "ChannelFragment"
const val KEY_CHANNEL_ID = "channel_id"

class ChannelFragment : Fragment() {
    private lateinit var binding: FragmentChannelBinding
    private var channelId: String? = null
    private var nextPage: String? = null
    private var channelAdapter: ChannelAdapter? = null
    private var isLoading = true
    private var isSubscribed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            channelId = it.getString(KEY_CHANNEL_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentChannelBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        channelId = channelId!!.replace("/channel/", "")
        binding.tvChannelName.text = channelId
        binding.rvChannels.layoutManager = LinearLayoutManager(context)
        fetchChannel()
        val sharedPref = context?.getSharedPreferences(SHARED_PREFERENCES_KEY_TOKEN, Context.MODE_PRIVATE)
        if (sharedPref?.getString(SHARED_PREFERENCES_KEY_TOKEN, "") != "") {
            isSubscribed()
        }
        binding.svChannel.viewTreeObserver
            .addOnScrollChangedListener {
                if (binding.svChannel.getChildAt(0).bottom
                    == (binding.svChannel.height + binding.svChannel.scrollY)
                ) {
                    //scroll view is at bottom
                    if (nextPage != null && !isLoading) {
                        isLoading = true
                        fetchNextPage()
                    }
                }
            }
    }

    private fun isSubscribed() {
        @SuppressLint("ResourceAsColor")
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences(SHARED_PREFERENCES_KEY_TOKEN, Context.MODE_PRIVATE)
                    RetrofitInstance.api.isSubscribed(
                        channelId!!,
                        sharedPref?.getString(SHARED_PREFERENCES_KEY_TOKEN, "")!!
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                val colorPrimary = TypedValue()
                (context as Activity).theme.resolveAttribute(
                    android.R.attr.colorPrimary,
                    colorPrimary,
                    true
                )

                val colorText = TypedValue()
                (context as Activity).theme.resolveAttribute(
                    R.attr.colorOnSurface,
                    colorText,
                    true
                )

                runOnUiThread {
                    if (response.subscribed == true) {
                        isSubscribed = true
                        binding.btnSubscribeChannel.text = getString(R.string.unsubscribe)
                        binding.btnSubscribeChannel.setTextColor(colorText.data)

                    }
                    if (response.subscribed != null) {
                        binding.btnSubscribeChannel.setOnClickListener {
                            if (isSubscribed) {
                                unsubscribe()
                                binding.btnSubscribeChannel.text = getString(R.string.subscribe)
                                binding.btnSubscribeChannel.setTextColor(colorPrimary.data)
                            } else {
                                subscribe()
                                binding.btnSubscribeChannel.text = getString(R.string.unsubscribe)
                                binding.btnSubscribeChannel.setTextColor(colorText.data)
                            }
                        }
                    }
                }
            }
        }
        run()
    }

    private fun subscribe() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences(SHARED_PREFERENCES_KEY_TOKEN, Context.MODE_PRIVATE)
                    RetrofitInstance.api.subscribe(
                        sharedPref?.getString(SHARED_PREFERENCES_KEY_TOKEN, "")!!,
                        Subscribe(channelId)
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response$e")
                    return@launchWhenCreated
                }
                isSubscribed = true
            }
        }
        run()
    }

    private fun unsubscribe() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    val sharedPref = context?.getSharedPreferences(SHARED_PREFERENCES_KEY_TOKEN, Context.MODE_PRIVATE)
                    RetrofitInstance.api.unsubscribe(
                        sharedPref?.getString(SHARED_PREFERENCES_KEY_TOKEN, "")!!,
                        Subscribe(channelId)
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                isSubscribed = false
            }
        }
        run()
    }

    private fun fetchChannel() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getChannel(channelId!!)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                isLoading = false

                runOnUiThread {
                    binding.tvChannelName.text = response.name
                    binding.tvChannelSubscriptions.text =
                        response.subscriberCount.formatShort() + " subscribers"
                    binding.tvChannelDescription.text = response.description

                    Picasso.get().load(response.bannerUrl).into(binding.ivChannelBanner)
                    Picasso.get().load(response.avatarUrl).into(binding.ivChannel)

                    channelAdapter = ChannelAdapter(response.relatedStreams!!.toMutableList())
                    binding.rvChannels.adapter = channelAdapter
                }
            }
        }
        run()
    }

    private fun fetchNextPage() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getChannelNextPage(channelId!!, nextPage!!)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response," + e.response())
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                channelAdapter?.updateItems(response.relatedStreams!!)
                isLoading = false
            }
        }
        run()
    }

    override fun onDestroyView() {
        channelAdapter = null
        binding.rvChannels.adapter = null
        super.onDestroyView()
    }
}
