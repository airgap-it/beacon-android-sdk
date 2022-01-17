package it.airgap.beaconsdk.blockchain.substrate.data

import kotlinx.serialization.Serializable

/**
 * Substrate runtime data.
 *
 * @property [runtimeVersion]
 * @property [transactionVersion]
 */
@Serializable
public data class SubstrateRuntimeSpec(
    public val runtimeVersion: String,
    public val transactionVersion: String,
) {
    public companion object {}
}
