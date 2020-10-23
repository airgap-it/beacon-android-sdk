package it.airgap.beaconsdk.message

import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.permission.PermissionScope
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.sdk.Threshold
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.BeaconConfig
import kotlinx.serialization.Serializable

@Serializable
sealed class BeaconMessage {
    internal abstract val type: Type
    internal abstract val version: String

    abstract val id: String
    abstract var senderId: String
        internal set

    @Serializable
    sealed class Request(override val type: Type) : BeaconMessage() {
        internal abstract fun extendWithMetadata(appMetadata: AppMetadata?)

        @Serializable
        data class Permission internal constructor(
            override val version: String,
            override val id: String,
            override var senderId: String,
            val appMetadata: AppMetadata,
            val network: Network,
            val scopes: List<PermissionScope>,
        ) : Request(Type.PermissionRequest) {

            override fun extendWithMetadata(appMetadata: AppMetadata?) = Unit // do nothing, the request comes with `appMetadata` already

            companion object {}
        }

        @Serializable
        data class Operation internal constructor(
            override val version: String,
            override val id: String,
            override var senderId: String,
            val network: Network,
            val operationDetails: TezosOperation,
            val sourceAddress: String,
        ) : Request(Type.OperationRequest) {
            var appMetadata: AppMetadata? = null
                internal set

            override fun extendWithMetadata(appMetadata: AppMetadata?) {
                this.appMetadata = appMetadata
            }

            companion object {}
        }

        @Serializable
        data class SignPayload internal constructor(
            override val version: String,
            override val id: String,
            override var senderId: String,
            val payload: String,
            val sourceAddress: String,
        ) : Request(Type.SignPayloadRequest) {
            var appMetadata: AppMetadata? = null
                internal set

            override fun extendWithMetadata(appMetadata: AppMetadata?) {
                this.appMetadata = appMetadata
            }

            companion object {}
        }

        @Serializable
        data class Broadcast internal constructor(
            override val version: String,
            override val id: String,
            override var senderId: String,
            val network: Network,
            val signedTransaction: String,
        ) : Request(Type.BroadcastRequest) {
            var appMetadata: AppMetadata? = null
                internal set

            override fun extendWithMetadata(appMetadata: AppMetadata?) {
                this.appMetadata = appMetadata
            }

            companion object {}
        }

        companion object {}
    }

    @Serializable
    sealed class Response(override val type: Type) : BeaconMessage() {

        @Serializable
        data class Permission internal constructor(
            override val version: String,
            override val id: String,
            override var senderId: String,
            val publicKey: String,
            val network: Network,
            val scopes: List<PermissionScope>,
            val threshold: Threshold? = null,
        ) : Response(Type.PermissionResponse) {
            constructor(
                id: String,
                publicKey: String,
                network: Network,
                scopes: List<PermissionScope>,
                threshold: Threshold? = null,
            ) : this(BeaconConfig.versionName, id, "", publicKey, network, scopes, threshold)

            companion object {}
        }

        @Serializable
        data class Operation internal constructor(
            override val version: String,
            override val id: String,
            override var senderId: String,
            val transactionHash: String,
        ) : Response(Type.OperationResponse) {
            constructor(id: String, transactionHash: String) : this(
                BeaconConfig.versionName,
                id,
                "",
                transactionHash,
            )

            companion object {}
        }

        @Serializable
        data class SignPayload internal constructor(
            override val version: String,
            override val id: String,
            override var senderId: String,
            val signature: String,
        ) : Response(Type.SignPayloadResponse) {
            constructor(id: String, signature: String) : this(
                BeaconConfig.versionName,
                id,
                "",
                signature,
            )

            companion object {}
        }

        @Serializable
        data class Broadcast internal constructor(
            override val version: String,
            override val id: String,
            override var senderId: String,
            val transactionHash: String,
        ) : Response(Type.BroadcastResponse) {
            constructor(id: String, transactionHash: String) : this(
                BeaconConfig.versionName,
                id,
                "",
                transactionHash,
            )

            companion object {}
        }

        companion object {}
    }

    @Serializable
    internal data class Disconnect internal constructor(
        override val version: String,
        override val id: String,
        override var senderId: String,
    ) : BeaconMessage() {
        override val type: Type = Type.Disconnect
    }

    @Serializable
    internal data class Error internal constructor(
        override val version: String,
        override val id: String,
        override var senderId: String,
        val errorType: BeaconException.Type,
    ) : BeaconMessage() {
        override val type: Type = Type.Error
    }

    companion object {}

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