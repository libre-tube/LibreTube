package com.github.libretube.ui.models

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Subscription
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SubscriptionsViewModel : ViewModel() {
    var errorResponse = MutableLiveData<Boolean>()
    var videoFeed = MutableLiveData<List<StreamItem>?>()
    var subscriptions = MutableLiveData<List<Subscription>?>()
    private var loadingSubscriptions: Boolean = false

    fun fetchFeed(start: Long? = null) {
        if (loadingSubscriptions) return
        loadingSubscriptions = true

        if (start == null) videoFeed.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val videoFeed = try {
                SubscriptionHelper.getFeed(start)
            } catch (e: Exception) {
                errorResponse.postValue(true)
                Log.e(TAG(), e.toString())
                return@launch
            }
            this@SubscriptionsViewModel.videoFeed.postValue(
                this@SubscriptionsViewModel.videoFeed.value.orEmpty().plus(videoFeed),
            )
            // save the last recent video to the prefs for the notification worker
            if (start == null) videoFeed.firstOrNull()?.let {
                PreferenceHelper.setLatestVideoId(it.url!!.toID())
            }
            loadingSubscriptions = false
        }
    }

    fun fetchSubscriptions() {
        viewModelScope.launch(Dispatchers.IO) {
            val subscriptions = try {
                SubscriptionHelper.getSubscriptions()
            } catch (e: Exception) {
                errorResponse.postValue(true)
                Log.e(TAG(), e.toString())
                return@launch
            }
            this@SubscriptionsViewModel.subscriptions.postValue(subscriptions)
        }
    }
}
