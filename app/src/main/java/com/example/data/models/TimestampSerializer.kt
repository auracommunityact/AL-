package com.example.data.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimestampSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Timestamp", PrimitiveKind.STRING)

    private val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun deserialize(decoder: Decoder): Long {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            return if (element.jsonPrimitive.isString) {
                try {
                    df.parse(element.jsonPrimitive.content)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            } else {
                element.jsonPrimitive.longOrNull ?: 0L
            }
        }
        
        return try {
            val isoString = decoder.decodeString()
            df.parse(isoString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    override fun serialize(encoder: Encoder, value: Long) {
        val isoString = df.format(Date(value))
        encoder.encodeString(isoString)
    }
}
