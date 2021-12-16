package it.airgap.beaconsdk.core.data

import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.blockchainRegistry
import it.airgap.beaconsdk.core.internal.utils.failWithBlockchainNotFound
import it.airgap.beaconsdk.core.internal.utils.getString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
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
@Serializable(with = Network.Serializer::class)
public abstract class Network {
    public abstract val blockchainIdentifier: String
    public abstract val name: String?
    public abstract val rpcUrl: String?

    public abstract val identifier: String

    public companion object {}

    @OptIn(ExperimentalSerializationApi::class)
    internal object Serializer : KJsonSerializer<Network> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Network") {
            element<String>("blockchainIdentifier")
            element<String>("name", isOptional = true)
            element<String>("rpcUrl", isOptional = true)
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): Network {
            val blockchainIdentifier = jsonElement.jsonObject.getString(descriptor.getElementName(0))
            val blockchain = blockchainRegistry.get(blockchainIdentifier) ?: failWithBlockchainNotFound(blockchainIdentifier)

            return jsonDecoder.json.decodeFromJsonElement(blockchain.serializer.network, jsonElement)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: Network) {
            val blockchain = blockchainRegistry.get(value.blockchainIdentifier) ?: failWithBlockchainNotFound(value.blockchainIdentifier)
            jsonEncoder.encodeSerializableValue(blockchain.serializer.network, value)
        }
    }
}