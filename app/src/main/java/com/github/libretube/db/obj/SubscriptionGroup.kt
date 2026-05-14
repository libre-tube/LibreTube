package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.Ignore
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
    var index: Int = 0,

    @Ignore
    var id: String = "",
) {
    constructor(name: String) : this(name = name, channels = emptyList())

    init {
        // in LibreTube, we identify channel groups by their name
        // so for compatibility with other APIs, the ID is set to the name of the group
        if (id.isEmpty()) id = name
    }
}
