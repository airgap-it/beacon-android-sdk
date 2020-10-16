package it.airgap.beaconsdk.message

import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.permission.PermissionScope
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.sdk.Threshold
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.internal.message.beaconmessage.BaseBeaconMessage
import it.airgap.beaconsdk.internal.message.beaconmessage.ApiBeaconMessage

sealed class BeaconMessage : BaseBeaconMessage {
    abstract val type: Type

    sealed class Request(override val type: Type) : BeaconMessage() {
        abstract val senderId: String
        abstract val appMetadata: AppMetadata

        data class Permission internal constructor(
            override val id: String,
            override val senderId: String,
            override val appMetadata: AppMetadata,
            override val network: Network,
            override val scopes: List<PermissionScope>,
        ) : Request(Type.PermissionRequest), BaseBeaconMessage.PermissionRequest {
            companion object {}
        }

        data class Operation internal constructor(
            override val id: String,
            override val senderId: String,
            override val appMetadata: AppMetadata,
            override val network: Network,
            override val operationDetails: TezosOperation,
            override val sourceAddress: String,
        ) : Request(Type.OperationRequest), BaseBeaconMessage.OperationRequest {
            companion object {}
        }

        data class SignPayload internal constructor(
            override val id: String,
            override val senderId: String,
            override val appMetadata: AppMetadata,
            override val payload: String,
            override val sourceAddress: String,
        ) : Request(Type.SignPayloadRequest), BaseBeaconMessage.SignPayloadRequest {
            companion object {}
        }

        data class Broadcast internal constructor(
            override val id: String,
            override val senderId: String,
            override val appMetadata: AppMetadata,
            override val network: Network,
            override val signedTransaction: String,
        ) : Request(Type.BroadcastRequest), BaseBeaconMessage.BroadcastRequest {
            companion object {}
        }

        companion object {}
    }

    sealed class Response(override val type: Type) : BeaconMessage() {

        data class Permission internal constructor(
            override val id: String,
            override val publicKey: String,
            override val network: Network,
            override val scopes: List<PermissionScope>,
            override val threshold: Threshold? = null,
        ) : Response(Type.PermissionResponse), BaseBeaconMessage.PermissionResponse {
            companion object {}
        }

        data class Operation internal constructor(
            override val id: String,
            override val transactionHash: String,
        ) : Response(Type.OperationResponse), BaseBeaconMessage.OperationResponse {
            companion object {}
        }

        data class SignPayload internal constructor(
            override val id: String,
            override val signature: String,
        ) : Response(Type.SignPayloadResponse), BaseBeaconMessage.SignPayloadResponse {
            companion object {}
        }

        data class Broadcast internal constructor(
            override val id: String,
            override val transactionHash: String,
        ) : Response(Type.BroadcastResponse), BaseBeaconMessage.BroadcastResponse {
            companion object {}
        }

        companion object {}
    }

    companion object {
        internal fun fromInternalBeaconRequest(request: ApiBeaconMessage.Request, appMetadata: AppMetadata): Request =
            when (request) {
                is ApiBeaconMessage.Request.Permission -> Request.Permission(
                    request.id,
                    request.senderId,
                    request.appMetadata,
                    request.network,
                    request.scopes,
                )
                is ApiBeaconMessage.Request.Operation -> Request.Operation(
                    request.id,
                    request.senderId,
                    appMetadata,
                    request.network,
                    request.operationDetails,
                    request.sourceAddress,
                )
                is ApiBeaconMessage.Request.SignPayload -> Request.SignPayload(
                    request.id,
                    request.senderId,
                    appMetadata,
                    request.payload,
                    request.sourceAddress,
                )
                is ApiBeaconMessage.Request.Broadcast -> Request.Broadcast(
                    request.id,
                    request.senderId,
                    appMetadata,
                    request.network,
                    request.signedTransaction,
                )
            }

        internal fun fromInternalBeaconResponse(response: ApiBeaconMessage.Response): Response =
            when (response) {
                is ApiBeaconMessage.Response.Permission -> Response.Permission(
                    response.id,
                    response.publicKey,
                    response.network,
                    response.scopes,
                    response.threshold,
                )
                is ApiBeaconMessage.Response.Operation -> Response.Operation(
                    response.id,
                    response.transactionHash,
                )
                is ApiBeaconMessage.Response.SignPayload -> Response.SignPayload(
                    response.id,
                    response.signature,
                )
                is ApiBeaconMessage.Response.Broadcast -> Response.Broadcast(
                    response.id,
                    response.transactionHash,
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
    }

}