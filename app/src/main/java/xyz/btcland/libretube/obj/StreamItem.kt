package xyz.btcland.libretube.obj

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import xyz.btcland.libretube.obj.search.SearchChannel
import xyz.btcland.libretube.obj.search.SearchPlaylist

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes(value =[
    JsonSubTypes.Type(SearchChannel::class),
    JsonSubTypes.Type(SearchPlaylist::class)
])
data class StreamItem(
    var url: String?,
    var title: String?,
    var thumbnail: String?,
    var uploaderName: String?,
    var uploaderUrl: String?,
    var uploaderAvatar: String?,
    var uploadedDate: String?,
    var duration: Long?,
    var views: Long?,
    var uploaderVerified: Boolean?
){
    constructor() : this("","","","","","","",0,0,null)
}
