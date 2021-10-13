package it.airgap.beaconsdk.blockchain.tezos.data.operation

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

internal class TezosBlockHeaderTest {

    @Test
    fun `is deserialized from JSON`() {
        listOf(expectedWithJson())
            .map { Json.decodeFromString<TezosBlockHeader>(it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(expectedWithJson())
            .map {
                Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                        Json.decodeFromString(JsonObject.serializer(), it.second)
            }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    private fun expectedWithJson(
        level: Int = 0,
        proto: Int = 0,
        predecessor: String = "predecessor",
        timestamp: String = "timestamp",
        validationPass: Int = 0,
        operationsHash: String = "operationsHash",
        fitness: List<String> = emptyList(),
        context: String = "context",
        priority: Int = 0,
        proofOfWorkNonce: String = "proofOfWorkNonce",
        signature: String = "signature",
    ): Pair<TezosBlockHeader, String> =
        TezosBlockHeader(
            level,
            proto,
            predecessor,
            timestamp,
            validationPass,
            operationsHash,
            fitness,
            context,
            priority,
            proofOfWorkNonce,
            signature,
        ) to """
            {
                "level": $level,
                "proto": $proto,
                "predecessor": "$predecessor",
                "timestamp": "$timestamp",
                "validation_pass": $validationPass,
                "operations_hash": "$operationsHash",
                "fitness": ${Json.encodeToString(fitness)},
                "context": "$context",
                "priority": $priority,
                "proof_of_work_nonce": "$proofOfWorkNonce",
                "signature": "$signature"
            }
        """.trimIndent()
}