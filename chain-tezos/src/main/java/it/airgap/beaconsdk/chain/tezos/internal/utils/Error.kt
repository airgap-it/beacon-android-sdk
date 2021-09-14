package it.airgap.beaconsdk.chain.tezos.internal.utils

import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.ChainBeaconRequest
import it.airgap.beaconsdk.core.message.ChainBeaconResponse

internal fun failWithUnknownMessage(message: BeaconMessage): Nothing =
    failWithIllegalArgument("Unknown Tezos message ${message::class}")

internal fun failWithUnknownPayload(payload: ChainBeaconRequest.Payload): Nothing =
    failWithIllegalArgument("Unknown Tezos message ${payload::class}")

internal fun failWithUnknownPayload(payload: ChainBeaconResponse.Payload): Nothing =
    failWithIllegalArgument("Unknown Tezos message ${payload::class}")

internal fun failWithNotTezosRequest(payload: ChainBeaconRequest.Payload): Nothing =
    failWithIllegalState("Payload ${payload::class} is not a TezosRequest")

internal fun failWithNotTezosResponse(payload: ChainBeaconResponse.Payload): Nothing =
    failWithIllegalState("Payload ${payload::class} is not a TezosResponse")