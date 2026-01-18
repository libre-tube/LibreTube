package com.github.libretube.player.parser

import video_streaming.UmpPartId.UMPPartId

/**
 * A parser to read UMP data.
 *
 * Example:
 * ```
 * val bytes = byteArrayOf(20, 1, 42)
 * val parser = Parser(bytes)
 * val part = parser.readPart()
 * assert(part.type == UMPPartId.MEDIA_HEADER)
 * assert(part.data == byteArrayOf(42))
 * ```
 */
class UmpParser(private var buf: ByteArray) {
    private var position = 0

    /**
     * Reads a single byte from the buffer.
     *
     * Example:
     * ```
     * val bytes = byteArrayOf(20, 1, 42)
     * val parser = Parser(bytes)
     * val byte = parser.readByte() // returns 20
     * ```
     */
    private fun readByte(): UByte? {
        if (position >= buf.size) return null
        return buf[position++].toUByte()
    }

    /**
     * Reads `n` bytes from the buffer.
     *
     * Example:
     * ```
     * val bytes = byteArrayOf(20, 1, 42)
     * val parser = Parser(bytes)
     * val data = parser.readBytes(2) // returns byteArrayOf(20, 1)
     * ```
     */
    private fun readBytes(n: Int): ByteArray? {
        if (position + n > buf.size) return null
        val result = buf.copyOfRange(position, position + n)
        position += n
        return result
    }

    /**
     * Read a variable sized integer from the buffer.
     *
     * The implementation follows https://github.com/gsuberland/UMP_Format/blob/main/UMP_Format.md#variable-sized-integers
     *
     * Example:
     * ```
     * val bytes = byteArrayOf(0x80.toByte(), 0x01)
     * val parser = Parser(bytes)
     * val value = parser.readVarint() // returns 64
     * ```
     */
    fun readVarint(): UInt? {
        val prefix = readByte() ?: return null

        // decode the size from the first 5 bits
        // [0...4] bits corresponds to a size of 1...5 bytes
        //val varintSize = minOf(prefix.countLeadingZeroBits(), 4) + 1
        val varintSize = minOf(prefix.inv().countLeadingZeroBits(), 4) + 1


        var shift = 0
        var result = 0u

        if (varintSize != 5) {
            shift = 8 - varintSize
            // compute mask of prefix
            val mask = (1u shl shift) - 1u
            result = result or (prefix.toUInt() and mask)
        }

        for (i in 1 until varintSize) {
            val byte = readByte()?.toUInt() ?: return null
            result = result or (byte shl shift)
            shift += 8
        }

        return result
    }

    /**
     * Returns the remaining data of the buffer.
     *
     * Example:
     * ```
     * val bytes = byteArrayOf(0x80.toByte(), 0x01)
     * val parser = Parser(bytes)
     * parser.readBytes(1)
     * val remaining = parser.data() // returns byteArrayOf(0x01)
     * ```
     */
    fun data(): ByteArray {
        return buf.copyOfRange(position, buf.size)
    }

    /**
     * Read a single [Part].
     *
     * Each part consist of a type (identified via a [UMPPartId]) and some data.
     *
     * https://github.com/davidzeng0/innertube/blob/main/googlevideo/ump.md#high-level-structure
     *
     * Example:
     * ```
     * val bytes = byteArrayOf(20, 1, 42)
     * val parser = Parser(bytes)
     * val part = parser.readPart()
     * // part.type == UMPPartId.MEDIA_HEADER
     * // part.data == byteArrayOf(42)
     * ```
     */
    fun readPart(): Part? {
        val ty = readVarint() ?: return null
        val umpType = UMPPartId.forNumber(ty.toInt()) ?: UMPPartId.UNKNOWN

        val size = readVarint() ?: return null
        val data = readBytes(size.toInt()) ?: return null

        return Part(umpType, data)
    }
}

/**
 * A single segment (part) of a UMP stream.
 *
 * Each part has an identifying type and may have associated data.
 */
data class Part(
    /**
     * Type of the part.
     *
     * Set to [UMPPartId.UNKNOWN] if the type could not be identified.
     */
    val type: UMPPartId,

    /**
     * Associated data of the part.
     *
     * May be empty.
     */
    val data: ByteArray
)