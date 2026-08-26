package com.example.data.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray

object SafeStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SafeString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        return try {
            if (decoder is JsonDecoder) {
                val element = decoder.decodeJsonElement()
                if (element is JsonPrimitive) {
                    if (element.isString) element.content else element.toString()
                } else {
                    ""
                }
            } else {
                decoder.decodeString()
            }
        } catch (e: Exception) {
            ""
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

object SafeIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SafeInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        return try {
            if (decoder is JsonDecoder) {
                val element = decoder.decodeJsonElement()
                if (element is JsonPrimitive) {
                    element.content.toIntOrNull() ?: 0
                } else {
                    0
                }
            } else {
                decoder.decodeInt()
            }
        } catch (e: Exception) {
            0
        }
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }
}

object SafeLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SafeLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        return try {
            if (decoder is JsonDecoder) {
                val element = decoder.decodeJsonElement()
                if (element is JsonPrimitive) {
                    element.content.toLongOrNull() ?: 0L
                } else {
                    0L
                }
            } else {
                decoder.decodeLong()
            }
        } catch (e: Exception) {
            0L
        }
    }

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }
}

object SafeStringListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SafeStringList")

    override fun deserialize(decoder: Decoder): List<String> {
        return try {
            if (decoder is JsonDecoder) {
                val element = decoder.decodeJsonElement()
                if (element is JsonArray) {
                    element.map { if (it is JsonPrimitive) it.content else it.toString() }
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            jsonEncoder.encodeJsonElement(JsonArray(value.map { JsonPrimitive(it) }))
        } else {
            encoder.encodeString(value.joinToString(","))
        }
    }
}
