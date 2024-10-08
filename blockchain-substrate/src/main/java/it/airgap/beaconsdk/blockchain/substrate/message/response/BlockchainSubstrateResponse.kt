package it.airgap.beaconsdk.blockchain.substrate.message.response

import it.airgap.beaconsdk.blockchain.substrate.message.request.SignPayloadSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.request.TransferSubstrateRequest
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.message.BlockchainBeaconResponse

public sealed class BlockchainSubstrateResponse : BlockchainBeaconResponse() {
    public companion object {}
}

public sealed class TransferSubstrateResponse : BlockchainSubstrateResponse() {
    public data class Submit internal constructor(
        override val id: String,
        override val version: String,
        override val destination: Connection.Id,
        override val blockchainIdentifier: String,
        public val transactionHash: String,
    ) : TransferSubstrateResponse() {
        public companion object {}
    }

    public data class SubmitAndReturn internal constructor(
        override val id: String,
        override val version: String,
        override val destination: Connection.Id,
        override val blockchainIdentifier: String,
        public val transactionHash: String,
        public val signature: String,
        public val payload: String?,
    ) : TransferSubstrateResponse() {
        public companion object {}
    }

    public data class Return internal constructor(
        override val id: String,
        override val version: String,
        override val destination: Connection.Id,
        override val blockchainIdentifier: String,
        public val signature: String,
        public val payload: String?,
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
        public fun from(request: TransferSubstrateRequest.Submit, transactionHash: String): TransferSubstrateResponse =
            Submit(request.id, request.version, request.origin, request.blockchainIdentifier, transactionHash)

        /**
         * Creates a new instance of [TransferSubstrateResponse] from the [request]
         * with the specified [transactionHash], [signature] and [payload].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(
            request: TransferSubstrateRequest.SubmitAndReturn,
            transactionHash: String,
            signature: String,
            payload: String? = null,
        ): TransferSubstrateResponse =
            SubmitAndReturn(request.id, request.version, request.origin, request.blockchainIdentifier, transactionHash, signature, payload)

        /**
         * Creates a new instance of [TransferSubstrateResponse] from the [request]
         * with the specified [signature] and [payload].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: TransferSubstrateRequest.Return, signature: String, payload: String? = null): TransferSubstrateResponse =
            Return(request.id, request.version, request.origin, request.blockchainIdentifier, signature, payload)

        /**
         * Creates a new instance of [TransferSubstrateResponse] from the [request]
         * with the specified [transactionHash], [signature] and [payload].
         *
         * The response will have an id matching the one of the [request].
         *
         * @throws [IllegalArgumentException] if:
         *  - the [request] is [TransferSubstrateRequest.Submit] and [transactionHash] was not provided.
         *  - the [request] is [TransferSubstrateRequest.SubmitAndReturn] and [transactionHash] or [signature] were not provided.
         *  - the [request] is [TransferSubstrateRequest.Return] and [signature] was not provided.
         */
        @Throws(IllegalArgumentException::class)
        public fun from(request: TransferSubstrateRequest, transactionHash: String?, signature: String?, payload: String? = null): TransferSubstrateResponse =
            when (request) {
                is TransferSubstrateRequest.Submit -> {
                    require(transactionHash != null)

                    Submit(
                        request.id,
                        request.version,
                        request.origin,
                        request.blockchainIdentifier,
                        transactionHash,
                    )
                }
                is TransferSubstrateRequest.SubmitAndReturn -> {
                    require(transactionHash != null && signature != null)

                    SubmitAndReturn(
                        request.id,
                        request.version,
                        request.origin,
                        request.blockchainIdentifier,
                        transactionHash,
                        signature,
                        payload,
                    )
                }
                is TransferSubstrateRequest.Return -> {
                    require(signature != null)

                    Return(
                        request.id,
                        request.version,
                        request.origin,
                        request.blockchainIdentifier,
                        signature,
                        payload,
                    )
                }
            }
    }
}

