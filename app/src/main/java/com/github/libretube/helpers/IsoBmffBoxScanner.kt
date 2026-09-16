package com.github.libretube.helpers

import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream

enum class IsoBmffScanResult {
    /** File is a well-formed ISO-BMFF / MP4 box sequence that ends at EOF. */
    Ok,

    /** File looks like ISO-BMFF but a box extends past EOF or leftover bytes remain. */
    Truncated,

    /** File is empty, too small, or not ISO-BMFF (e.g. WebM). */
    NotIsoBmff,
}

/**
 * Walks ISO-BMFF boxes to detect truncated MP4 / fMP4 files without decoding samples.
 */
object IsoBmffBoxScanner {
    private val isoBoxTypes = setOf(
        "ftyp", "styp", "moov", "moof", "mdat", "mfra", "sidx", "ssix",
        "free", "skip", "wide", "uuid", "pssh", "emsg", "meta", "meco"
    )

    fun scan(path: Path): IsoBmffScanResult {
        val size = runCatching { path.fileSize() }.getOrDefault(0L)
        if (size <= 0L) return IsoBmffScanResult.NotIsoBmff
        return path.inputStream().use { scan(it, size) }
    }

    fun scan(bytes: ByteArray): IsoBmffScanResult {
        if (bytes.isEmpty()) return IsoBmffScanResult.NotIsoBmff
        return scan(bytes.inputStream(), bytes.size.toLong())
    }

    fun scan(input: InputStream, fileSize: Long): IsoBmffScanResult {
        if (fileSize < 8L) return IsoBmffScanResult.NotIsoBmff
        val data = DataInputStream(input)
        var offset = 0L
        var firstBox = true
        return try {
            while (offset < fileSize) {
                val remaining = fileSize - offset
                if (remaining < 8L) return IsoBmffScanResult.Truncated

                val size32 = data.readInt().toLong() and 0xffffffffL
                val type = ByteArray(4).also { data.readFully(it) }.toString(Charsets.US_ASCII)
                var headerSize = 8L
                val boxSize = when {
                    size32 == 1L -> {
                        if (remaining < 16L) return IsoBmffScanResult.Truncated
                        headerSize = 16L
                        data.readLong()
                    }
                    size32 == 0L -> remaining
                    else -> size32
                }

                if (firstBox) {
                    firstBox = false
                    if (type !in isoBoxTypes) return IsoBmffScanResult.NotIsoBmff
                }
                if (boxSize < headerSize || offset + boxSize > fileSize) {
                    return IsoBmffScanResult.Truncated
                }

                data.skipFully(boxSize - headerSize)
                offset += boxSize
            }
            if (offset == fileSize) IsoBmffScanResult.Ok else IsoBmffScanResult.Truncated
        } catch (_: EOFException) {
            IsoBmffScanResult.Truncated
        }
    }

    private fun DataInputStream.skipFully(n: Long) {
        var remaining = n
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
                continue
            }
            if (read() == -1) throw EOFException()
            remaining--
        }
    }
}
