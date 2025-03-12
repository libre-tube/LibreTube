package com.github.libretube.db.obj

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.github.libretube.ui.dialogs.ShareDialog
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "localSubscription")
data class LocalSubscription(
    @PrimaryKey val channelId: String,
    @Ignore val url: String = "",

    @ColumnInfo(defaultValue = "NULL") val name: String? = null,
    @ColumnInfo(defaultValue = "NULL") val avatar: String? = null,
    @ColumnInfo(defaultValue = "false") val verified: Boolean = false
) {
    constructor(
        channelId: String,
        name: String? = null,
        avatar: String? = null,
        verified: Boolean = false
    ) : this(channelId, "${ShareDialog.YOUTUBE_FRONTEND_URL}/channel/$channelId", name, avatar, verified)
}
