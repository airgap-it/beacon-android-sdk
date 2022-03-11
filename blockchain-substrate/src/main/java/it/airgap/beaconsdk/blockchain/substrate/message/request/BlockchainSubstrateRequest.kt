package it.airgap.beaconsdk.blockchain.substrate.message.request

import it.airgap.beaconsdk.blockchain.substrate.data.*
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.message.BlockchainBeaconRequest
import it.airgap.beaconsdk.core.data.AppMetadata

public sealed class BlockchainSubstrateRequest : BlockchainBeaconRequest() {
    public abstract val scope: SubstratePermission.Scope

    public companion object {}
}

public sealed class TransferSubstrateRequest : BlockchainSubstrateRequest() {
    override val scope: SubstratePermission.Scope = SubstratePermission.Scope.Transfer

    public abstract val sourceAddress: String
    public abstract val amount: String
    public abstract val recipient: String
    public abstract val network: SubstrateNetwork

    public data class Submit internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Origin,
        override val accountId: String,
        override val sourceAddress: String,
        override val amount: String,
        override val recipient: String,
        override val network: SubstrateNetwork,
    ) : TransferSubstrateRequest()

    public data class SubmitAndReturn internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Origin,
        override val accountId: String,
        override val sourceAddress: String,
        override val amount: String,
        override val recipient: String,
        override val network: SubstrateNetwork,
    ) : TransferSubstrateRequest()

    public data class Return internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Origin,
        override val accountId: String,
        override val sourceAddress: String,
        override val amount: String,
        override val recipient: String,
        override val network: SubstrateNetwork,
    ) : TransferSubstrateRequest()

    public companion object {}
}

public sealed class SignPayloadSubstrateRequest : BlockchainSubstrateRequest() {
    override val scope: SubstratePermission.Scope
        get() = when (payload) {
            is SubstrateSignerPayload.Json -> SubstratePermission.Scope.SignPayloadJson
            is SubstrateSignerPayload.Raw -> SubstratePermission.Scope.SignPayloadRaw
        }

    public abstract val address: String
    public abstract val payload: SubstrateSignerPayload

    public data class Submit internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Origin,
        override val accountId: String?,
        override val address: String,
        override val payload: SubstrateSignerPayload,
    ) : SignPayloadSubstrateRequest()

    public data class SubmitAndReturn internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Origin,
        override val accountId: String?,
        override val address: String,
        override val payload: SubstrateSignerPayload,
    ) : SignPayloadSubstrateRequest()

    public data class Return internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Origin,
        override val accountId: String?,
        override val address: String,
        override val payload: SubstrateSignerPayload,
    ) : SignPayloadSubstrateRequest()

    public companion object {}
}
