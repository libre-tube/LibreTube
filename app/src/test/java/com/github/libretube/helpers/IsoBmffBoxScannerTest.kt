package com.github.libretube.helpers

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class IsoBmffBoxScannerTest {
    @Test
    fun emptyFileIsNotIsoBmff() {
        assertEquals(IsoBmffScanResult.NotIsoBmff, IsoBmffBoxScanner.scan(byteArrayOf()))
    }

    @Test
    fun webmHeaderIsNotIsoBmff() {
        val webm = byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte(), 0, 0, 0, 0)
        assertEquals(IsoBmffScanResult.NotIsoBmff, IsoBmffBoxScanner.scan(webm))
    }

    @Test
    fun validBoxesScanOk() {
        val ftyp = box("ftyp", 24)
        val mdat = box("mdat", 16)
        assertEquals(IsoBmffScanResult.Ok, IsoBmffBoxScanner.scan(ftyp + mdat))
    }

    @Test
    fun truncatedBoxIsDetected() {
        val header = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
        header.putInt(100)
        header.put("mdat".toByteArray(Charsets.US_ASCII))
        assertEquals(IsoBmffScanResult.Truncated, IsoBmffBoxScanner.scan(header.array()))
    }

    @Test
    fun leftoverBytesAreTruncated() {
        val ftyp = box("ftyp", 16)
        assertEquals(IsoBmffScanResult.Truncated, IsoBmffBoxScanner.scan(ftyp + byteArrayOf(0x00)))
    }

    private fun box(type: String, size: Int): ByteArray {
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(size)
        buffer.put(type.toByteArray(Charsets.US_ASCII))
        return buffer.array()
    }
}
