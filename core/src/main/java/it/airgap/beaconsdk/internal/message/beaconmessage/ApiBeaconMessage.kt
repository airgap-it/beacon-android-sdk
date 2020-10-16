package it.airgap.beaconsdk.internal.message.beaconmessage

import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.permission.PermissionScope
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.sdk.Threshold
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.BeaconConfig
import it.airgap.beaconsdk.message.BeaconMessage
import kotlinx.serialization.Serializable

@Serializable
internal sealed class ApiBeaconMessage : BaseBeaconMessage {
    abstract val type: Type
    abstract val version: String
    abstract val senderId: String

    @Serializable
    sealed class Request(override val type: Type) : ApiBeaconMessage() {

        @Serializable
        data class Permission internal constructor(
            override val version: String,
            override val id: String,
            override val senderId: String,
            override val appMetadata: AppMetadata,
            override val network: Network,
            override val scopes: List<PermissionScope>,
        ) : Request(Type.PermissionRequest), BaseBeaconMessage.PermissionRequest

        @Serializable
        data class Operation internal constructor(
            override val version: String,
            override val id: String,
            override val senderId: String,
            override val network: Network,
            override val operationDetails: TezosOperation,
            override val sourceAddress: String,
        ) : Request(Type.OperationRequest), BaseBeaconMessage.OperationRequest

        @Serializable
        data class SignPayload internal constructor(
            override val version: String,
            override val id: String,
            override val senderId: String,
            override val payload: String,
            override val sourceAddress: String,
        ) : Request(Type.SignPayloadRequest), BaseBeaconMessage.SignPayloadRequest

        @Serializable
        data class Broadcast internal constructor(
            override val version: String,
            override val id: String,
            override val senderId: String,
            override val network: Network,
            override val signedTransaction: String,
        ) : Request(Type.BroadcastRequest), BaseBeaconMessage.BroadcastRequest
    }

    @Serializable
    sealed class Response(override val type: Type) : ApiBeaconMessage() {

        @Serializable
        data class Permission internal constructor(
            override val version: String,
            override val id: String,
            override val senderId: String,
            override val publicKey: String,
            override val network: Network,
            override val scopes: List<PermissionScope>,
            override val threshold: Threshold? = null,
        ) : Response(Type.PermissionResponse), BaseBeaconMessage.PermissionResponse

        @Serializable
        data class Operation internal constructor(
            override val version: String,
            override val id: String,
            override val senderId: String,
            override val transactionHash: String,
        ) : Response(Type.OperationResponse), BaseBeaconMessage.OperationResponse

        @Serializable
        data class SignPayload internal constructor(
            override val version: String,
            override val id: String,
            override val senderId: String,
            override val signature: String,
        ) : Response(Type.SignPayloadResponse), BaseBeaconMessage.SignPayloadResponse

        @Serializable
        data class Broadcast internal constructor(
            override val version: String,
            override val id: String,
            override val senderId: String,
            override val transactionHash: String,
        ) : Response(Type.BroadcastResponse), BaseBeaconMessage.BroadcastResponse
    }

    @Serializable
    data class Disconnect internal constructor(
        override val version: String,
        override val id: String,
        override val senderId: String,
    ) : ApiBeaconMessage(), BaseBeaconMessage.Disconnect {
        override val type: Type = Type.Disconnect
    }

    @Serializable
    data class Error internal constructor(
        override val version: String,
        override val id: String,
        override val senderId: String,
        override val errorType: BeaconException.Type,
    ) : ApiBeaconMessage(), BaseBeaconMessage.Error {
        override val type: Type = Type.Error
    }

    companion object {
        fun fromBeaconRequest(request: BeaconMessage.Request): Request =
            when (request) {
                is BeaconMessage.Request.Permission -> Request.Permission(
                    BeaconConfig.versionName,
                    request.id,
                    request.senderId,
                    request.appMetadata,
                    request.network,
                    request.scopes,
                )
                is BeaconMessage.Request.Operation -> Request.Operation(
                    BeaconConfig.versionName,
                    request.id,
                    request.senderId,
                    request.network,
                    request.operationDetails,
                    request.sourceAddress,
                )
                is BeaconMessage.Request.SignPayload -> Request.SignPayload(
                    BeaconConfig.versionName,
                    request.id,
                    request.senderId,
                    request.payload,
                    request.sourceAddress,
                )
                is BeaconMessage.Request.Broadcast -> Request.Broadcast(
                    BeaconConfig.versionName,
                    request.id,
                    request.senderId,
                    request.network,
                    request.signedTransaction,
                )
            }

        fun fromBeaconResponse(response: BeaconMessage.Response, senderId: String): Response =
            when (response) {
                is BeaconMessage.Response.Permission -> Response.Permission(
                    BeaconConfig.versionName,
                    response.id,
                    senderId,
                    response.publicKey,
                    response.network,
                    response.scopes,
                    response.threshold,
                )
                is BeaconMessage.Response.Operation -> Response.Operation(
                    BeaconConfig.versionName,
                    response.id,
                    senderId,
                    response.transactionHash
                )
                is BeaconMessage.Response.SignPayload -> Response.SignPayload(
                    BeaconConfig.versionName,
                    response.id,
                    senderId,
                    response.signature
                )
                is BeaconMessage.Response.Broadcast -> Response.Broadcast(
                    BeaconConfig.versionName,
                    response.id,
                    senderId,
                    response.transactionHash
                )
            }
    }

    enum class Type {
        PermissionRequest,
        PermissionResponse,

        OperationRequest,
        OperationResponse,

        SignPayloadRequest,
        SignPayloadResponse,

        BroadcastRequest,
        BroadcastResponse,

        Disconnect,
        Error,
    }
}