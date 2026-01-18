package com.github.libretube.player.parser

import kotlin.math.min

/**
 * A buffer that wraps multiple smaller contiguous buffers,
 * allowing to treat them, as if they were a single buffer.
 */
class CompositeBuffer(
    private val data: List<ByteArray>,
) {
    private var position: Int = 0
    private val length = data.sumOf { it.size }

    /**
     * Returns the number of [Byte]s remaining in the buffer.
     */
    fun remaining() = length - position

    /**
     * Returns whether the buffer still has [Byte]s to read.
     */
    fun hasRemaining() = position != length

    /**
     * Transfers [length] bytes into [destination] starting at [offset].
     */
    fun read(destination: ByteArray, offset: Int, length: Int) {
        assert(destination.size >= offset + length) { "Destination buffer does not have enough space to hold ${offset + length} bytes" }

        var bytesTransferred = 0
        var globalOffset = 0
        var writeOffset = offset

        for (chunk in data) {
            if (position >= globalOffset + chunk.size) {
                globalOffset += chunk.size
                continue
            }

            val chunkOffset = position - globalOffset
            val available = chunk.size - chunkOffset
            val remaining = length - bytesTransferred
            val toCopy = min(available, remaining)

            chunk.copyInto(
                destination,
                writeOffset,
                chunkOffset,
                chunkOffset + toCopy
            )

            position += toCopy
            writeOffset += toCopy
            bytesTransferred += toCopy

            if (bytesTransferred == length) break
            globalOffset += chunk.size
        }
    }
}