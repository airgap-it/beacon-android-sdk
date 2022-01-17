package it.airgap.beaconsdk.blockchain.substrate.message.request

import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.message.BlockchainBeaconRequest
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAppMetadata
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateNetwork
import it.airgap.beaconsdk.blockchain.substrate.data.SubstratePermission
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateRuntimeSpec
import it.airgap.beaconsdk.core.data.AppMetadata

public sealed class BlockchainSubstrateRequest : BlockchainBeaconRequest() {
    public abstract val scope: SubstratePermission.Scope

    public companion object {}
}

public sealed class TransferSubstrateRequest : BlockchainSubstrateRequest() {
    public abstract val sourceAddress: String
    public abstract val amount: String
    public abstract val recipient: String
    public abstract val network: SubstrateNetwork

    public data class Broadcast internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Origin,
        override val accountId: String,
        override val scope: SubstratePermission.Scope,
        override val sourceAddress: String,
        override val amount: String,
        override val recipient: String,
        override val network: SubstrateNetwork,
    ) : TransferSubstrateRequest() {
        public companion object {}
    }

    public data class BroadcastAndReturn internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Origin,
        override val accountId: String,
        override val scope: SubstratePermission.Scope,
        override val sourceAddress: String,
        override val amount: String,
        override val recipient: String,
        override val network: SubstrateNetwork,
    ) : TransferSubstrateRequest() {
        public companion object {}
    }

    public data class Return internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Origin,
        override val accountId: String,
        override val scope: SubstratePermission.Scope,
        override val sourceAddress: String,
        override val amount: String,
        override val recipient: String,
        override val network: SubstrateNetwork,
    ) : TransferSubstrateRequest() {
        public companion object {}
    }

    public companion object {}
}

public sealed class SignSubstrateRequest : BlockchainSubstrateRequest() {
    public abstract val network: SubstrateNetwork
    public abstract val runtimeSpec: SubstrateRuntimeSpec
    public abstract val payload: String

    public data class Broadcast internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Origin,
        override val accountId: String,
        override val scope: SubstratePermission.Scope,
        override val network: SubstrateNetwork,
        override val runtimeSpec: SubstrateRuntimeSpec,
        override val payload: String,
    ) : SignSubstrateRequest() {
        public companion object {}
    }

    public data class BroadcastAndReturn internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Origin,
        override val accountId: String,
        override val scope: SubstratePermission.Scope,
        override val network: SubstrateNetwork,
        override val runtimeSpec: SubstrateRuntimeSpec,
        override val payload: String,
    ) : SignSubstrateRequest() {
        public companion object {}
    }

    public data class Return internal constructor(
        override val id: String,
        override val version: String,
        override val blockchainIdentifier: String,
        override val senderId: String,
        override val appMetadata: AppMetadata?,
        override val origin: Origin,
        override val accountId: String,
        override val scope: SubstratePermission.Scope,
        override val network: SubstrateNetwork,
        override val runtimeSpec: SubstrateRuntimeSpec,
        override val payload: String,
    ) : SignSubstrateRequest() {
        public companion object {}
    }

    public companion object {}
}