package it.airgap.beaconsdk.core.internal.message.v3

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.blockchainRegistry
import it.airgap.beaconsdk.core.message.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
public data class V3BeaconMessage(
    public val id: String,
    override val version: String,
    public val senderId: String,
    public val message: Content,
) : VersionedBeaconMessage() {

    override suspend fun toBeaconMessage(
        origin: Origin,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = message.toBeaconMessage(id, version, senderId, origin, storageManager, identifierCreator)

    @Serializable
    public sealed class Content {
        public abstract suspend fun toBeaconMessage(
            id: String,
            version: String,
            senderId: String,
            origin: Origin,
            storageManager: StorageManager,
            identifierCreator: IdentifierCreator,
        ): BeaconMessage
    }

    public companion object {
        public fun from(senderId: String, message: BeaconMessage): V3BeaconMessage = with(message) {
            val content = when (this) {
                is AcknowledgeBeaconResponse -> AcknowledgeV3BeaconResponseContent
                is ErrorBeaconResponse -> ErrorV3BeaconResponseContent(errorType, blockchainIdentifier)
                is DisconnectBeaconMessage -> DisconnectV3BeaconMessageContent
                is PermissionBeaconRequest -> blockchainRegistry.get(blockchainIdentifier).creator.v3.contentFrom(senderId, message).getOrThrow()
                is BlockchainBeaconRequest -> blockchainRegistry.get(blockchainIdentifier).creator.v3.contentFrom(senderId, message).getOrThrow()
                is PermissionBeaconResponse -> blockchainRegistry.get(blockchainIdentifier).creator.v3.contentFrom(senderId, message).getOrThrow()
                is BlockchainBeaconResponse -> blockchainRegistry.get(blockchainIdentifier).creator.v3.contentFrom(senderId, message).getOrThrow()
            }

            V3BeaconMessage(id, version, senderId, content)
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable(with = PermissionV3BeaconRequestContent.Serializer::class)
@SerialName(PermissionV3BeaconRequestContent.TYPE)
public data class PermissionV3BeaconRequestContent(
    public val blockchainIdentifier: String,
    public val chainData: ChainData,
) : V3BeaconMessage.Content() {

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = chainData.toBeaconMessage(id, version, senderId, origin, storageManager, identifierCreator)

    @Serializable
    public abstract class ChainData {
        public abstract suspend fun toBeaconMessage(
            id: String,
            version: String,
            senderId: String,
            origin: Origin,
            storageManager: StorageManager,
            identifierCreator: IdentifierCreator,
        ): BeaconMessage

        public companion object {
            public fun serializer(blockchainIdentifier: String): KSerializer<ChainData> = Serializer(blockchainIdentifier)
        }

        internal data class Serializer(
            private val blockchainIdentifier: String,
        ) : KSerializer<ChainData> by blockchainRegistry.get(blockchainIdentifier).serializer.v3.permissionRequestData
    }

    public companion object {
        internal const val TYPE: String = "permission_request"
    }

    internal object Serializer : KJsonSerializer<PermissionV3BeaconRequestContent> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PermissionV3BeaconRequestContent") {
            element<String>("blockchainIdentifier")
            element<ChainData>("chainData")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): PermissionV3BeaconRequestContent =
            jsonDecoder.decodeStructure(descriptor) {
                val blockchainIdentifier = decodeStringElement(descriptor, 0)
                val chainData = decodeSerializableElement(descriptor, 1, ChainData.serializer(blockchainIdentifier))

                PermissionV3BeaconRequestContent(blockchainIdentifier, chainData)
            }

        override fun serialize(jsonEncoder: JsonEncoder, value: PermissionV3BeaconRequestContent) =
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, blockchainIdentifier)
                    encodeSerializableElement(descriptor, 1, ChainData.serializer(blockchainIdentifier), chainData)
                }
            }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable(with = BlockchainV3BeaconRequestContent.Serializer::class)
@SerialName(BlockchainV3BeaconRequestContent.TYPE)
public data class BlockchainV3BeaconRequestContent(
    public val blockchainIdentifier: String,
    public val accountId: String,
    public val chainData: ChainData,
) : V3BeaconMessage.Content() {

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = chainData.toBeaconMessage(id, version, senderId, origin, accountId, storageManager, identifierCreator)

    @Serializable
    public abstract class ChainData {
        public abstract suspend fun toBeaconMessage(
            id: String,
            version: String,
            senderId: String,
            origin: Origin,
            accountId: String,
            storageManager: StorageManager,
            identifierCreator: IdentifierCreator,
        ): BeaconMessage

        public companion object {
            public fun serializer(blockchainIdentifier: String): KSerializer<ChainData> = Serializer(blockchainIdentifier)
        }

        internal data class Serializer(
            private val blockchainIdentifier: String,
        ) : KSerializer<ChainData> by blockchainRegistry.get(blockchainIdentifier).serializer.v3.blockchainRequestData
    }

    public companion object {
        internal const val TYPE: String = "blockchain_request"
    }

    internal object Serializer : KJsonSerializer<BlockchainV3BeaconRequestContent> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BlockchainV3BeaconRequestContent") {
            element<String>("blockchainIdentifier")
            element<String>("accountId")
            element<ChainData>("chainData")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): BlockchainV3BeaconRequestContent =
            jsonDecoder.decodeStructure(descriptor) {
                val blockchainIdentifier = decodeStringElement(descriptor, 0)
                val accountId = decodeStringElement(descriptor, 1)
                val chainData = decodeSerializableElement(descriptor, 2, ChainData.serializer(blockchainIdentifier))

                BlockchainV3BeaconRequestContent(blockchainIdentifier, accountId, chainData)
            }

        override fun serialize(jsonEncoder: JsonEncoder, value: BlockchainV3BeaconRequestContent) =
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, blockchainIdentifier)
                    encodeStringElement(descriptor, 1, accountId)
                    encodeSerializableElement(descriptor, 2, ChainData.serializer(blockchainIdentifier), chainData)
                }
            }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable(with = PermissionV3BeaconResponseContent.Serializer::class)
