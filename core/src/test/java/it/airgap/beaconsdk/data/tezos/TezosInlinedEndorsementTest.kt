package it.airgap.beaconsdk.data.tezos

import fromValues
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Test
import kotlin.test.assertEquals

internal class TezosInlinedEndorsementTest {

    @Test
    fun `is deserialized from JSON`() {
        listOf(
            expectedWithJson(),
            expectedWithJson(includeNulls = true),
            expectedWithJson(signature = "signature")
        )
            .map { Json.decodeFromString<TezosInlinedEndorsement>(it.second) to it.first }
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
        branch: String = "branch",
        operations: TezosEndorsementOperation = TezosEndorsementOperation("level"),
        signature: String? = null,
        includeNulls: Boolean = false,
    ): Pair<TezosInlinedEndorsement, String> {
        val values = mapOf(
            "branch" to branch,
            "operations" to Json.encodeToJsonElement(operations),
            "signature" to signature,
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return TezosInlinedEndorsement(
            branch,
            operations,
            signature,
        ) to json
    }
}