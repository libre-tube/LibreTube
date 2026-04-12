package com.github.libretube.ui.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlaylistViewModel: ViewModel() {
    val searchQuery = MutableLiveData<String?>(null)

    fun setQuery(query: String?) {
        searchQuery.value = query
    }
}