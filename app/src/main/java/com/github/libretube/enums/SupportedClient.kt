package com.github.libretube.enums

enum class ImportFormat(val value: Int) {
    NEWPIPE(0),
    FREETUBE(1),
    YOUTUBECSV(2);

    companion object {
        fun fromInt(value: Int) = ImportFormat.values().first { it.value == value }
    }
}