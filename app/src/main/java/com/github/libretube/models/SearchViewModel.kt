package com.github.libretube.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SearchViewModel : ViewModel() {
    var searchQuery = MutableLiveData<String>()

    fun setQuery(query: String?) {
        this.searchQuery.value = query
    }
}
