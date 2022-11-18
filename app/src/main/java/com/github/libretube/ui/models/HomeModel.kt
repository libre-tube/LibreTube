package com.github.libretube.ui.models

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.ui.extensions.withMaxSize
import com.github.libretube.util.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeModel : ViewModel() {
    val feed = MutableLiveData<List<StreamItem>>()
    var trending = MutableLiveData<List<StreamItem>>()
    val playlists = MutableLiveData<List<Playlists>>()

    suspend fun fetchHome(context: Context, trendingRegion: String, forceReload: Boolean = false) {
        val token = PreferenceHelper.getToken()
        val appContext = context.applicationContext
        runOrError(appContext) {
            if (!feed.value.isNullOrEmpty() && !forceReload) return@runOrError
            feed.postValue(
                SubscriptionHelper.getFeed().withMaxSize(20)
            )
        }

        runOrError(appContext) {
            if (!trending.value.isNullOrEmpty() && !forceReload) return@runOrError
            trending.postValue(
                RetrofitInstance.api.getTrending(trendingRegion).withMaxSize(10)
            )
        }

        runOrError(appContext) {
            if ((token == "" || playlists.value.isNullOrEmpty()) && !forceReload) return@runOrError
            playlists.postValue(
                RetrofitInstance.authApi.getUserPlaylists(token).withMaxSize(20)
            )
        }
    }

    private fun runOrError(context: Context, action: suspend () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                action.invoke()
            } catch (e: Exception) {
                e.localizedMessage?.let { context.toastFromMainThread(it) }
            }
        }
    }
}
