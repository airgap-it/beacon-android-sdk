package it.airgap.blockchain.substrate.data

import it.airgap.beaconsdk.core.data.BeaconError
import kotlinx.serialization.Serializable

@Serializable
public sealed class SubstrateError : BeaconError() {
}