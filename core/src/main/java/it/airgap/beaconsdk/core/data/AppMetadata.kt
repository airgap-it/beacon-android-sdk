package it.airgap.beaconsdk.core.data

import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.compat.Compat
import it.airgap.beaconsdk.core.internal.compat.VersionedCompat
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.blockchainRegistry
import it.airgap.beaconsdk.core.internal.utils.compat
import it.airgap.beaconsdk.core.internal.utils.getStringOrNull
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject

/**
 * Metadata describing a dApp.
 *
 * @property [blockchainIdentifier] The unique name of the blockchain on which the dApp operates.
 * @property [senderId] The value that identifies the dApp.
 * @property [name] The name of the dApp.
 * @property [icon] An optional URL for the dApp icon.
 */
public abstract class AppMetadata {
    public abstract val blockchainIdentifier: String
    public abstract val senderId: String
    public abstract val name: String
    public abstract val icon: String?

    public companion object {
        public fun serializer(blockchainRegistry: BlockchainRegistry, compat: Compat<VersionedCompat>): KSerializer<AppMetadata> =
            Serializer(blockchainRegistry, compat)

        public fun serializer(beaconScope: BeaconScope? = null): KSerializer<AppMetadata> = Serializer(beaconScope)
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal class Serializer(private val blockchainRegistry: BlockchainRegistry, private val compat: Compat<VersionedCompat>) : KJsonSerializer<AppMetadata> {
        constructor(beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope), compat(beaconScope))

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AppMetadata") {
            element<String>("blockchainIdentifier")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): AppMetadata {
            val blockchainIdentifier = jsonElement.jsonObject.getStringOrNull(descriptor.getElementName(0))
            val blockchain = blockchainIdentifier?.let {
                blockchainRegistry.get(blockchainIdentifier)
            } ?: compat.versioned.blockchain

            return jsonDecoder.json.decodeFromJsonElement(blockchain.serializer.data.appMetadata, jsonElement)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: AppMetadata) {
            val blockchain = blockchainRegistry.get(value.blockchainIdentifier)
            jsonEncoder.encodeSerializableValue(blockchain.serializer.data.appMetadata, value)
        }
    }
}