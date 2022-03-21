package com.github.libretube.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.RetrofitInstance
import com.github.libretube.adapters.SubscriptionAdapter
import com.github.libretube.adapters.SubscriptionChannelAdapter
import com.github.libretube.databinding.FragmentSubscriptionsBinding
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "SubFragment"

class SubscriptionsFragment : Fragment() {
    private lateinit var binding: FragmentSubscriptionsBinding
    private lateinit var token: String
    private var subscriptionAdapter: SubscriptionAdapter? = null
    private var isLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSubscriptionsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPref = context?.getSharedPreferences(SHARED_PREFERENCES_KEY_TOKEN, Context.MODE_PRIVATE)
        token = sharedPref?.getString(SHARED_PREFERENCES_KEY_TOKEN, "")!!
        Log.e(TAG, token)
        if (token != "") {
            binding.loginOrRegister.isVisible = false
            binding.pbSubscriptions.isVisible = true
            binding.srlSubscriptions.isEnabled = true

            binding.subChannels.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            binding.rvSubscriptionFeed.layoutManager =
                GridLayoutManager(view.context, resources.getInteger(R.integer.grid_items))

            fetchChannels()
            fetchFeed()

            binding.srlSubscriptions.setOnRefreshListener {
                fetchChannels()
                fetchFeed()
            }

            binding.svSubscriptions.viewTreeObserver
                .addOnScrollChangedListener {
                    if (binding.svSubscriptions.getChildAt(0).bottom
                        == (binding.svSubscriptions.height + binding.svSubscriptions.scrollY)
                    ) {
                        //scroll view is at bottom
                        if (isLoaded) {
                            binding.srlSubscriptions.isRefreshing = true
                            subscriptionAdapter?.updateItems()
                            binding.srlSubscriptions.isRefreshing = false
                        }
                    }
                }
        } else {
            binding.srlSubscriptions.isEnabled = false
        }
    }

    private fun fetchFeed() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.getFeed(token)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                } finally {
                    binding.srlSubscriptions.isRefreshing = false
                }
                if (response.isNotEmpty()) {
                    subscriptionAdapter = SubscriptionAdapter(response)
                    binding.rvSubscriptionFeed.adapter = subscriptionAdapter
                    subscriptionAdapter?.updateItems()
                }
                binding.pbSubscriptions.isVisible = false
                isLoaded = true
            }
        }
        run()
    }

    private fun fetchChannels() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.subscriptions(token)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                } finally {
                    binding.srlSubscriptions.isRefreshing = false
                }
                if (response.isNotEmpty()) {
                    binding.subChannels.adapter =
                        SubscriptionChannelAdapter(response.toMutableList())
                } else {
                    Toast.makeText(context, R.string.subscribeIsEmpty, Toast.LENGTH_SHORT).show()
                }
            }
        }
        run()
    }

    override fun onStop() {
        Log.e(TAG, "Stopped")
        subscriptionAdapter = null
        binding.rvSubscriptionFeed.adapter = null
        super.onStop()
    }

    override fun onDestroy() {
        Log.e(TAG, "Destroyed")
        super.onDestroy()
    }
}
