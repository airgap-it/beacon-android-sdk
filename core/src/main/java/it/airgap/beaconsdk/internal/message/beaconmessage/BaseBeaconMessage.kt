package it.airgap.beaconsdk.internal.message.beaconmessage

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.permission.PermissionScope
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.sdk.Threshold
import it.airgap.beaconsdk.data.tezos.TezosOperation

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface BaseBeaconMessage {
    val id: String

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface PermissionRequest {
        val appMetadata: AppMetadata
        val network: Network
        val scopes: List<PermissionScope>
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface PermissionResponse {
        val publicKey: String
        val network: Network
        val scopes: List<PermissionScope>
        val threshold: Threshold?
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface OperationRequest {
        val network: Network
        val operationDetails: TezosOperation
        val sourceAddress: String
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface OperationResponse {
        val transactionHash: String
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface SignPayloadRequest {
        val payload: String
        val sourceAddress: String
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface SignPayloadResponse {
        val signature: String
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface BroadcastRequest {
        val network: Network
        val signedTransaction: String
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface BroadcastResponse {
        val transactionHash: String
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface Disconnect

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface Error {
        val errorType: BeaconException.Type
    }
}