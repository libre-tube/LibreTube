package xyz.btcland.libretube.obj.search

data class SearchPlaylist(
    var name: String? = null,
    var thumbnail: String? = null,
    var url: String? = null,
    var uploaderName: String? =null,
    var videos: Long = -1

)
