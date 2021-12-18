package xyz.btcland.libretube.obj

data class ChapterSegment(
    var title: String?,
    var image: String?,
    var start: Int?
){
    constructor(): this("","",-1)
}
