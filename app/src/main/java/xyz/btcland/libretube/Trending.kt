package xyz.btcland.libretube

data class Trending(
    val url: String,
    val title: String,
    val thumbnail: String,
    val uploaderName: String,
    val uploaderUrl:String,
    val uploaderAvatar:String,
    val uploadedDate: String,
    val duration: Int,
    val views: Int,
    val uploaderVerified: Boolean
)
