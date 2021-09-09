package it.airgap.beaconsdk.chain.tezos.internal.utils

import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.ChainBeaconRequest
import it.airgap.beaconsdk.core.message.ChainBeaconResponse

internal fun failWithUnknownMessage(message: BeaconMessage): Nothing =
    failWithIllegalArgument("Unknown Tezos message ${message::class}")

internal fun failWithUnknownPayload(payload: ChainBeaconRequest.Payload): Nothing =
    failWithIllegalArgument("Unknown Tezos message ${payload::class}")

internal fun failWithUnknownPayload(payload: ChainBeaconResponse.Payload): Nothing =
    failWithIllegalArgument("Unknown Tezos message ${payload::class}")

internal fun failWithUnknownType(type: String): Nothing =
    failWithIllegalArgument("Unknown Tezos message \"$type\"")