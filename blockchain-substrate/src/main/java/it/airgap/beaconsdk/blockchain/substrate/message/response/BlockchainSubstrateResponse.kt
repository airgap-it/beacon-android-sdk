package it.airgap.beaconsdk.blockchain.substrate.message.response

import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.message.BlockchainBeaconResponse

public sealed class BlockchainSubstrateResponse : BlockchainBeaconResponse() {
    public companion object {}
}

public sealed class TransferSubstrateResponse : BlockchainSubstrateResponse() {
    public data class Broadcast(
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        public val transactionHash: String,
    ) : TransferSubstrateResponse() {
        public companion object {}
    }

    public data class BroadcastAndReturn(
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        public val transactionHash: String,
        public val payload: String,
    ) : TransferSubstrateResponse() {
        public companion object {}
    }

    public data class Return(
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        public val payload: String,
    ) : TransferSubstrateResponse() {
        public companion object {}
    }

    public companion object {}
}

public sealed class SignSubstrateResponse : BlockchainSubstrateResponse() {

    public data class Broadcast(
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        public val signature: String,
    ) : SignSubstrateResponse() {
        public companion object {}
    }

    public data class BroadcastAndReturn(
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        public val signature: String,
        public val payload: String,
    ) : SignSubstrateResponse() {
        public companion object {}
    }

    public data class Return(
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        public val payload: String,
    ) : SignSubstrateResponse() {
        public companion object {}
    }

    public companion object {}
}