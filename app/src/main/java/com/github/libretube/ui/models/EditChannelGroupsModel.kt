package com.github.libretube.ui.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.libretube.db.obj.SubscriptionGroup

class EditChannelGroupsModel : ViewModel() {
    val groups = MutableLiveData<List<SubscriptionGroup>>()
    var groupToEdit: SubscriptionGroup? = null
}
