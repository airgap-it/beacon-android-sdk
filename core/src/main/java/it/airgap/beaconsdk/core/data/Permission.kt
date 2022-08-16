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
 * Base class for granted permission data.
 *
 * @property [blockchainIdentifier] The unique name of the blockchain on which the permission is valid.
 * @property [accountId] The value that identifies the account which granted the permissions.
 * @property [senderId] The value that identifies the sender to whom the permissions were granted.
 * @property [connectedAt] The timestamp at which the permissions were granted.
 */
public abstract class Permission {
    public abstract val blockchainIdentifier: String
    public abstract val accountId: String
    public abstract val senderId: String
    public abstract val connectedAt: Long

    public companion object {
        public fun serializer(blockchainRegistry: BlockchainRegistry, compat: Compat<VersionedCompat>): KSerializer<Permission> =
            Serializer(blockchainRegistry, compat)

        public fun serializer(beaconScope: BeaconScope? = null): KSerializer<Permission> = Serializer(beaconScope)
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal class Serializer(private val blockchainRegistry: BlockchainRegistry, private val compat: Compat<VersionedCompat>) : KJsonSerializer<Permission> {
        constructor(beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope), compat(beaconScope))

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Permission") {
            element<String>("blockchainIdentifier")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): Permission {
            val blockchainIdentifier = jsonElement.jsonObject.getStringOrNull(descriptor.getElementName(0))
            val blockchain = blockchainIdentifier?.let {
                blockchainRegistry.get(blockchainIdentifier)
            } ?: compat.versioned.blockchain

            return jsonDecoder.json.decodeFromJsonElement(blockchain.serializer.data.permission, jsonElement)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: Permission) {
            val blockchain = blockchainRegistry.get(value.blockchainIdentifier)
            jsonEncoder.encodeSerializableValue(blockchain.serializer.data.permission, value)
        }
    }
}