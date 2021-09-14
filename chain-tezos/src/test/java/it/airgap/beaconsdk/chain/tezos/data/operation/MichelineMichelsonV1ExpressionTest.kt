package it.airgap.beaconsdk.chain.tezos.data.operation

import fromValues
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Test
import kotlin.test.assertEquals

internal class MichelineMichelsonV1ExpressionTest {
    @Test
    fun `is deserialized from JSON`() {
        listOf(
            primitiveIntWithJson(),
            primitiveStringWithJson(),
            primitiveBytesWithJson(),
            primitiveApplicationWithJson(),
            primitiveApplicationWithJson(includeNulls = true),
            nodeWithJson(),
            nodeWithJson(listOf(MichelinePrimitiveInt(0))),
        ).map {
            Json.decodeFromString<MichelineMichelsonV1Expression>(it.second) to it.first
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    @Test
    fun `serializes to JSON`() {
        listOf(
            primitiveIntWithJson(),
            primitiveStringWithJson(),
            primitiveBytesWithJson(),
            primitiveApplicationWithJson(),
            nodeWithJson(),
            nodeWithJson(listOf(MichelinePrimitiveInt(0))),
        ).map {
            Json.decodeFromString(JsonElement.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonElement.serializer(), it.second)
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    private fun primitiveIntWithJson(int: Int = 0) : Pair<MichelinePrimitiveInt, String> =
        MichelinePrimitiveInt(int) to """
            {
                "int": "$int"
            }
        """.trimIndent()

    private fun primitiveStringWithJson(string: String = "string") : Pair<MichelinePrimitiveString, String> =
        MichelinePrimitiveString(string) to """
            {
                "string": "$string"
            }
        """.trimIndent()

    private fun primitiveBytesWithJson(bytes: String = "bytes") : Pair<MichelinePrimitiveBytes, String> =
        MichelinePrimitiveBytes(bytes) to """
                {
                    "bytes": "$bytes"
                }
            """.trimIndent()

    private fun primitiveApplicationWithJson(
        prim: String = "prim",
        args: List<MichelineMichelsonV1Expression>? = null,
        annots: List<String>? = null,
        includeNulls: Boolean = false,
    ) : Pair<MichelinePrimitiveApplication, String> {
        val values = mapOf(
            "prim" to prim,
            "args" to args?.let { Json.encodeToJsonElement(it) },
            "annots" to annots?.let { Json.encodeToJsonElement(it) },
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return MichelinePrimitiveApplication(prim, args, annots) to json
    }

    private fun nodeWithJson(expressions: List<MichelineMichelsonV1Expression> = emptyList()) : Pair<MichelineNode, String> =
        MichelineNode(expressions) to Json.encodeToString(expressions)
}