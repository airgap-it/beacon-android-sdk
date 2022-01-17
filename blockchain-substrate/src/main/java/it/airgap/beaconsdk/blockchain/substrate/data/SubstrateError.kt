package it.airgap.beaconsdk.blockchain.substrate.data

import it.airgap.beaconsdk.core.data.BeaconError
import kotlinx.serialization.Serializable

/**
 * Types of Substrate errors supported in Beacon
 */
@Serializable
public sealed class SubstrateError : BeaconError() {

    public companion object {}
}