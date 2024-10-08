package it.airgap.beaconsdk.core.data

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.blockchainRegistry
import it.airgap.beaconsdk.core.internal.utils.getString
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
 * Base for networks supported in Beacon.
 *
 * @property [blockchainIdentifier] A unique name of the blockchain which the network applies to.
 * @property [name] An optional name of the network.
 * @property [rpcUrl] An optional URL for the network RPC interface.
 */
public abstract class Network {
    public abstract val blockchainIdentifier: String
    public abstract val name: String?
    public abstract val rpcUrl: String?

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract val identifier: String

    public companion object {
        public fun serializer(blockchainRegistry: BlockchainRegistry): KSerializer<Network> = Serializer(blockchainRegistry)
        public fun serializer(beaconScope: BeaconScope? = null): KSerializer<Network> = Serializer(beaconScope)
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal class Serializer(private val blockchainRegistry: BlockchainRegistry) : KJsonSerializer<Network> {
        constructor(beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope))

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Network") {
            element<String>("blockchainIdentifier")
            element<String>("name", isOptional = true)
            element<String>("rpcUrl", isOptional = true)
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): Network {
            val blockchainIdentifier = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val blockchain = blockchainRegistry.get(blockchainIdentifier)

            return jsonDecoder.json.decodeFromJsonElement(blockchain.serializer.data.network, jsonElement)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: Network) {
            val blockchain = blockchainRegistry.get(value.blockchainIdentifier)
            jsonEncoder.encodeSerializableValue(blockchain.serializer.data.network, value)
        }
    }
}