@SerialName(PermissionV3BeaconResponseContent.TYPE)
public data class PermissionV3BeaconResponseContent(
    public val blockchainIdentifier: String,
    public val accountId: String,
    public val chainData: ChainData,
) : V3BeaconMessage.Content() {

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = chainData.toBeaconMessage(id, version, senderId, origin, accountId, storageManager, identifierCreator)

    @Serializable
    public abstract class ChainData {
        public abstract suspend fun toBeaconMessage(
            id: String,
            version: String,
            senderId: String,
            origin: Origin,
            accountId: String,
            storageManager: StorageManager,
            identifierCreator: IdentifierCreator,
        ): BeaconMessage

        public companion object {
            public fun serializer(blockchainIdentifier: String): KSerializer<ChainData> = Serializer(blockchainIdentifier)
        }

        internal data class Serializer(
            private val blockchainIdentifier: String,
        ) : KSerializer<ChainData> by blockchainRegistry.get(blockchainIdentifier).serializer.v3.permissionResponseData
    }

    public companion object {
        internal const val TYPE: String = "permission_response"
    }

    internal object Serializer : KJsonSerializer<PermissionV3BeaconResponseContent> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PermissionV3BeaconResponseContent") {
            element<String>("blockchainIdentifier")
            element<String>("accountId")
            element<ChainData>("chainData")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): PermissionV3BeaconResponseContent =
            jsonDecoder.decodeStructure(descriptor) {
                val blockchainIdentifier = decodeStringElement(descriptor, 0)
                val accountId = decodeStringElement(descriptor, 1)
                val chainData = decodeSerializableElement(descriptor, 2, ChainData.serializer(blockchainIdentifier))

                PermissionV3BeaconResponseContent(blockchainIdentifier, accountId, chainData)
            }

        override fun serialize(jsonEncoder: JsonEncoder, value: PermissionV3BeaconResponseContent) =
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, blockchainIdentifier)
                    encodeStringElement(descriptor, 1, accountId)
                    encodeSerializableElement(descriptor, 2, ChainData.serializer(blockchainIdentifier), chainData)
                }
            }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable(with = BlockchainV3BeaconResponseContent.Serializer::class)
