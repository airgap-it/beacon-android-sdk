package it.airgap.beaconsdk.core.data

import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.blockchainRegistry
import it.airgap.beaconsdk.core.internal.utils.getStringOrNull
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
 * Metadata describing a dApp.
 *
 * @property [blockchainIdentifier] The unique name of the blockchain on which the dApp operates.
 * @property [senderId] The value that identifies the dApp.
 * @property [name] The name of the dApp.
 * @property [icon] An optional URL for the dApp icon.
 */
@Serializable(with = AppMetadata.Serializer::class)
public abstract class AppMetadata {
    public abstract val blockchainIdentifier: String
    public abstract val senderId: String
    public abstract val name: String
    public abstract val icon: String?

    public companion object {}

    @OptIn(ExperimentalSerializationApi::class)
    internal object Serializer : KJsonSerializer<AppMetadata> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AppMetadata") {
            element<String>("blockchainIdentifier")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): AppMetadata {
            val blockchainIdentifier = jsonElement.jsonObject.getStringOrNull(descriptor.getElementName(0))
            val blockchain = blockchainIdentifier?.let {
                blockchainRegistry.get(blockchainIdentifier)
            } ?: CoreCompat.versioned.blockchain

            return jsonDecoder.json.decodeFromJsonElement(blockchain.serializer.data.appMetadata, jsonElement)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: AppMetadata) {
            val blockchain = blockchainRegistry.get(value.blockchainIdentifier)
            jsonEncoder.encodeSerializableValue(blockchain.serializer.data.appMetadata, value)
        }
    }
}