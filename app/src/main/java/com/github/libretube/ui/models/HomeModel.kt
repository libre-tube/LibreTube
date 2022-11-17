package com.github.libretube.ui.models

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.util.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeModel : ViewModel() {
    val feed = MutableLiveData<List<StreamItem>>()
    var trending = MutableLiveData<List<StreamItem>>()
    val playlists = MutableLiveData<List<Playlists>>()

    suspend fun fetchHome(context: Context, trendingRegion: String) {
        val token = PreferenceHelper.getToken()
        val appContext = context.applicationContext
        runOrError(appContext) {
            trending.postValue(RetrofitInstance.api.getTrending(trendingRegion))
        }

        runOrError(appContext) {
            feed.postValue(RetrofitInstance.authApi.getFeed(token))
        }

        runOrError(appContext) {
            playlists.postValue(RetrofitInstance.authApi.getUserPlaylists(token))
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