@SerialName(BlockchainV3BeaconResponseContent.TYPE)
public data class BlockchainV3BeaconResponseContent(
    public val blockchainIdentifier: String,
    public val chainData: ChainData,
) : V3BeaconMessage.Content() {

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = chainData.toBeaconMessage(id, version, senderId, origin, storageManager, identifierCreator)

    @Serializable
    public abstract class ChainData {
        public abstract suspend fun toBeaconMessage(
            id: String,
            version: String,
            senderId: String,
            origin: Origin,
            storageManager: StorageManager,
            identifierCreator: IdentifierCreator,
        ): BeaconMessage

        public companion object {
            public fun serializer(blockchainIdentifier: String): KSerializer<ChainData> = Serializer(blockchainIdentifier)
        }

        internal data class Serializer(
            private val blockchainIdentifier: String,
        ) : KSerializer<ChainData> by blockchainRegistry.get(blockchainIdentifier).serializer.v3.blockchainResponseData
    }

    public companion object {
        internal const val TYPE: String = "blockchain_response"
    }

    internal object Serializer : KJsonSerializer<BlockchainV3BeaconResponseContent> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BlockchainV3BeaconResponseContent") {
            element<String>("blockchainIdentifier")
            element<ChainData>("chainData")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): BlockchainV3BeaconResponseContent =
            jsonDecoder.decodeStructure(descriptor) {
                val blockchainIdentifier = decodeStringElement(descriptor, 0)
                val chainData = decodeSerializableElement(descriptor, 1, ChainData.serializer(blockchainIdentifier))

                BlockchainV3BeaconResponseContent(blockchainIdentifier, chainData)
            }

        override fun serialize(jsonEncoder: JsonEncoder, value: BlockchainV3BeaconResponseContent) =
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, blockchainIdentifier)
                    encodeSerializableElement(descriptor, 1, ChainData.serializer(blockchainIdentifier), chainData)
                }
            }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@SerialName(AcknowledgeV3BeaconResponseContent.TYPE)
public object AcknowledgeV3BeaconResponseContent : V3BeaconMessage.Content() {
    internal const val TYPE: String = "acknowledge"

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = AcknowledgeBeaconResponse(id, version, origin, senderId)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@SerialName(ErrorV3BeaconResponseContent.TYPE)
public data class ErrorV3BeaconResponseContent(
    val errorType: BeaconError,
    val blockchainIdentifier: String? = null,
) : V3BeaconMessage.Content() {

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = ErrorBeaconResponse(id, version, origin, errorType, blockchainIdentifier)

    public companion object {
        internal const val TYPE: String = "error"
    }

    internal class Serializer : KJsonSerializer<ErrorV3BeaconResponseContent> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ErrorV3BeaconResponseContent") {
            element<String>("blockchainIdentifier", isOptional = true)
            element<BeaconError>("errorType")
        }

        override fun deserialize(
            jsonDecoder: JsonDecoder,
            jsonElement: JsonElement,
        ): ErrorV3BeaconResponseContent = jsonDecoder.decodeStructure(descriptor) {
            val blockchainIdentifier = runCatching { decodeStringElement(descriptor, 0) }.getOrNull()
            val errorType = decodeSerializableElement(descriptor, 1, BeaconError.serializer(blockchainIdentifier))

            return ErrorV3BeaconResponseContent(errorType, blockchainIdentifier)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: ErrorV3BeaconResponseContent) {
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    blockchainIdentifier?.let { encodeStringElement(descriptor, 0, it) }
                    encodeSerializableElement(descriptor, 1, BeaconError.serializer(blockchainIdentifier), errorType)
                }
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@SerialName(DisconnectV3BeaconMessageContent.TYPE)
public object DisconnectV3BeaconMessageContent : V3BeaconMessage.Content() {
    internal const val TYPE: String = "disconnect"

    override suspend fun toBeaconMessage(
        id: String,
        version: String,
        senderId: String,
        origin: Origin,
        storageManager: StorageManager,
        identifierCreator: IdentifierCreator,
    ): BeaconMessage = DisconnectBeaconMessage(id, senderId, version, origin)
}