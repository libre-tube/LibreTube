package com.github.libretube.services

import android.os.Bundle
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.libretube.extensions.togglePlayPauseState
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@androidx.annotation.OptIn(UnstableApi::class)
class OnlinePlayerServiceTest {
    @Test
    fun `idle player with an error keeps the service alive`() {
        val player = TestPlayer(playerError = playbackError())

        assertFalse(shouldStopOnlinePlayerService(player.instance, Player.STATE_IDLE))
    }

    @Test
    fun `idle player without an error stops the service`() {
        val player = TestPlayer()

        assertTrue(shouldStopOnlinePlayerService(player.instance, Player.STATE_IDLE))
    }

    @Test
    fun `retry prepares an errored player before playing`() {
        val player = TestPlayer(playerError = playbackError())

        player.instance.togglePlayPauseState()

        assertEquals(listOf("prepare", "play"), player.calls)
    }

    private fun playbackError(): PlaybackException = TestPlaybackException()

    private class TestPlaybackException : PlaybackException(
        "Playback failed",
        null,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        Bundle.EMPTY,
        0L
    )

    private class TestPlayer(
        private val playerError: PlaybackException? = null
    ) : InvocationHandler {
        val calls = mutableListOf<String>()

        val instance: Player = Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
            this
        ) as Player

        override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
            return when (method.name) {
                "getPlayerError" -> playerError
                "getPlaybackState" -> Player.STATE_IDLE
                "isPlaying" -> false
                "prepare", "play" -> {
                    calls += method.name
                    null
                }
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "TestPlayer"
                else -> defaultValue(method.returnType)
            }
        }

        private fun defaultValue(type: Class<*>): Any? = when (type) {
            Boolean::class.javaPrimitiveType -> false
            Byte::class.javaPrimitiveType -> 0.toByte()
            Short::class.javaPrimitiveType -> 0.toShort()
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            Float::class.javaPrimitiveType -> 0F
            Double::class.javaPrimitiveType -> 0.0
            Char::class.javaPrimitiveType -> '\u0000'
            else -> null
        }
    }
}
