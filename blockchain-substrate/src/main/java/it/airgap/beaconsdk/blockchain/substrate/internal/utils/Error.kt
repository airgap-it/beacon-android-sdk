package it.airgap.beaconsdk.blockchain.substrate.internal.utils

import it.airgap.beaconsdk.core.internal.utils.failWithIllegalArgument
import it.airgap.beaconsdk.core.message.BeaconMessage

internal fun failWithUnknownMessage(message: BeaconMessage): Nothing =
    failWithIllegalArgument("Unknown Substrate message ${message::class}")