public sealed class SignPayloadSubstrateResponse : BlockchainSubstrateResponse() {

    public data class Submit internal constructor(
        override val id: String,
        override val version: String,
        override val destination: Connection.Id,
        override val blockchainIdentifier: String,
        public val transactionHash: String,
    ) : SignPayloadSubstrateResponse() {
        public companion object {}
    }

    public data class SubmitAndReturn internal constructor(
        override val id: String,
        override val version: String,
        override val destination: Connection.Id,
        override val blockchainIdentifier: String,
        public val transactionHash: String,
        public val signature: String,
        public val payload: String?,
    ) : SignPayloadSubstrateResponse() {
        public companion object {}
    }

    public data class Return internal constructor(
        override val id: String,
        override val version: String,
        override val destination: Connection.Id,
        override val blockchainIdentifier: String,
        public val signature: String,
        public val payload: String?,
    ) : SignPayloadSubstrateResponse() {
        public companion object {}
    }

    public companion object {
        /**
         * Creates a new instance of [SignPayloadSubstrateResponse] from the [request]
         * with the specified [transactionHash].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: SignPayloadSubstrateRequest.Submit, transactionHash: String): SignPayloadSubstrateResponse =
            Submit(
                request.id,
                request.version,
                request.origin,
                request.blockchainIdentifier,
                transactionHash,
            )

        /**
         * Creates a new instance of [SignPayloadSubstrateResponse] from the [request]
         * with the specified [transactionHash], [signature] and [payload].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(
            request: SignPayloadSubstrateRequest.SubmitAndReturn,
            transactionHash: String,
            signature: String,
            payload: String? = null,
        ): SignPayloadSubstrateResponse =
            SubmitAndReturn(
                request.id,
                request.version,
                request.origin,
                request.blockchainIdentifier,
                transactionHash,
                signature,
                payload,
            )

        /**
         * Creates a new instance of [SignPayloadSubstrateResponse] from the [request]
         * with the specified [signature] and [payload].
         *
         * The response will have an id matching the one of the [request].
         */
        public fun from(request: SignPayloadSubstrateRequest.Return, signature: String, payload: String? = null): SignPayloadSubstrateResponse =
            Return(
                request.id,
                request.version,
                request.origin,
                request.blockchainIdentifier,
                signature,
                payload,
            )

        /**
         * Creates a new instance of [SignPayloadSubstrateResponse] from the [request]
         * with the specified [transactionHash], [signature] and [payload].
         *
         * The response will have an id matching the one of the [request].
         *
         * @throws [IllegalArgumentException] if:
         *  - the [request] is [SignPayloadSubstrateRequest.Submit] and [transactionHash] was not provided.
         *  - the [request] is [SignPayloadSubstrateRequest.SubmitAndReturn] and [transactionHash] or [signature] were not provided.
         *  - the [request] is [SignPayloadSubstrateRequest.Return] and [signature] was not provided.
         */
        @Throws(IllegalArgumentException::class)
        public fun from(request: SignPayloadSubstrateRequest, transactionHash: String?, signature: String?, payload: String? = null): SignPayloadSubstrateResponse =
            when (request) {
                is SignPayloadSubstrateRequest.Submit -> {
                    require(transactionHash != null)

                    Submit(
                        request.id,
                        request.version,
                        request.origin,
                        request.blockchainIdentifier,
                        transactionHash,
                    )
                }
                is SignPayloadSubstrateRequest.SubmitAndReturn -> {
                    require(transactionHash != null && signature != null)

                    SubmitAndReturn(
                        request.id,
                        request.version,
                        request.origin,
                        request.blockchainIdentifier,
                        transactionHash,
                        signature,
                        payload,
                    )
                }
                is SignPayloadSubstrateRequest.Return -> {
                    require(signature != null)

                    Return(
                        request.id,
                        request.version,
                        request.origin,
                        request.blockchainIdentifier,
                        signature,
                        payload,
                    )
                }
            }
    }
}
