package xyz.btcland.libretube.obj.search

data class SearchChannel(
    var name: String? = null,
    var thumbnail: String? = null,
    var url: String? = null,
    var description: String? = null,
    var subscribers: Long? = -1,
    var videos: Long? = -1,
    var verified: Boolean? = null
)
