package com.github.libretube.util

import android.content.Context
import android.net.Uri
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.api.obj.Streams
import java.io.File
import java.io.FileOutputStream

class MetadataHelper(
    private val context: Context
) {
    private val mapper = ObjectMapper()
    private val metadataDir = DownloadHelper.getDownloadDir(context, DownloadHelper.METADATA_DIR)

    fun createMetadata(fileName: String, streams: Streams) {
        val targetFile = File(metadataDir, fileName)
        targetFile.createNewFile()

        context.contentResolver.openFileDescriptor(
            Uri.fromFile(targetFile),
            "w"
        )?.use {
            FileOutputStream(it.fileDescriptor).use { fileOutputStream ->
                fileOutputStream.write(
                    mapper.writeValueAsBytes(
                        streams
                    )
                )
            }
        }
    }

    fun getMetadata(fileName: String): Streams? {
        val sourceFile = File(metadataDir, fileName)

        return try {
            val json = context.contentResolver.openInputStream(
                Uri.fromFile(sourceFile)
            )?.use {
                it.bufferedReader().use { reader -> reader.readText() }
            }
            mapper.readValue(json, Streams::class.java)
        } catch (e: Exception) {
            return null
        }
    }
}
