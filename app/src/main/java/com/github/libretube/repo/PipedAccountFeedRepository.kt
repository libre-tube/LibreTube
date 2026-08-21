package com.github.libretube.repo

import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.helpers.PreferenceHelper

class PipedAccountFeedRepository : FeedRepository {

    private val token get() = PreferenceHelper.getToken()

    override suspend fun getFeed(
        forceRefresh: Boolean,
        onProgressUpdate: (FeedProgress) -> Unit
    ): List<StreamItem> {
        return RetrofitInstance.authApi.getFeed(token)
    }
}
