package dev.bluehouse.enablevolte

import android.os.BaseBundle
import android.os.PersistableBundle
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Changes(
    @Serializable(with = PersistableBundleSerializer::class)
    val bundle: PersistableBundle
)

object PersistableBundleSerializer : KSerializer<PersistableBundle> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PersistableBundle")

    override fun serialize(encoder: Encoder, value: PersistableBundle) {
        val json = encodeBundle(value)
        encoder.encodeSerializableValue(JsonObject.serializer(), json)
    }

    override fun deserialize(decoder: Decoder): PersistableBundle {
        val json = decoder.decodeSerializableValue(JsonObject.serializer())
        return decodeBundle(json)
    }

    private fun encodeBundle(bundle: BaseBundle): JsonObject {
        val map = mutableMapOf<String, JsonElement>()
        for (key in bundle.keySet()) {
            map[key] = when (val value = bundle.get(key)) {
                is Boolean -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                is Double -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is BooleanArray -> JsonArray(value.map { JsonPrimitive(it) })
                is IntArray -> JsonArray(value.map { JsonPrimitive(it) })
                is DoubleArray -> JsonArray(value.map { JsonPrimitive(it) })
                is Array<*> -> JsonArray(value.map { if (it is String) JsonPrimitive(it) else JsonNull })
                is BaseBundle -> encodeBundle(value)
                else -> JsonNull
            }
        }
        return JsonObject(map)
    }

    private fun decodeBundle(json: JsonObject): PersistableBundle {
        val bundle = PersistableBundle()
        for ((key, element) in json) {
            when (element) {
                is JsonPrimitive -> {
                    if (element.isString) {
                        bundle.putString(key, element.content)
                    } else if (element.jsonPrimitive.booleanOrNull != null) {
                        bundle.putBoolean(key, element.jsonPrimitive.booleanOrNull!!)
                    } else if (element.jsonPrimitive.intOrNull != null) {
                        bundle.putInt(key, element.jsonPrimitive.intOrNull!!)
                    } else if (element.jsonPrimitive.doubleOrNull != null) {
                        bundle.putDouble(key, element.jsonPrimitive.doubleOrNull!!)
                    }
                }
                is JsonArray -> {
                    val first = element.jsonArray.firstOrNull()?.jsonPrimitive
                    if (first != null) {
                        if (first.isString) {
                            bundle.putStringArray(key, element.map { it.jsonPrimitive.content }.toTypedArray())
                        } else if (first.booleanOrNull != null) {
                            bundle.putBooleanArray(key, element.map { it.jsonPrimitive.booleanOrNull!! }.toBooleanArray())
                        } else if (first.intOrNull != null) {
                            bundle.putIntArray(key, element.map { it.jsonPrimitive.intOrNull!! }.toIntArray())
                        } else if (first.doubleOrNull != null) {
                            bundle.putDoubleArray(key, element.map { it.jsonPrimitive.doubleOrNull!! }.toDoubleArray())
                        }
                    }
                }
                is JsonObject -> bundle.putPersistableBundle(key, decodeBundle(element))
                else -> {}
            }
        }
        return bundle
    }
}