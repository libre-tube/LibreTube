package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@Entity(tableName = "subscriptionGroups")
data class SubscriptionGroup(
    @PrimaryKey
    @SerialName("groupName")
    @JsonNames("groupName", "name")
    var name: String,
    var channels: List<String> = listOf(),
    var index: Int = 0
)
