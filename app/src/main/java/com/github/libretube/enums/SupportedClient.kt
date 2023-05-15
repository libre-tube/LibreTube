package com.github.libretube.enums

enum class SupportedClient(val value: Int) {
    LIBRETUBE(0),
    NEWPIPE(1),
    FREETUBE(2);

    companion object {
        fun fromInt(value: Int) = SupportedClient.values().first { it.value == value }
    }
}