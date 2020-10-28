package it.airgap.beaconsdk.message

import it.airgap.beaconsdk.data.network.Network
import it.airgap.beaconsdk.data.permission.PermissionScope
import it.airgap.beaconsdk.data.sdk.AppMetadata
import it.airgap.beaconsdk.data.sdk.Threshold
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.BeaconConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class BeaconMessage {
    internal abstract val version: String

    abstract val id: String
    abstract var senderId: String
        internal set

    @Serializable
    sealed class Request : BeaconMessage() {
        internal abstract fun extendWithMetadata(appMetadata: AppMetadata?)

        @Serializable
        @SerialName("permission_request")
        data class Permission internal constructor(
            override val version: String,
            override val id: String,
            @SerialName("beaconId") override var senderId: String,
            val appMetadata: AppMetadata,
            val network: Network,
            val scopes: List<PermissionScope>,
        ) : Request() {

            override fun extendWithMetadata(appMetadata: AppMetadata?) = Unit // do nothing, the request comes with `appMetadata` already

            companion object {}
        }

        @Serializable
        @SerialName("operation_request")
        data class Operation internal constructor(
            override val version: String,
            override val id: String,
            @SerialName("beaconId") override var senderId: String,
            val network: Network,
            val operationDetails: TezosOperation,
            val sourceAddress: String,
        ) : Request() {
            var appMetadata: AppMetadata? = null
                internal set

            override fun extendWithMetadata(appMetadata: AppMetadata?) {
                this.appMetadata = appMetadata
            }

            companion object {}
        }

        @Serializable
        @SerialName("sign_payload_request")
        data class SignPayload internal constructor(
            override val version: String,
            override val id: String,
            @SerialName("beaconId") override var senderId: String,
            val payload: String,
            val sourceAddress: String,
        ) : Request() {
            var appMetadata: AppMetadata? = null
                internal set

            override fun extendWithMetadata(appMetadata: AppMetadata?) {
                this.appMetadata = appMetadata
            }

            companion object {}
        }

        @Serializable
        @SerialName("broadcast_request")
        data class Broadcast internal constructor(
            override val version: String,
            override val id: String,
            @SerialName("beaconId") override var senderId: String,
            val network: Network,
            val signedTransaction: String,
        ) : Request() {
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
    sealed class Response : BeaconMessage() {

        @Serializable
        @SerialName("permission_response")
        data class Permission internal constructor(
            override val version: String,
            override val id: String,
            @SerialName("beaconId") override var senderId: String,
            val publicKey: String,
            val network: Network,
            val scopes: List<PermissionScope>,
            val threshold: Threshold? = null,
        ) : Response() {
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
        @SerialName("operation_response")
        data class Operation internal constructor(
            override val version: String,
            override val id: String,
            @SerialName("beaconId") override var senderId: String,
            val transactionHash: String,
        ) : Response() {
            constructor(id: String, transactionHash: String) : this(
                BeaconConfig.versionName,
                id,
                "",
                transactionHash,
            )

            companion object {}
        }

        @Serializable
        @SerialName("sign_payload_response")
        data class SignPayload internal constructor(
            override val version: String,
            override val id: String,
            @SerialName("beaconId") override var senderId: String,
            val signature: String,
        ) : Response() {
            constructor(id: String, signature: String) : this(
                BeaconConfig.versionName,
                id,
                "",
                signature,
            )

            companion object {}
        }

        @Serializable
        @SerialName("broadcast_response")
        data class Broadcast internal constructor(
            override val version: String,
            override val id: String,
            @SerialName("beaconId") override var senderId: String,
            val transactionHash: String,
        ) : Response() {
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
    @SerialName("disconnect")
    internal data class Disconnect internal constructor(
        override val version: String,
        override val id: String,
        @SerialName("beaconId") override var senderId: String,
    ) : BeaconMessage()

    @Serializable
    @SerialName("error")
    internal data class Error internal constructor(
        override val version: String,
        override val id: String,
        @SerialName("beaconId") override var senderId: String,
        val errorType: BeaconException.Type,
    ) : BeaconMessage()

    companion object {}

}