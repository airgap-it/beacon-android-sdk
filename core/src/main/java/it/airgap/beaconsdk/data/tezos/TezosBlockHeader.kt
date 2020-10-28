package it.airgap.beaconsdk.data.tezos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TezosBlockHeader(
    val level: Int,
    val proto: Int,
    val predecessor: String,
    val timestamp: String,
    @SerialName("validation_pass") val validationPass: Int,
    @SerialName("operations_hash") val operationsHash: String,
    val fitness: List<String>,
    val context: String,
    val priority: Int,
    @SerialName("proof_of_work_nonce") val proofOfWorkNonce: String,
    val signature: String
) {
    companion object {}
}