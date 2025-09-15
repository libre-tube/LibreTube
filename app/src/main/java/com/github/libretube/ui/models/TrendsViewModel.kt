package com.github.libretube.ui.models

import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.TrendingCategory
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrendsViewModel : ViewModel() {
    val trendingVideos = MutableLiveData<Map<TrendingCategory, List<StreamItem>>>()
    var recyclerViewState: Parcelable? = null

    private var currentJob: Job? = null

    fun fetchTrending(context: Context, category: TrendingCategory) {
        // cancel previously started, still running requests as users can only see one tab at a time,
        // so it doesn't make sense to continue loading the previously seen (now hidden) tab data
        runCatching { currentJob?.cancel() }

        currentJob = viewModelScope.launch {
            try {
                val region = PreferenceHelper.getTrendingRegion(context)
                val response = withContext(Dispatchers.IO) {
                    MediaServiceRepository.instance.getTrending(region, category)
                }
                setStreamsForCategory(category, response)
            } catch (e: Exception) {
                Log.e(TAG(), e.stackTraceToString())
                context.toastFromMainDispatcher(e.localizedMessage.orEmpty())
            }
        }
    }

    fun setStreamsForCategory(category: TrendingCategory, streams: List<StreamItem>) {
        val newState = trendingVideos.value.orEmpty()
            .toMutableMap()
            .apply {
                put(category, streams)
            }
        trendingVideos.postValue(newState)
    }
}
