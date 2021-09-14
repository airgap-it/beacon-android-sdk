//package it.airgap.beaconsdk.chain.tezos.data
//
//import fromValues
//import kotlinx.serialization.decodeFromString
//import kotlinx.serialization.encodeToString
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.JsonObject
//import org.junit.Test
//import kotlin.test.assertEquals
//
//internal class TezosNetworkTest {
//    @Test
//    fun `is deserialized from JSON`() {
//        (
//                expectedWithJsonStrings() +
//                expectedWithJsonStrings(includeNulls = true) +
//                expectedWithJsonStrings(name = "name") +
//                expectedWithJsonStrings(rpcUrl = "rpcUrl") +
//                expectedWithJsonStrings(name = "name", rpcUrl = "rpcUrl")
//        ).map {
//            Json.decodeFromString<TezosNetwork>(it.second) to it.first
//        }.forEach {
//            assertEquals(it.second, it.first)
//        }
//    }
//
//    @Test
//    fun `serializes to JSON`() {
//        (
//                expectedWithJsonStrings() +
//                expectedWithJsonStrings(name = "name") +
//                expectedWithJsonStrings(rpcUrl = "rpcUrl") +
//                expectedWithJsonStrings(name = "name", rpcUrl = "rpcUrl")
//        ).map {
//            Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
//                    Json.decodeFromString(JsonObject.serializer(), it.second)
//        }.forEach {
//            assertEquals(it.second, it.first)
//        }
//    }
//
//    private fun expectedWithJsonStrings(
//        name: String? = null,
//        rpcUrl: String? = null,
//        includeNulls: Boolean = false,
//    ): List<Pair<TezosNetwork, String>> = listOf(
//        TezosNetwork.Mainnet(name, rpcUrl) to json("mainnet", name, rpcUrl, includeNulls),
//        TezosNetwork.Delphinet(name, rpcUrl) to json("delphinet", name, rpcUrl, includeNulls),
//        TezosNetwork.Edonet(name, rpcUrl) to json("edonet", name, rpcUrl, includeNulls),
//        TezosNetwork.Florencenet(name, rpcUrl) to json("florencenet", name, rpcUrl, includeNulls),
//        TezosNetwork.Granadanet(name, rpcUrl) to json("granadanet", name, rpcUrl, includeNulls),
//        TezosNetwork.Custom(name, rpcUrl) to json("custom", name, rpcUrl, includeNulls),
//    )
//
//    private fun json(
//        identifier: String,
//        name: String? = null,
//        rpcUrl: String? = null,
//        includeNulls: Boolean = false,
//    ): String {
//        val values = mapOf(
//            "type" to identifier,
//            "name" to name,
//            "rpcUrl" to rpcUrl,
//        )
//
//        return JsonObject.fromValues(values, includeNulls).toString()
//    }
//}