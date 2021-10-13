package it.airgap.beaconsdk.core.data

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.blockchainRegistry
import it.airgap.beaconsdk.core.internal.utils.failWithBlockchainNotFound
import it.airgap.beaconsdk.core.internal.utils.getStringOrNull
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse
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
 * Base class for granted permission data.
 *
 * @property [blockchainIdentifier] The unique name of the blockchain on which the permission is valid.
 * @property [accountIdentifier] The value that identifies the account which granted the permissions.
 * @property [address] The address of the account derived from its public key.
 * @property [senderId] The value that identifies the sender to whom the permissions were granted.
 * @property [appMetadata] The metadata describing the dApp to which the permissions were granted.
 * @property [publicKey] The public key of the account.
 * @property [connectedAt] The timestamp at which the permissions were granted.
 * @property [threshold] An optional threshold configuration.
 */
@Serializable(with = Permission.Serializer::class)
public abstract class Permission {
    public abstract val blockchainIdentifier: String
    public abstract val accountIdentifier: String
    public abstract val address: String
    public abstract val senderId: String
    public abstract val appMetadata: AppMetadata
    public abstract val publicKey: String
    public abstract val connectedAt: Long
    public abstract val threshold: Threshold?

    @OptIn(ExperimentalSerializationApi::class)
    internal object Serializer : KJsonSerializer<Permission> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Permission") {
            element<String>("blockchainIdentifier")
            element<String>("accountIdentifier")
            element<String>("address")
            element<String>("senderId")
            element<AppMetadata>("appMetadata")
            element<String>("publicKey")
            element<Long>("connectedAt")
            element<Threshold?>("threshold", isOptional = true)
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): Permission {
            val blockchainIdentifier = jsonElement.jsonObject.getStringOrNull(descriptor.getElementName(0))
            val blockchain = blockchainIdentifier?.let {
                blockchainRegistry.get(blockchainIdentifier) ?: failWithBlockchainNotFound(blockchainIdentifier)
            } ?: CoreCompat.versioned.blockchain

            return jsonDecoder.json.decodeFromJsonElement(blockchain.serializer.permission, jsonElement)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: Permission) {
            val blockchain = blockchainRegistry.get(value.blockchainIdentifier) ?: failWithBlockchainNotFound(value.blockchainIdentifier)
            jsonEncoder.encodeSerializableValue(blockchain.serializer.permission, value)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface Creator {
        public fun fromPermissionResponse(appMetadata: AppMetadata, response: PermissionBeaconResponse): Permission
    }
}