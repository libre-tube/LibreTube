package com.github.libretube.util

import android.content.Context
import android.net.Uri
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.InputStream
import java.io.OutputStream

object MediaExporter {
    /**
     * Asynchronously starts muxing an audio and a video file into a single file.
     */
    @OptIn(UnstableApi::class)
    fun muxMedia(
        context: Context,
        audioUri: Uri,
        videoUri: Uri,
        outputPath: String,
        onCompletion: () -> Unit,
        onError: (ExportException) -> Unit
    ) {
        val video = EditedMediaItem.Builder(MediaItem.fromUri(videoUri)).build()
        val audio = EditedMediaItem.Builder(MediaItem.fromUri(audioUri)).build()

        val videoSequence = EditedMediaItemSequence.withVideoFrom(listOf(video))
        val audioSequence = EditedMediaItemSequence.withAudioFrom(listOf(audio))
        val composition = Composition.Builder(videoSequence, audioSequence).build()

        val transformer = Transformer.Builder(context).build()
        val handler = Handler(transformer.applicationLooper)

        handler.post {
            transformer.addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onCompletion()
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    onError(exportException)
                }
            })

            transformer.start(composition, outputPath)
        }
    }

    fun copyFileContents(input: InputStream, output: OutputStream) {
        output.use { outputStream ->
            input.use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    fun copyFileContents(context: Context, input: Uri, output: Uri) {
        copyFileContents(
            context.contentResolver.openInputStream(input)!!,
            context.contentResolver.openOutputStream(output)!!
        )
    }
}