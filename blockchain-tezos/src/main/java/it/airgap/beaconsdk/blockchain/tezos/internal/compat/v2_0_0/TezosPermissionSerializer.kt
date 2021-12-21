package it.airgap.beaconsdk.blockchain.tezos.internal.compat.v2_0_0

import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.internal.utils.failWithMissingField
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*

@OptIn(ExperimentalSerializationApi::class)
internal object TezosPermissionSerializer : KSerializer<TezosPermission> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TezosPermission") {
        element<String>("blockchainIdentifier")
        element<String>("accountIdentifier")
        element<String>("address")
        element<String>("senderId")
        element<AppMetadata>("appMetadata")
        element<String>("publicKey")
        element<Long>("connectedAt")
        element<TezosNetwork>("network")
        element<List<TezosPermission.Scope>>("scopes")
    }

    override fun deserialize(decoder: Decoder): TezosPermission =
        decoder.decodeStructure(descriptor) {
            var accountIdentifierOrNull: String? = null
            var addressOrNull: String? = null
            var senderIdOrNull: String? = null
            var appMetadataOrNull: AppMetadata? = null
            var publicKeyOrNull: String? = null
            var connectedAtOrNull: Long? = null
            var networkOrNull: TezosNetwork? = null
            var scopesOrNull: List<TezosPermission.Scope>? = null

            while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    1 -> accountIdentifierOrNull = decodeStringElement(descriptor, i)
                    2 -> addressOrNull = decodeStringElement(descriptor, i)
                    3 -> senderIdOrNull = decodeStringElement(descriptor, i)
                    4 -> appMetadataOrNull = decodeSerializableElement(descriptor, i, AppMetadata.serializer())
                    5 -> publicKeyOrNull = decodeStringElement(descriptor, i)
                    6 -> connectedAtOrNull = decodeLongElement(descriptor, i)
                    7 -> networkOrNull = decodeSerializableElement(descriptor, i, TezosNetwork.serializer())
                    8 -> scopesOrNull = decodeSerializableElement(descriptor, i, ListSerializer(TezosPermission.Scope.serializer()))
                }
            }

            val accountIdentifier = accountIdentifierOrNull ?: failWithMissingField(descriptor.getElementName(1))
            val address = addressOrNull ?: failWithMissingField(descriptor.getElementName(2))
            val senderId = senderIdOrNull ?: failWithMissingField(descriptor.getElementName(3))
            val appMetadata = appMetadataOrNull ?: failWithMissingField(descriptor.getElementName(4))
            val publicKey = publicKeyOrNull ?: failWithMissingField(descriptor.getElementName(5))
            val connectedAt = connectedAtOrNull ?: failWithMissingField(descriptor.getElementName(6))
            val network = networkOrNull ?: failWithMissingField(descriptor.getElementName(7))
            val scopes = scopesOrNull ?: failWithMissingField(descriptor.getElementName(8))

            TezosPermission(accountIdentifier, address, senderId, appMetadata, publicKey, connectedAt, network, scopes)
        }

    override fun serialize(encoder: Encoder, value: TezosPermission) {
        encoder.encodeStructure(descriptor) {
            with(value) {
                encodeStringElement(descriptor, 0, value.blockchainIdentifier)
                encodeStringElement(descriptor, 1, value.accountId)
                encodeStringElement(descriptor, 2, value.address)
                encodeStringElement(descriptor, 3, value.senderId)
                encodeSerializableElement(descriptor, 4, AppMetadata.serializer(), value.appMetadata)
                encodeStringElement(descriptor, 5, publicKey)
                encodeLongElement(descriptor, 6, connectedAt)
                encodeSerializableElement(descriptor, 7, TezosNetwork.serializer(), value.network)
                encodeSerializableElement(descriptor, 8, ListSerializer(TezosPermission.Scope.serializer()), value.scopes)
            }
        }
    }
}