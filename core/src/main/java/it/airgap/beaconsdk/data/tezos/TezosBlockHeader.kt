package it.airgap.beaconsdk.data.tezos

import kotlinx.serialization.Serializable

@Serializable
data class TezosBlockHeader(
    val level: Int,
    val proto: Int,
    val predecessor: String,
    val timestamp: String,
    val validationPass: Int,
    val operationsHash: String,
    val fitness: List<String>,
    val context: String,
    val priority: Int,
    val proofOfWorkNonce: String,
    val signature: String
) {
    companion object {}
}