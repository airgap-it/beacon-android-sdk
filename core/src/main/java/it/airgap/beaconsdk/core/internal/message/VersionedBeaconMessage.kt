package it.airgap.beaconsdk.core.internal.message

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.compat.Compat
import it.airgap.beaconsdk.core.internal.compat.VersionedCompat
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v3.V3BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.blockchainRegistry
import it.airgap.beaconsdk.core.internal.utils.compat
import it.airgap.beaconsdk.core.internal.utils.getString
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class VersionedBeaconMessage {
    public abstract val version: String

    public abstract suspend fun toBeaconMessage(origin: Connection.Id, destination: Connection.Id, beaconScope: BeaconScope): BeaconMessage

    public companion object {
        public fun from(senderId: String, message: BeaconMessage, context: Context): VersionedBeaconMessage {
            return when (message.version.major) {
                "1" -> V1BeaconMessage.from(senderId, message, context.toV1())
                "2" -> V2BeaconMessage.from(senderId, message, context.toV2())
                "3" -> V3BeaconMessage.from(senderId, message, context.toV3())

                // fallback to the newest version
                else -> V3BeaconMessage.from(senderId, message, context.toV3())
            }
        }

        private val String.major: String
            get() = substringBefore('.')

        public fun serializer(blockchainRegistry: BlockchainRegistry, compat: Compat<VersionedCompat>): KSerializer<VersionedBeaconMessage> =
            Serializer(blockchainRegistry, compat)

        public fun serializer(beaconScope: BeaconScope? = null): KSerializer<VersionedBeaconMessage> =
            Serializer(beaconScope)
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal class Serializer(private val blockchainRegistry: BlockchainRegistry, private val compat: Compat<VersionedCompat>) : KJsonSerializer<VersionedBeaconMessage> {
        constructor(beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope), compat(beaconScope))

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("VersionedBeaconMessage") {
            element<String>("version")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): VersionedBeaconMessage {
            val version = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when (version.major) {
                "1" -> jsonDecoder.json.decodeFromJsonElement(V1BeaconMessage.serializer(blockchainRegistry, compat), jsonElement)
                "2" -> jsonDecoder.json.decodeFromJsonElement(V2BeaconMessage.serializer(blockchainRegistry, compat), jsonElement)
                "3" -> jsonDecoder.json.decodeFromJsonElement(V3BeaconMessage.serializer(), jsonElement)

                // fallback to the newest version
                else -> jsonDecoder.json.decodeFromJsonElement(V3BeaconMessage.serializer(), jsonElement)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: VersionedBeaconMessage) {
            when (value) {
                is V1BeaconMessage -> jsonEncoder.encodeSerializableValue(V1BeaconMessage.serializer(blockchainRegistry, compat), value)
                is V2BeaconMessage -> jsonEncoder.encodeSerializableValue(V2BeaconMessage.serializer(blockchainRegistry, compat), value)
                is V3BeaconMessage -> jsonEncoder.encodeSerializableValue(V3BeaconMessage.serializer(), value)
            }
        }
    }

    public class Context(public val blockchainRegistry: BlockchainRegistry, public val compat: Compat<VersionedCompat>) {
        public fun toV1(): V1BeaconMessage.Context = V1BeaconMessage.Context(compat)
        public fun toV2(): V2BeaconMessage.Context = V2BeaconMessage.Context(compat)
        public fun toV3(): V3BeaconMessage.Context = V3BeaconMessage.Context(blockchainRegistry)
    }
}