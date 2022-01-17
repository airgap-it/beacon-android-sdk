package it.airgap.beaconsdk.blockchain.substrate.message.response

import it.airgap.beaconsdk.blockchain.substrate.message.request.SignSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.request.TransferSubstrateRequest
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.message.BlockchainBeaconResponse

public sealed class BlockchainSubstrateResponse : BlockchainBeaconResponse() {
    public companion object {}
}

public sealed class TransferSubstrateResponse : BlockchainSubstrateResponse() {
    public data class Broadcast internal constructor(
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        public val transactionHash: String,
    ) : TransferSubstrateResponse() {
        public companion object {}
    }

    public data class BroadcastAndReturn internal constructor(
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        public val transactionHash: String,
        public val payload: String,
    ) : TransferSubstrateResponse() {
        public companion object {}
    }

    public data class Return internal constructor(
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        public val payload: String,
    ) : TransferSubstrateResponse() {
        public companion object {}
    }

    public companion object {

        /**
         * Creates a new instance of [TransferSubstrateResponse] from the [request]
         * with the specified [transactionHash].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: TransferSubstrateRequest.Broadcast, transactionHash: String): TransferSubstrateResponse =
            Broadcast(request.id, request.version, request.origin, request.blockchainIdentifier, transactionHash)

        /**
         * Creates a new instance of [TransferSubstrateResponse] from the [request]
         * with the specified [transactionHash] and [payload].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: TransferSubstrateRequest.BroadcastAndReturn, transactionHash: String, payload: String): TransferSubstrateResponse =
            BroadcastAndReturn(request.id, request.version, request.origin, request.blockchainIdentifier, transactionHash, payload)

        /**
         * Creates a new instance of [TransferSubstrateResponse] from the [request]
         * with the specified [payload].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: TransferSubstrateRequest.Return, payload: String): TransferSubstrateResponse =
            Return(request.id, request.version, request.origin, request.blockchainIdentifier, payload)

        /**
         * Creates a new instance of [TransferSubstrateResponse] from the [request]
         * with the specified [transactionHash] and [payload].
         *
         * The response will have an id matching the one of the [request].
         *
         * @throws [IllegalArgumentException] if:
         *  - the [request] is [TransferSubstrateRequest.Broadcast] and [transactionHash] was not provided.
         *  - the [request] is [TransferSubstrateRequest.BroadcastAndReturn] and [transactionHash] and [payload] were not provided.
         *  - the [request] is [TransferSubstrateRequest.Return] and [payload] was not provided.
         */
        @Throws(IllegalArgumentException::class)
        public fun from(request: TransferSubstrateRequest, transactionHash: String?, payload: String?): TransferSubstrateResponse =
            when (request) {
                is TransferSubstrateRequest.Broadcast -> {
                    require(transactionHash != null)

                    Broadcast(
                        request.id,
                        request.version,
                        request.origin,
                        request.blockchainIdentifier,
                        transactionHash,
                    )
                }
                is TransferSubstrateRequest.BroadcastAndReturn -> {
                    require(transactionHash != null && payload != null)

                    BroadcastAndReturn(
                        request.id,
                        request.version,
                        request.origin,
                        request.blockchainIdentifier,
                        transactionHash,
                        payload,
                    )
                }
                is TransferSubstrateRequest.Return -> {
                    require(payload != null)

                    Return(
                        request.id,
                        request.version,
                        request.origin,
                        request.blockchainIdentifier,
                        payload,
                    )
                }
            }
    }
}

public sealed class SignSubstrateResponse : BlockchainSubstrateResponse() {

    public data class Broadcast internal constructor(
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        public val signature: String,
    ) : SignSubstrateResponse() {
        public companion object {}
    }

    public data class BroadcastAndReturn internal constructor(
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        public val signature: String,
        public val payload: String,
    ) : SignSubstrateResponse() {
        public companion object {}
    }

    public data class Return internal constructor(
        override val id: String,
        override val version: String,
        override val requestOrigin: Origin,
        override val blockchainIdentifier: String,
        public val payload: String,
    ) : SignSubstrateResponse() {
        public companion object {}
    }

    public companion object {
        /**
         * Creates a new instance of [SignSubstrateResponse] from the [request]
         * with the specified [transactionHash].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: SignSubstrateRequest.Broadcast, transactionHash: String): SignSubstrateResponse =
            Broadcast(request.id,
                request.version,
                request.origin,
                request.blockchainIdentifier,
                transactionHash)

        /**
         * Creates a new instance of [SignSubstrateResponse] from the [request]
         * with the specified [transactionHash] and [payload].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: SignSubstrateRequest.BroadcastAndReturn, transactionHash: String, payload: String): SignSubstrateResponse =
            BroadcastAndReturn(request.id,
                request.version,
                request.origin,
                request.blockchainIdentifier,
                transactionHash,
                payload)

        /**
         * Creates a new instance of [SignSubstrateResponse] from the [request]
         * with the specified [payload].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: SignSubstrateRequest.Return, payload: String): SignSubstrateResponse =
            Return(request.id,
                request.version,
                request.origin,
                request.blockchainIdentifier,
                payload)

        /**
         * Creates a new instance of [SignSubstrateResponse] from the [request]
         * with the specified [transactionHash] and [payload].
         *
         * The response will have an id matching the one of the [request].
         *
         * @throws [IllegalArgumentException] if:
         *  - the [request] is [SignSubstrateRequest.Broadcast] and [transactionHash] was not provided.
         *  - the [request] is [SignSubstrateRequest.BroadcastAndReturn] and [transactionHash] and [payload] were not provided.
         *  - the [request] is [SignSubstrateRequest.Return] and [payload] was not provided.
         */
        @Throws(IllegalArgumentException::class)
        public fun from(request: SignSubstrateRequest, transactionHash: String?, payload: String?): SignSubstrateResponse =
            when (request) {
                is SignSubstrateRequest.Broadcast -> {
                    require(transactionHash != null)

                    Broadcast(
                        request.id,
                        request.version,
                        request.origin,
                        request.blockchainIdentifier,
                        transactionHash,
                    )
                }
                is SignSubstrateRequest.BroadcastAndReturn -> {
                    require(transactionHash != null && payload != null)

                    BroadcastAndReturn(
                        request.id,
                        request.version,
                        request.origin,
                        request.blockchainIdentifier,
                        transactionHash,
                        payload,
                    )
                }
                is SignSubstrateRequest.Return -> {
                    require(payload != null)

                    Return(
                        request.id,
                        request.version,
                        request.origin,
                        request.blockchainIdentifier,
                        payload,
                    )
                }
            }
    }
}