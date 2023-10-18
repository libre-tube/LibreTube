package com.github.libretube.json

import android.util.Log
import com.github.libretube.extensions.TAG
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object SafeInstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", STRING)

    override fun deserialize(decoder: Decoder): Instant {
        val string = decoder.decodeString()
        return try {
            string.toInstant()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG(), "Error parsing date '$string'", e)
            string.toLocalDate().atStartOfDayIn(TimeZone.currentSystemDefault())
        }
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }
}
