package com.github.libretube.json

import android.util.Log
import com.github.libretube.extensions.TAG
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object SafeLocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", STRING)

    override fun deserialize(decoder: Decoder): LocalDate {
        val string = decoder.decodeString()
        return try {
            string.toLocalDate()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG(), "Error parsing date '$string'", e)
            string.toInstant().toLocalDateTime(TimeZone.currentSystemDefault()).date
        }
    }

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }
}
