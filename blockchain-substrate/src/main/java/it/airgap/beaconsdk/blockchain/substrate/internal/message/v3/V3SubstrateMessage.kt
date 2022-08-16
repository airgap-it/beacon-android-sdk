@file:OptIn(ExperimentalSerializationApi::class)

package it.airgap.beaconsdk.blockchain.substrate.internal.message.v3

import it.airgap.beaconsdk.blockchain.substrate.data.*
import it.airgap.beaconsdk.blockchain.substrate.message.request.BlockchainSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.request.PermissionSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.request.SignPayloadSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.request.TransferSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.response.BlockchainSubstrateResponse
import it.airgap.beaconsdk.blockchain.substrate.message.response.PermissionSubstrateResponse
import it.airgap.beaconsdk.blockchain.substrate.message.response.SignPayloadSubstrateResponse
import it.airgap.beaconsdk.blockchain.substrate.message.response.TransferSubstrateResponse
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.utils.KJsonSerializer
import it.airgap.beaconsdk.core.internal.utils.dependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.internal.utils.getString
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.findAppMetadata
import it.airgap.beaconsdk.core.storage.findPermission
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.json.*

@Serializable
internal data class PermissionV3SubstrateRequest(
    val appMetadata: SubstrateAppMetadata,
    val scopes: List<SubstratePermission.Scope>,
    val networks: List<SubstrateNetwork>?,
) : PermissionV3BeaconRequestContent.BlockchainData() {
    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        blockchainIdentifier: String,
    ): BeaconMessage = PermissionSubstrateRequest(id, version, blockchainIdentifier, senderId, origin, destination, appMetadata, scopes, networks ?: emptyList())

    companion object {
        fun from(permissionRequest: PermissionSubstrateRequest): PermissionV3SubstrateRequest = with(permissionRequest) {
            PermissionV3SubstrateRequest(
                appMetadata,
                scopes,
                networks.ifEmpty { null },
            )
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = BlockchainV3SubstrateRequest.Serializer::class)
@JsonClassDiscriminator(BlockchainV3SubstrateRequest.CLASS_DISCRIMINATOR)
internal sealed class BlockchainV3SubstrateRequest : BlockchainV3BeaconRequestContent.BlockchainData() {
    abstract val type: String
    abstract val scope: SubstratePermission.Scope

    companion object {
        const val CLASS_DISCRIMINATOR = "type"

        fun from(blockchainRequest: BlockchainSubstrateRequest): BlockchainV3SubstrateRequest = with(blockchainRequest) {
            when (this) {
                is TransferSubstrateRequest -> TransferV3SubstrateRequest(
                    scope,
                    amount,
                    recipient,
                    network,
                    TransferV3SubstrateRequest.Mode.from(this),
                )
                is SignPayloadSubstrateRequest -> SignPayloadV3SubstrateRequest(
                    scope,
                    SignPayloadV3SubstrateRequest.Mode.from(this),
                    payload,
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<BlockchainV3SubstrateRequest> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BlockchainV3SubstrateRequest") {
            element<String>("type")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): BlockchainV3SubstrateRequest {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when (type) {
                TransferV3SubstrateRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(TransferV3SubstrateRequest.serializer(), jsonElement)
                SignPayloadV3SubstrateRequest.TYPE -> jsonDecoder.json.decodeFromJsonElement(SignPayloadV3SubstrateRequest.serializer(), jsonElement)
                else -> failWithUnknownType(type)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: BlockchainV3SubstrateRequest) {
            when (value) {
                is TransferV3SubstrateRequest -> jsonEncoder.encodeSerializableValue(TransferV3SubstrateRequest.serializer(), value)
                is SignPayloadV3SubstrateRequest -> jsonEncoder.encodeSerializableValue(SignPayloadV3SubstrateRequest.serializer(), value)
            }
        }

        private fun failWithUnknownType(type: String): Nothing = failWithIllegalArgument("Unknown Substrate message type $type")
    }
}

@Serializable
@SerialName(TransferV3SubstrateRequest.TYPE)
internal data class TransferV3SubstrateRequest(
    override val scope: SubstratePermission.Scope,
    val amount: String,
    val recipient: String,
    val network: SubstrateNetwork,
    val mode: Mode
) : BlockchainV3SubstrateRequest() {
    @EncodeDefault
    override val type: String = TYPE
    
    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        accountId: String,
        blockchainIdentifier: String,
    ): BeaconMessage {
        val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata<SubstrateAppMetadata> { it.senderId == senderId }
        val account = dependencyRegistry(beaconScope).storageManager.findPermission<SubstratePermission> { it.accountId == accountId }?.account ?: failWithAccountNotFound(accountId)

        return mode.createTransferSubstrateRequest(
            id,
            version,
            blockchainIdentifier,
            senderId,
            appMetadata,
            origin,
            destination,
            accountId,
            account.address,
            amount,
            recipient,
            network,
        )
    }

    @Serializable
    enum class Mode {
        @SerialName("submit") Submit {
            override fun createTransferSubstrateRequest(
                id: String,
                version: String,
                blockchainIdentifier: String,
                senderId: String,
                appMetadata: @Contextual AppMetadata?,
                origin: Connection.Id,
                destination: Connection.Id,
                accountId: String,
                sourceAddress: String,
                amount: String,
                recipient: String,
                network: SubstrateNetwork,
            ): TransferSubstrateRequest = TransferSubstrateRequest.Submit(
                id,
                version,
                blockchainIdentifier,
                senderId,
                appMetadata,
                origin,
                destination,
                accountId,
                sourceAddress,
                amount,
                recipient,
                network,
            )
        },
        @SerialName("submit-and-return") SubmitAndReturn {
            override fun createTransferSubstrateRequest(
                id: String,
                version: String,
                blockchainIdentifier: String,
                senderId: String,
                appMetadata: @Contextual AppMetadata?,
                origin: Connection.Id,
                destination: Connection.Id,
                accountId: String,
                sourceAddress: String,
                amount: String,
                recipient: String,
                network: SubstrateNetwork,
            ): TransferSubstrateRequest = TransferSubstrateRequest.SubmitAndReturn(
                id,
                version,
                blockchainIdentifier,
                senderId,
                appMetadata,
                origin,
                destination,
                accountId,
                sourceAddress,
                amount,
                recipient,
                network,
            )
        },
        @SerialName("return") Return {
            override fun createTransferSubstrateRequest(
                id: String,
                version: String,
                blockchainIdentifier: String,
                senderId: String,
                appMetadata: @Contextual AppMetadata?,
                origin: Connection.Id,
                destination: Connection.Id,
                accountId: String,
                sourceAddress: String,
                amount: String,
                recipient: String,
                network: SubstrateNetwork,
            ): TransferSubstrateRequest = TransferSubstrateRequest.Return(
                id,
                version,
                blockchainIdentifier,
                senderId,
                appMetadata,
                origin,
                destination,
                accountId,
                sourceAddress,
                amount,
                recipient,
                network,
            )
        };
        
        abstract fun createTransferSubstrateRequest(
            id: String,
            version: String,
            blockchainIdentifier: String,
            senderId: String,
            appMetadata: @Contextual AppMetadata?,
            origin: Connection.Id,
            destination: Connection.Id,
            accountId: String,
            sourceAddress: String,
            amount: String,
            recipient: String,
            network: SubstrateNetwork,
        ): TransferSubstrateRequest
        
        companion object {
            fun from(transferRequest: TransferSubstrateRequest): Mode = when (transferRequest) {
                is TransferSubstrateRequest.Submit -> Submit
                is TransferSubstrateRequest.SubmitAndReturn -> SubmitAndReturn
                is TransferSubstrateRequest.Return -> Return
            }
        }
    }

    companion object {
        const val TYPE = "transfer_request"
    }
}

@Serializable
@SerialName(SignPayloadV3SubstrateRequest.TYPE)
internal data class SignPayloadV3SubstrateRequest(
    override val scope: SubstratePermission.Scope,
    val mode: Mode,
    val payload: SubstrateSignerPayload,
) : BlockchainV3SubstrateRequest() {
    @EncodeDefault
    override val type: String = TYPE

    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        accountId: String,
        blockchainIdentifier: String,
    ): BeaconMessage {
        val appMetadata = dependencyRegistry(beaconScope).storageManager.findAppMetadata<SubstrateAppMetadata> { it.senderId == senderId }
        val account = dependencyRegistry(beaconScope).storageManager.findPermission<SubstratePermission> { it.accountId == accountId }?.account ?: failWithAccountNotFound(accountId)

        return mode.createSignPayloadSubstrateRequest(
            id,
            version,
            blockchainIdentifier,
            senderId,
            appMetadata,
            origin,
            destination,
            accountId,
            account.address,
            payload
        )
    }

    @Serializable
    enum class Mode {
        @SerialName("submit") Submit {
            override fun createSignPayloadSubstrateRequest(
                id: String,
                version: String,
                blockchainIdentifier: String,
                senderId: String,
                appMetadata: @Contextual AppMetadata?,
                origin: Connection.Id,
                destination: Connection.Id,
                accountId: String,
                address: String,
                payload: SubstrateSignerPayload,
            ): SignPayloadSubstrateRequest = SignPayloadSubstrateRequest.Submit(
                id,
                version,
                blockchainIdentifier,
                senderId,
                appMetadata,
                origin,
                destination,
                accountId,
                address,
                payload,
            )
        },
        @SerialName("submit-and-return") SubmitAndReturn {
            override fun createSignPayloadSubstrateRequest(
                id: String,
                version: String,
                blockchainIdentifier: String,
                senderId: String,
                appMetadata: @Contextual AppMetadata?,
                origin: Connection.Id,
                destination: Connection.Id,
                accountId: String,
                address: String,
                payload: SubstrateSignerPayload,
            ): SignPayloadSubstrateRequest = SignPayloadSubstrateRequest.SubmitAndReturn(
                id,
                version,
                blockchainIdentifier,
                senderId,
                appMetadata,
                origin,
                destination,
                accountId,
                address,
                payload,
            )
        },
        @SerialName("return") Return {
            override fun createSignPayloadSubstrateRequest(
                id: String,
                version: String,
                blockchainIdentifier: String,
                senderId: String,
                appMetadata: @Contextual AppMetadata?,
                origin: Connection.Id,
                destination: Connection.Id,
                accountId: String,
                address: String,
                payload: SubstrateSignerPayload,
            ): SignPayloadSubstrateRequest = SignPayloadSubstrateRequest.Return(
                id,
                version,
                blockchainIdentifier,
                senderId,
                appMetadata,
                origin,
                destination,
                accountId,
                address,
                payload,
            )
        };

        abstract fun createSignPayloadSubstrateRequest(
            id: String,
            version: String,
            blockchainIdentifier: String,
            senderId: String,
            appMetadata: @Contextual AppMetadata?,
            origin: Connection.Id,
            destination: Connection.Id,
            accountId: String,
            address: String,
            payload: SubstrateSignerPayload,
        ): SignPayloadSubstrateRequest

        companion object {
            fun from(signPayloadRequest: SignPayloadSubstrateRequest): Mode = when (signPayloadRequest) {
                is SignPayloadSubstrateRequest.Submit -> Submit
                is SignPayloadSubstrateRequest.SubmitAndReturn -> SubmitAndReturn
                is SignPayloadSubstrateRequest.Return -> Return
            }
        }
    }

    companion object {
        const val TYPE = "sign_payload_request"
    }
}

@Serializable
internal data class PermissionV3SubstrateResponse(
    val appMetadata: SubstrateAppMetadata,
    val scopes: List<SubstratePermission.Scope>,
    val accounts: List<SubstrateAccount>,
) : PermissionV3BeaconResponseContent.BlockchainData() {
    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        blockchainIdentifier: String,
    ): BeaconMessage = PermissionSubstrateResponse(id, version, destination, blockchainIdentifier, appMetadata, scopes, accounts)

    companion object {
        fun from(requestResponse: PermissionSubstrateResponse): PermissionV3SubstrateResponse = with(requestResponse) {
            PermissionV3SubstrateResponse(
                appMetadata,
                scopes,
                accounts,
            )
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = BlockchainV3SubstrateResponse.Serializer::class)
@JsonClassDiscriminator(BlockchainV3SubstrateResponse.CLASS_DISCRIMINATOR)
internal sealed class BlockchainV3SubstrateResponse : BlockchainV3BeaconResponseContent.BlockchainData() {
    abstract val type: String
    
    companion object {
        const val CLASS_DISCRIMINATOR = "type"

        fun from(blockchainResponse: BlockchainSubstrateResponse): BlockchainV3SubstrateResponse = with(blockchainResponse) {
            when (this) {
                is TransferSubstrateResponse -> TransferV3SubstrateResponse.from(this)
                is SignPayloadSubstrateResponse -> SignPayloadV3SubstrateResponse.from(this)
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KJsonSerializer<BlockchainV3SubstrateResponse> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BlockchainV3SubstrateResponse") {
            element<String>("type")
        }

        override fun deserialize(jsonDecoder: JsonDecoder, jsonElement: JsonElement): BlockchainV3SubstrateResponse {
            val type = jsonElement.jsonObject.getString(descriptor.getElementName(0))

            return when (type) {
                TransferV3SubstrateResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(TransferV3SubstrateResponse.serializer(), jsonElement)
                SignPayloadV3SubstrateResponse.TYPE -> jsonDecoder.json.decodeFromJsonElement(SignPayloadV3SubstrateResponse.serializer(), jsonElement)
                else -> failWithUnknownType(type)
            }
        }

        override fun serialize(jsonEncoder: JsonEncoder, value: BlockchainV3SubstrateResponse) {
            when (value) {
                is TransferV3SubstrateResponse -> jsonEncoder.encodeSerializableValue(TransferV3SubstrateResponse.serializer(), value)
                is SignPayloadV3SubstrateResponse -> jsonEncoder.encodeSerializableValue(SignPayloadV3SubstrateResponse.serializer(), value)
            }
        }

        private fun failWithUnknownType(type: String): Nothing = failWithIllegalArgument("Unknown Substrate message type $type")
    }
}

@Serializable
@SerialName(TransferV3SubstrateResponse.TYPE)
internal data class TransferV3SubstrateResponse(
    val transactionHash: String? = null,
    val signature: String? = null,
    val payload: String? = null,
) : BlockchainV3SubstrateResponse() {
    @EncodeDefault
    override val type: String = TYPE
    
    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        blockchainIdentifier: String,
    ): BeaconMessage = when {
        transactionHash != null && signature == null && payload == null -> TransferSubstrateResponse.Submit(id, version, destination, blockchainIdentifier, transactionHash)
        transactionHash != null && signature != null -> TransferSubstrateResponse.SubmitAndReturn(id, version, destination, blockchainIdentifier, transactionHash, signature, payload)
        transactionHash == null && signature != null -> TransferSubstrateResponse.Return(id, version, destination, blockchainIdentifier, signature, payload)
        else -> failWithInvalidMessage(this)
    }

    companion object {
        const val TYPE = "transfer_response"
        
        fun from(transferResponse: TransferSubstrateResponse): TransferV3SubstrateResponse = with(transferResponse) {
            when (this) {
                is TransferSubstrateResponse.Submit -> TransferV3SubstrateResponse(transactionHash = transactionHash)
                is TransferSubstrateResponse.SubmitAndReturn -> TransferV3SubstrateResponse(transactionHash, signature, payload)
                is TransferSubstrateResponse.Return -> TransferV3SubstrateResponse(signature = signature, payload = payload)
            }
        }
    }
    
    private fun failWithInvalidMessage(message: TransferV3SubstrateResponse): Nothing = failWithIllegalArgument("Message $message is invalid.")
}

@Serializable
@SerialName(SignPayloadV3SubstrateResponse.TYPE)
internal data class SignPayloadV3SubstrateResponse(
    val transactionHash: String? = null,
    val signature: String? = null,
    val payload: String? = null,
) : BlockchainV3SubstrateResponse() {
    @EncodeDefault
    override val type: String = TYPE

    override suspend fun toBeaconMessage(
        beaconScope: BeaconScope,
        id: String,
        version: String,
        senderId: String,
        origin: Connection.Id,
        destination: Connection.Id,
        blockchainIdentifier: String,
    ): BeaconMessage = when {
        transactionHash != null && signature == null && payload == null -> SignPayloadSubstrateResponse.Submit(id, version, destination, blockchainIdentifier, transactionHash)
        transactionHash != null && signature != null -> SignPayloadSubstrateResponse.SubmitAndReturn(id, version, destination, blockchainIdentifier, transactionHash, signature, payload)
        transactionHash == null && signature != null -> SignPayloadSubstrateResponse.Return(id, version, destination, blockchainIdentifier, signature, payload)
        else -> failWithInvalidMessage(this)
    }

    companion object {
        const val TYPE = "sign_payload_response"

        fun from(transferResponse: SignPayloadSubstrateResponse): SignPayloadV3SubstrateResponse = with(transferResponse) {
            when (this) {
                is SignPayloadSubstrateResponse.Submit -> SignPayloadV3SubstrateResponse(transactionHash = transactionHash)
                is SignPayloadSubstrateResponse.SubmitAndReturn -> SignPayloadV3SubstrateResponse(transactionHash, signature, payload)
                is SignPayloadSubstrateResponse.Return -> SignPayloadV3SubstrateResponse(signature = signature, payload = payload)
            }
        }
    }

    private fun failWithInvalidMessage(message: SignPayloadV3SubstrateResponse): Nothing = failWithIllegalArgument("Message $message is invalid.")
}

private fun failWithAccountNotFound(accountId: String): Nothing = failWithIllegalArgument("Could not found an account that can handle the request ($accountId).")
