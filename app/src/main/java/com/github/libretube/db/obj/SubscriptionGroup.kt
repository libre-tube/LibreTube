package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptionGroups")
data class SubscriptionGroup(
    @PrimaryKey var name: String,
    val channels: MutableList<String>
)
