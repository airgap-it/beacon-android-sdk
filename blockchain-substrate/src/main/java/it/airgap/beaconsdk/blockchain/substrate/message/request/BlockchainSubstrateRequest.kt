package it.airgap.beaconsdk.blockchain.substrate.message.request

import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.message.BlockchainBeaconRequest
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAppMetadata
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateNetwork
import it.airgap.beaconsdk.blockchain.substrate.data.SubstratePermission
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateRuntimeSpec

public sealed class BlockchainSubstrateRequest : BlockchainBeaconRequest() {
    public abstract val scope: SubstratePermission.Scope

    public companion object {}
}

public data class TransferSubstrateRequest(
    override val id: String,
    override val version: String,
    override val blockchainIdentifier: String,
    override val senderId: String,
    override val appMetadata: SubstrateAppMetadata?,
    override val origin: Origin,
    override val accountId: String,
    override val scope: SubstratePermission.Scope,
    public val sourceAddress: String,
    public val amount: String,
    public val recipient: String,
    public val network: SubstrateNetwork,
    public val mode: Mode,
) : BlockchainSubstrateRequest() {

    public enum class Mode {
        Broadcast,
        BroadcastAndReturn,
        Return;

        public companion object {}
    }

    public companion object {}
}

public data class SignSubstrateRequest(
    override val id: String,
    override val version: String,
    override val blockchainIdentifier: String,
    override val senderId: String,
    override val appMetadata: SubstrateAppMetadata?,
    override val origin: Origin,
    override val accountId: String,
    override val scope: SubstratePermission.Scope,
    public val network: SubstrateNetwork,
    public val runtimeSpec: SubstrateRuntimeSpec,
    public val payload: String,
    public val mode: Mode,
) : BlockchainSubstrateRequest() {

    public enum class Mode {
        Broadcast,
        BroadcastAndReturn,
        Return;

        public companion object {}
    }

    public companion object {}
}