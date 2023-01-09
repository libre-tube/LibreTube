package com.github.libretube.enums

enum class PlayerEvent(val value: Int) {
    Pause(0),
    Play(1),
    Forward(2),
    Rewind(3),
    Next(5),
    Prev(6),
    Background(7);

    companion object {
        fun fromInt(value: Int) = PlayerEvent.values().first { it.value == value }
    }
}
