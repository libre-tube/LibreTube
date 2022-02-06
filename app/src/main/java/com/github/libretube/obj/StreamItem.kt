package com.github.libretube.obj

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
    var uploaderVerified: Boolean?,
    var uploaded: Long?
){
    constructor() : this("","","","","","","",0,0,null,0)
}
