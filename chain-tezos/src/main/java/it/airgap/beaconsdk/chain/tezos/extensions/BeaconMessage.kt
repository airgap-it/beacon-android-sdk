package it.airgap.beaconsdk.chain.tezos.extensions

import it.airgap.beaconsdk.chain.tezos.internal.utils.failWithNotTezosRequest
import it.airgap.beaconsdk.chain.tezos.internal.utils.failWithNotTezosResponse
import it.airgap.beaconsdk.chain.tezos.message.TezosRequest
import it.airgap.beaconsdk.chain.tezos.message.TezosResponse
import it.airgap.beaconsdk.core.message.ChainBeaconRequest
import it.airgap.beaconsdk.core.message.ChainBeaconResponse

// -- ChainBeaconRequest --

/**
 * Casts the target [ChainBeaconRequest] to a request with a [TezosRequest] payload.
 *
 * @return The target [ChainBeaconRequest] as ChainBeaconRequest<TezosRequest>.
 * @throws [IllegalStateException] if the payload has not been recognized as a [TezosRequest] payload.
 */
public fun ChainBeaconRequest<*>.asTezosRequest(): ChainBeaconRequest<TezosRequest> =
    asTezosRequestOrNull() ?: failWithNotTezosRequest(payload)

/**
 * Casts the target [ChainBeaconRequest] to a request with a [TezosRequest] payload.
 *
 * @return The target [ChainBeaconRequest] as ChainBeaconRequest<TezosRequest>
 * or null if the payload has not been recognized as a [TezosRequest] payload.
 */
@Suppress("UNCHECKED_CAST")
public fun ChainBeaconRequest<*>.asTezosRequestOrNull(): ChainBeaconRequest<TezosRequest>? =
    when (payload) {
        is TezosRequest -> this as ChainBeaconRequest<TezosRequest>
        else -> null
    }

/**
 * Executes the [block] if the target [ChainBeaconRequest] has a payload of type [TezosRequest].
 */
@Suppress("UNCHECKED_CAST")
public inline fun ChainBeaconRequest<*>.onTezosRequest(block: (ChainBeaconRequest<TezosRequest>) -> Unit) {
    when (payload) {
        is TezosRequest -> block(this as ChainBeaconRequest<TezosRequest>)
    }
}

// -- ChainBeaconResponse --

/**
 * Casts the target [ChainBeaconResponse] to a response with a [TezosRequest] payload.
 *
 * @return The target [ChainBeaconResponse] as ChainBeaconResponse<TezosRequest>
 * @throws [IllegalStateException] if the payload has not been recognized as a [TezosResponse] payload.
 */
public fun ChainBeaconResponse<*>.asTezosResponse(): ChainBeaconResponse<TezosResponse> =
    asTezosResponseOrNull() ?: failWithNotTezosResponse(payload)

/**
 * Casts the target [ChainBeaconResponse] to a response with a [TezosRequest] payload.
 *
 * @return The target [ChainBeaconResponse] as ChainBeaconResponse<TezosRequest>
 * or null if the payload has not been recognized as a [TezosResponse] payload.
 */
@Suppress("UNCHECKED_CAST")
public fun ChainBeaconResponse<*>.asTezosResponseOrNull(): ChainBeaconResponse<TezosResponse>? =
    when (payload) {
        is TezosRequest -> this as ChainBeaconResponse<TezosResponse>
        else -> null
    }

/**
 * Executes the given [block] if the target [ChainBeaconResponse] has a payload of type [TezosResponse].
 */
@Suppress("UNCHECKED_CAST")
public inline fun ChainBeaconResponse<*>.onTezosResponse(block: (ChainBeaconResponse<TezosResponse>) -> Unit) {
    when (payload) {
        is TezosResponse -> block(this as ChainBeaconResponse<TezosResponse>)
    }
}