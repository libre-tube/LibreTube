package xyz.btcland.libretube.obj

import xyz.btcland.libretube.obj.Subtitle

data class Streams(
    val title: String?,
    val description: String?,
    val uploadDate: String?,
    val uploader: String?,
    val uploaderUrl: String?,
    val uploaderAvatar: String?,
    val thumbnailUrl: String?,
    val hls: String?,
    val dash: String?,
    val lbryId: String?,
    val uploaderVerified: Boolean?,
    val duration: Int?,
    val views: Int?,
    val likes: Int?,
    val dislikes: Int?,
    val audioStreams: List<PipedStream>?,
    val videoStreams: List<PipedStream>?,
    val relatedStreams: List<StreamItem>?,
    val subtitles: List<Subtitle>?,
    val livestream: Boolean?,
    val proxyUrl: String?,
    val chapters: List<ChapterSegment>?
){
    constructor(): this("","","","","","","","","","",null,-1,-1,-1,-1, emptyList(), emptyList(),
        emptyList(), emptyList(), null,"", emptyList())
}
