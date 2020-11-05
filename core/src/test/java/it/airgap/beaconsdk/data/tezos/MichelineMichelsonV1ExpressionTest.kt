package it.airgap.beaconsdk.data.tezos

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
            nodeWithJson()
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
            nodeWithJson()
        ).map {
            Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonObject.serializer(), it.second)
        }.forEach {
            assertEquals(it.second, it.first)
        }
    }

    private fun primitiveIntWithJson(int: String = "int") : Pair<MichelinePrimitiveInt, String> =
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
        val json = when {
            !includeNulls && args == null && annots == null -> {
                """
                    {
                        "prim": "$prim"
                    }
                """
            }
            !includeNulls && annots == null -> {
                """
                    {
                        "prim": "$prim",
                        "args": ${Json.encodeToString(args)}
                    }
                """
            }
            !includeNulls && args == null -> {
                """
                    {
                        "prim": "$prim",
                        "annots": ${Json.encodeToString(annots)}
                    }
                """
            }
            else -> {
                """
                    {
                        "prim": "$prim",
                        "args": ${Json.encodeToString(args)},
                        "annots": ${Json.encodeToString(annots)}
                    }
                """
            }
        }.trimIndent()

        return MichelinePrimitiveApplication(prim, args, annots) to json
    }

    private fun nodeWithJson(expressions: List<MichelineMichelsonV1Expression> = emptyList()) : Pair<MichelineNode, String> =
        MichelineNode(expressions) to """
                {
                    "expressions": ${Json.encodeToString(expressions)}
                }
            """.trimIndent()
}