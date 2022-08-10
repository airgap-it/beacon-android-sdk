package it.airgap.beaconsdk.core.internal.message.v3

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.blockchainRegistry
import it.airgap.beaconsdk.core.message.*
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonClassDiscriminator
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
        origin: Connection.Id,
        destination: Connection.Id,
        beaconScope: BeaconScope,
    ): BeaconMessage = message.toBeaconMessage(beaconScope, id, version, senderId, origin, destination)

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @JsonClassDiscriminator(Content.CLASS_DISCRIMINATOR)
    public sealed class Content {
        public abstract suspend fun toBeaconMessage(
            beaconScope: BeaconScope,
            id: String,
            version: String,
            senderId: String,
            origin: Connection.Id,
            destination: Connection.Id,
        ): BeaconMessage

        public companion object {
            internal const val CLASS_DISCRIMINATOR = "type"
        }
    }

    public companion object {
        public fun from(senderId: String, message: BeaconMessage, context: Context): V3BeaconMessage = with(message) {
            val content = when (this) {
                is AcknowledgeBeaconResponse -> AcknowledgeV3BeaconResponseContent
                is ErrorBeaconResponse -> ErrorV3BeaconResponseContent(errorType, description)
                is DisconnectBeaconMessage -> DisconnectV3BeaconMessageContent
                is PermissionBeaconRequest -> context.blockchainRegistry.get(blockchainIdentifier).creator.v3.contentFrom(message).getOrThrow()
                is BlockchainBeaconRequest -> context.blockchainRegistry.get(blockchainIdentifier).creator.v3.contentFrom(message).getOrThrow()
                is PermissionBeaconResponse -> context.blockchainRegistry.get(blockchainIdentifier).creator.v3.contentFrom(message).getOrThrow()
                is BlockchainBeaconResponse -> context.blockchainRegistry.get(blockchainIdentifier).creator.v3.contentFrom(message).getOrThrow()
            }

            V3BeaconMessage(id, version, senderId, content)
        }
    }

    public class Context(public val blockchainRegistry: BlockchainRegistry)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class PermissionV3BeaconRequestContent(
    public val blockchainIdentifier: String,
    public val blockchainData: BlockchainData,
) : V3BeaconMessage.Content() {

    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
    ): BeaconMessage = blockchainData.toBeaconMessage(beaconScope, id, version, senderId, origin, destination, blockchainIdentifier)

    @Serializable
    public abstract class BlockchainData {
        public abstract suspend fun toBeaconMessage(
            beaconScope: BeaconScope,
            id: String,
            version: String,
            senderId: String,
            origin: Connection.Id,
            destination: Connection.Id,
            blockchainIdentifier: String,
        ): BeaconMessage

        public companion object {
            public fun serializer(blockchainRegistry: BlockchainRegistry, blockchainIdentifier: String): KSerializer<BlockchainData> =
                Serializer(blockchainRegistry, blockchainIdentifier)

            public fun serializer(blockchainIdentifier: String, beaconScope: BeaconScope? = null): KSerializer<BlockchainData> =
                Serializer(blockchainIdentifier, beaconScope)
        }

        public data class Serializer(
            private val blockchainRegistry: BlockchainRegistry,
            private val blockchainIdentifier: String,
        ) : KSerializer<BlockchainData> by blockchainRegistry.get(blockchainIdentifier).serializer.v3.permissionRequestData {
            public constructor(blockchainIdentifier: String, beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope), blockchainIdentifier)
        }
    }

    public companion object {
        internal const val TYPE: String = "permission_request"

        public fun serializer(blockchainRegistry: BlockchainRegistry): KSerializer<PermissionV3BeaconRequestContent> =
            Serializer(blockchainRegistry)

        public fun serializer(beaconScope: BeaconScope? = null): KSerializer<PermissionV3BeaconRequestContent> =
            Serializer(beaconScope)
    }

    internal class Serializer(private val blockchainRegistry: BlockchainRegistry) : KJsonSerializer<PermissionV3BeaconRequestContent> {
        constructor(beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope))

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(TYPE) {
            element<String>("blockchainIdentifier")
            element<BlockchainData>("blockchainData")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): PermissionV3BeaconRequestContent =
            jsonDecoder.decodeStructure(descriptor) {
                val blockchainIdentifier = decodeStringElement(descriptor, 0)
                val blockchainData = decodeSerializableElement(descriptor, 1, BlockchainData.serializer(blockchainRegistry, blockchainIdentifier))

                PermissionV3BeaconRequestContent(blockchainIdentifier, blockchainData)
            }

        override fun serialize(jsonEncoder: JsonEncoder, value: PermissionV3BeaconRequestContent) =
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, blockchainIdentifier)
                    encodeSerializableElement(descriptor, 1, BlockchainData.serializer(blockchainRegistry, blockchainIdentifier), blockchainData)
                }
            }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class BlockchainV3BeaconRequestContent(
    public val blockchainIdentifier: String,
    public val accountId: String,
    public val blockchainData: BlockchainData,
) : V3BeaconMessage.Content() {

    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
    ): BeaconMessage = blockchainData.toBeaconMessage(beaconScope, id, version, senderId, origin, destination, accountId, blockchainIdentifier)

    @Serializable
    public abstract class BlockchainData {
        public abstract suspend fun toBeaconMessage(
            beaconScope: BeaconScope,
            id: String,
            version: String,
            senderId: String,
            origin: Connection.Id,
            destination: Connection.Id,
            accountId: String,
            blockchainIdentifier: String,
        ): BeaconMessage

        public companion object {
            public fun serializer(blockchainRegistry: BlockchainRegistry, blockchainIdentifier: String): KSerializer<BlockchainData> =
                Serializer(blockchainRegistry, blockchainIdentifier)

            public fun serializer(blockchainIdentifier: String, beaconScope: BeaconScope): KSerializer<BlockchainData> =
                Serializer(blockchainIdentifier, beaconScope)
        }

        internal data class Serializer(
            private val blockchainRegistry: BlockchainRegistry,
            private val blockchainIdentifier: String,
        ) : KSerializer<BlockchainData> by blockchainRegistry.get(blockchainIdentifier).serializer.v3.blockchainRequestData {
            constructor(blockchainIdentifier: String, beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope), blockchainIdentifier)
        }
    }

    public companion object {
        internal const val TYPE: String = "blockchain_request"

        public fun serializer(blockchainRegistry: BlockchainRegistry): KSerializer<BlockchainV3BeaconRequestContent> =
            Serializer(blockchainRegistry)

        public fun serializer(beaconScope: BeaconScope? = null): KSerializer<BlockchainV3BeaconRequestContent> =
            Serializer(beaconScope)
    }

    internal class Serializer(private val blockchainRegistry: BlockchainRegistry) : KJsonSerializer<BlockchainV3BeaconRequestContent> {
        constructor(beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope))

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(TYPE) {
            element<String>("blockchainIdentifier")
            element<String>("accountId")
            element<BlockchainData>("blockchainData")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): BlockchainV3BeaconRequestContent =
            jsonDecoder.decodeStructure(descriptor) {
                val blockchainIdentifier = decodeStringElement(descriptor, 0)
                val accountId = decodeStringElement(descriptor, 1)
                val blockchainData = decodeSerializableElement(descriptor, 2, BlockchainData.serializer(blockchainRegistry, blockchainIdentifier))

                BlockchainV3BeaconRequestContent(blockchainIdentifier, accountId, blockchainData)
            }

        override fun serialize(jsonEncoder: JsonEncoder, value: BlockchainV3BeaconRequestContent) =
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, blockchainIdentifier)
                    encodeStringElement(descriptor, 1, accountId)
                    encodeSerializableElement(descriptor, 2, BlockchainData.serializer(blockchainRegistry, blockchainIdentifier), blockchainData)
                }
            }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class PermissionV3BeaconResponseContent(
    public val blockchainIdentifier: String,
    public val blockchainData: BlockchainData,
) : V3BeaconMessage.Content() {

    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
    ): BeaconMessage = blockchainData.toBeaconMessage(beaconScope, id, version, senderId, origin, destination, blockchainIdentifier)

    @Serializable
    public abstract class BlockchainData {
        public abstract suspend fun toBeaconMessage(
            beaconScope: BeaconScope,
            id: String,
            version: String,
            senderId: String,
            origin: Connection.Id,
            destination: Connection.Id,
            blockchainIdentifier: String,
        ): BeaconMessage

        public companion object {
            public fun serializer(blockchainRegistry: BlockchainRegistry, blockchainIdentifier: String): KSerializer<BlockchainData> =
                Serializer(blockchainRegistry, blockchainIdentifier)

            public fun serializer(blockchainIdentifier: String, beaconScope: BeaconScope? = null): KSerializer<BlockchainData> =
                Serializer(blockchainIdentifier, beaconScope)
        }

        internal data class Serializer(
            private val blockchainRegistry: BlockchainRegistry,
            private val blockchainIdentifier: String,
        ) : KSerializer<BlockchainData> by blockchainRegistry.get(blockchainIdentifier).serializer.v3.permissionResponseData {
            constructor(blockchainIdentifier: String, beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope), blockchainIdentifier)
        }
    }

    public companion object {
        internal const val TYPE: String = "permission_response"

        public fun serializer(blockchainRegistry: BlockchainRegistry): KSerializer<PermissionV3BeaconResponseContent> =
            Serializer(blockchainRegistry)

        public fun serializer(beaconScope: BeaconScope? = null): KSerializer<PermissionV3BeaconResponseContent> =
            Serializer(beaconScope)
    }

    internal class Serializer(private val blockchainRegistry: BlockchainRegistry) : KJsonSerializer<PermissionV3BeaconResponseContent> {
        constructor(beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope))

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(TYPE) {
            element<String>("blockchainIdentifier")
            element<BlockchainData>("blockchainData")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): PermissionV3BeaconResponseContent =
            jsonDecoder.decodeStructure(descriptor) {
                val blockchainIdentifier = decodeStringElement(descriptor, 0)
                val blockchainData = decodeSerializableElement(descriptor, 1, BlockchainData.serializer(blockchainRegistry, blockchainIdentifier))

                PermissionV3BeaconResponseContent(blockchainIdentifier, blockchainData)
            }

        override fun serialize(jsonEncoder: JsonEncoder, value: PermissionV3BeaconResponseContent) =
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, blockchainIdentifier)
                    encodeSerializableElement(descriptor, 1, BlockchainData.serializer(blockchainRegistry, blockchainIdentifier), blockchainData)
                }
            }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class BlockchainV3BeaconResponseContent(
    public val blockchainIdentifier: String,
    public val blockchainData: BlockchainData,
) : V3BeaconMessage.Content() {

    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
    ): BeaconMessage = blockchainData.toBeaconMessage(beaconScope, id, version, senderId, origin, destination, blockchainIdentifier)

    @Serializable
    public abstract class BlockchainData {
        public abstract suspend fun toBeaconMessage(
            beaconScope: BeaconScope,
            id: String,
            version: String,
            senderId: String,
            origin: Connection.Id,
            destination: Connection.Id,
            blockchainIdentifier: String,
        ): BeaconMessage

        public companion object {
            public fun serializer(blockchainRegistry: BlockchainRegistry, blockchainIdentifier: String): KSerializer<BlockchainData> = Serializer(blockchainRegistry, blockchainIdentifier)
        }

        internal data class Serializer(
            private val blockchainRegistry: BlockchainRegistry,
            private val blockchainIdentifier: String,
        ) : KSerializer<BlockchainData> by blockchainRegistry.get(blockchainIdentifier).serializer.v3.blockchainResponseData
    }

    public companion object {
        internal const val TYPE: String = "blockchain_response"

        public fun serializer(blockchainRegistry: BlockchainRegistry): KSerializer<BlockchainV3BeaconResponseContent> =
            Serializer(blockchainRegistry)

        public fun serializer(beaconScope: BeaconScope? = null): KSerializer<BlockchainV3BeaconResponseContent> =
            Serializer(beaconScope)
    }

    internal class Serializer(private val blockchainRegistry: BlockchainRegistry) : KJsonSerializer<BlockchainV3BeaconResponseContent> {
        constructor(beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope))

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(TYPE) {
            element<String>("blockchainIdentifier")
            element<BlockchainData>("blockchainData")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): BlockchainV3BeaconResponseContent =
            jsonDecoder.decodeStructure(descriptor) {
                val blockchainIdentifier = decodeStringElement(descriptor, 0)
                val blockchainData = decodeSerializableElement(descriptor, 1, BlockchainData.serializer(blockchainRegistry, blockchainIdentifier))

                BlockchainV3BeaconResponseContent(blockchainIdentifier, blockchainData)
            }

        override fun serialize(jsonEncoder: JsonEncoder, value: BlockchainV3BeaconResponseContent) =
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    encodeStringElement(descriptor, 0, blockchainIdentifier)
                    encodeSerializableElement(descriptor, 1, BlockchainData.serializer(blockchainRegistry, blockchainIdentifier), blockchainData)
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
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
    ): BeaconMessage = AcknowledgeBeaconResponse(id, version, destination, senderId)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class ErrorV3BeaconResponseContent(
    val errorType: BeaconError,
    val description: String? = null,
) : V3BeaconMessage.Content() {
    val blockchainIdentifier: String? = errorType.blockchainIdentifier

    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
    ): BeaconMessage = ErrorBeaconResponse(id, version, destination, errorType, description)

    public companion object {
        internal const val TYPE: String = "error"

        public fun serializer(blockchainRegistry: BlockchainRegistry): KSerializer<ErrorV3BeaconResponseContent> =
            Serializer(blockchainRegistry)

        public fun serializer(beaconScope: BeaconScope? = null): KSerializer<ErrorV3BeaconResponseContent> =
            Serializer(beaconScope)
    }

    internal class Serializer(private val blockchainRegistry: BlockchainRegistry) : KJsonSerializer<ErrorV3BeaconResponseContent> {
        constructor(beaconScope: BeaconScope? = null) : this(blockchainRegistry(beaconScope))

        private fun beaconErrorSerializer(blockchainIdentifier: String? = null): KSerializer<BeaconError> =
            BeaconError.serializer(blockchainRegistry, blockchainIdentifier)

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(TYPE) {
            element<String>("blockchainIdentifier", isOptional = true)
            element("errorType", beaconErrorSerializer().descriptor)
            element<String>("description", isOptional = true)
        }

        override fun deserialize(
            jsonDecoder: JsonDecoder,
            jsonElement: JsonElement,
        ): ErrorV3BeaconResponseContent = jsonDecoder.decodeStructure(descriptor) {
            val blockchainIdentifier = runCatching { decodeStringElement(descriptor, 0) }.getOrNull()
            val errorType = decodeSerializableElement(descriptor, 1, beaconErrorSerializer(blockchainIdentifier))
            val description = runCatching { decodeStringElement(descriptor, 2) }.getOrNull()

            return ErrorV3BeaconResponseContent(errorType, description)
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: ErrorV3BeaconResponseContent) {
            jsonEncoder.encodeStructure(descriptor) {
                with(value) {
                    blockchainIdentifier?.let { encodeStringElement(descriptor, 0, it) }
                    encodeSerializableElement(descriptor, 1, beaconErrorSerializer(blockchainIdentifier), errorType)
                    description?.let { encodeStringElement(descriptor, 2, it) }
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
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
    ): BeaconMessage = DisconnectBeaconMessage(id, senderId, version, origin, destination)
}