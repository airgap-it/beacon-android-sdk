package it.airgap.beaconsdk.core.data

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.blockchainRegistry
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
 * @property [accountId] The value that identifies the account which granted the permissions.
 * @property [senderId] The value that identifies the sender to whom the permissions were granted.
 * @property [connectedAt] The timestamp at which the permissions were granted.
 */
@Serializable(with = Permission.Serializer::class)
public abstract class Permission {
    public abstract val blockchainIdentifier: String
    public abstract val accountId: String
    public abstract val senderId: String
    public abstract val connectedAt: Long

    public companion object {}

    @OptIn(ExperimentalSerializationApi::class)
    internal object Serializer : KJsonSerializer<Permission> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Permission") {
            element<String>("blockchainIdentifier")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): Permission {
            val blockchainIdentifier = jsonElement.jsonObject.getStringOrNull(descriptor.getElementName(0))
            val blockchain = blockchainIdentifier?.let {
                blockchainRegistry.get(blockchainIdentifier)
            } ?: CoreCompat.versioned.blockchain

            return jsonDecoder.json.decodeFromJsonElement(blockchain.serializer.data.permission, jsonElement)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: Permission) {
            val blockchain = blockchainRegistry.get(value.blockchainIdentifier)
            jsonEncoder.encodeSerializableValue(blockchain.serializer.data.permission, value)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface Creator {
        public fun fromPermissionResponse(response: PermissionBeaconResponse): Permission
    }
}