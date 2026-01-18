package com.github.libretube.player.parser


import org.junit.Assert.*
import org.junit.Test

class CompositeBufferTest {
    @Test
    fun testReadSingleChunk() {
        val buffer = CompositeBuffer(
            listOf(byteArrayOf(1, 2, 3, 4, 5))
        )

        val dst = ByteArray(5)
        buffer.read(dst, 0, 5)

        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), dst)
        assertEquals(0, buffer.remaining())
        assertFalse(buffer.hasRemaining())
    }

    @Test
    fun testReadAcrossMultipleChunks() {
        val buffer = CompositeBuffer(
            listOf(
                byteArrayOf(1, 2),
                byteArrayOf(3, 4, 5),
                byteArrayOf(6)
            )
        )

        val dst = ByteArray(6)
        buffer.read(dst, 0, 6)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6), dst)
        assertEquals(0, buffer.remaining())
        assertFalse(buffer.hasRemaining())
    }

    @Test
    fun testPartialRead() {
        val buffer = CompositeBuffer(
            listOf(
                byteArrayOf(1, 2, 3),
                byteArrayOf(4, 5)
            )
        )

        val dst = ByteArray(2)
        buffer.read(dst, 0, 2)

        assertArrayEquals(byteArrayOf(1, 2), dst)
        assertEquals(3, buffer.remaining())
        assertTrue(buffer.hasRemaining())
    }

    @Test
    fun testSequentialReadsMaintainPosition() {
        val buffer = CompositeBuffer(
            listOf(
                byteArrayOf(1, 2),
                byteArrayOf(3, 4, 5)
            )
        )

        val first = ByteArray(3)
        buffer.read(first, 0, 3)
        assertArrayEquals(byteArrayOf(1, 2, 3), first)

        val second = ByteArray(2)
        buffer.read(second, 0, 2)
        assertArrayEquals(byteArrayOf(4, 5), second)
        assertFalse(buffer.hasRemaining())
    }

    @Test
    fun testReadWithOffsetInDestination() {
        val buffer = CompositeBuffer(
            listOf(byteArrayOf(7, 8, 9))
        )

        val dst = ByteArray(5) { 0 }
        buffer.read(dst, 1, 3)
        assertArrayEquals(byteArrayOf(0, 7, 8, 9, 0), dst)
    }
}
