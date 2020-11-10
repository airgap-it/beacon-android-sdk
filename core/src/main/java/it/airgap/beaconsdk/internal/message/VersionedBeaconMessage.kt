package it.airgap.beaconsdk.internal.message

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Origin
import it.airgap.beaconsdk.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedExtendedStorage
import it.airgap.beaconsdk.internal.utils.failWithExpectedJsonDecoder
import it.airgap.beaconsdk.internal.utils.failWithMissingField
import it.airgap.beaconsdk.message.BeaconMessage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = VersionedBeaconMessage.Serializer::class)
internal abstract class VersionedBeaconMessage {
    abstract val version: String

    abstract suspend fun toBeaconMessage(origin: Origin, storage: DecoratedExtendedStorage): BeaconMessage

    abstract fun pairsWith(other: BeaconMessage): Boolean
    abstract fun pairsWith(other: VersionedBeaconMessage): Boolean

    abstract fun comesFrom(appMetadata: AppMetadata): Boolean

    companion object {
        fun fromBeaconMessage(
            version: String,
            senderId: String,
            message: BeaconMessage,
        ): VersionedBeaconMessage {
            return when (version.major) {
                "1" -> V1BeaconMessage.fromBeaconMessage(version, senderId, message)
                "2" -> V2BeaconMessage.fromBeaconMessage(version, senderId, message)

                // fallback to the newest version
                else -> V2BeaconMessage.fromBeaconMessage(version, senderId, message)
            }
        }

        private val String.major: String
            get() = substringBefore('.')
    }

    class Serializer : KSerializer<VersionedBeaconMessage> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("BeaconMessage", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): VersionedBeaconMessage {
            val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
            val jsonElement = jsonDecoder.decodeJsonElement()

            val versionField = "version"
            val version = jsonElement.jsonObject[versionField]?.jsonPrimitive?.content
                ?: failWithMissingField(versionField)

            return when (version.major) {
                "1" -> jsonDecoder.json.decodeFromJsonElement(V1BeaconMessage.serializer(), jsonElement)
                "2" -> jsonDecoder.json.decodeFromJsonElement(V2BeaconMessage.serializer(), jsonElement)

                // fallback to the newest version
                else -> jsonDecoder.json.decodeFromJsonElement(V2BeaconMessage.serializer(), jsonElement)
            }
        }

        override fun serialize(encoder: Encoder, value: VersionedBeaconMessage) {
            when (value) {
                is V1BeaconMessage -> encoder.encodeSerializableValue(V1BeaconMessage.serializer(), value)
                is V2BeaconMessage -> encoder.encodeSerializableValue(V2BeaconMessage.serializer(), value)
            }
        }
    }
}