package com.github.libretube.ui.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DownloadsViewModel: ViewModel() {
    val searchQuery = MutableLiveData<String?>(null)

    fun setQuery(query: String?) {
        searchQuery.value = query
    }
}