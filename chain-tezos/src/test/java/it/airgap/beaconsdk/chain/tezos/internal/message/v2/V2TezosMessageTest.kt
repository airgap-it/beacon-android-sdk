package it.airgap.beaconsdk.chain.tezos.internal.message.v2

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.chain.tezos.Tezos
import it.airgap.beaconsdk.chain.tezos.message.*
import it.airgap.beaconsdk.core.data.beacon.AppMetadata
import it.airgap.beaconsdk.core.data.beacon.Network
import it.airgap.beaconsdk.core.data.beacon.Origin
import it.airgap.beaconsdk.core.data.beacon.SigningType
import it.airgap.beaconsdk.core.data.tezos.TezosEndorsementOperation
import it.airgap.beaconsdk.core.data.tezos.TezosOperation
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.AccountUtils
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.ChainBeaconRequest
import it.airgap.beaconsdk.core.message.ChainBeaconResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class V2TezosMessageTest {
    @MockK
    private lateinit var accountUtils: AccountUtils

    private lateinit var storageManager: StorageManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), accountUtils)
    }

    @Test
    fun `is deserialized from JSON`() {
        messagesWithJsonStrings() + messagesWithJsonStrings(includeNulls = true)
            .map { Json.decodeFromString<V2TezosMessage>(it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `serializes to JSON`() {
        messagesWithJsonStrings()
            .map { Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonObject.serializer(), it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `is created from Beacon message`() {
        val version = "2"
        val senderId = "senderId"

        versionedWithBeacon(version, senderId)
            .map { V2TezosMessage.from(senderId, it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `converts to Beacon message`() {
        val senderId = "senderId"
        val otherId = "otherId"
        val origin = Origin.P2P(senderId)

        val matchingAppMetadata = AppMetadata(senderId, "v2App")
        val otherAppMetadata = AppMetadata(otherId, "v2OtherApp")

        runBlocking { storageManager.setAppMetadata(listOf(otherAppMetadata, matchingAppMetadata)) }

        runBlocking {
            versionedWithBeacon(senderId = senderId, appMetadata = matchingAppMetadata, origin = origin)
                .map { it.first.toBeaconMessage(origin, storageManager) to it.second }
                .forEach {
                    assertEquals(it.second, it.first)
                }
        }
    }

    // -- message to JSON --

    private fun messagesWithJsonStrings(includeNulls: Boolean = false) =
        listOf(
            createOperationRequestJsonPair(),
            createOperationRequestJsonPair(tezosOperations = listOf(TezosEndorsementOperation("level"))),
            createSignPayloadRequestJsonPair(),
            createBroadcastRequestJsonPair(),

            createOperationResponseJsonPair(),
            createSignPayloadResponseJsonPair(),
            createBroadcastResponseJsonPair(),
        )

    // -- V2BeaconMessage to BeaconMessage --

    private fun versionedWithBeacon(
        version: String = "2",
        senderId: String = "senderId",
        origin: Origin = Origin.P2P(senderId),
        appMetadata: AppMetadata? = null,
    ): List<Pair<V2BeaconMessage, BeaconMessage>> =
        listOf(
            createOperationRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin),
            createOperationRequestPair(
                version = version,
                senderId = senderId,
                tezosOperations = listOf(TezosEndorsementOperation("level")),
                appMetadata = appMetadata,
                origin = origin,
            ),
            createSignPayloadRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin),
            createBroadcastRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin),

            createOperationResponsePair(version = version, senderId = senderId, origin = origin),
            createSignPayloadResponsePair(version = version, senderId = senderId, origin = origin),
            createBroadcastResponsePair(version = version, senderId = senderId, origin = origin),
        )

    // -- request to JSON --

    private fun createOperationRequestJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: Network = Network.Custom(),
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress"
    ): Pair<OperationV2TezosRequest, String> =
        OperationV2TezosRequest(version, id, senderId, network, tezosOperations, sourceAddress) to """
            {
                "type": "operation_request",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "network": ${Json.encodeToString(network)},
                "operationDetails": ${Json.encodeToString(tezosOperations)},
                "sourceAddress": "$sourceAddress"
            }
        """.trimIndent()

    private fun createSignPayloadRequestJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
    ): Pair<SignPayloadV2TezosRequest, String> =
        SignPayloadV2TezosRequest(version, id, senderId, signingType, payload, sourceAddress) to """
            {
                "type": "sign_payload_request",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "signingType": ${Json.encodeToString(signingType)},
                "payload": "$payload",
                "sourceAddress": "$sourceAddress"
            }
        """.trimIndent()

    private fun createBroadcastRequestJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: Network = Network.Custom(),
        signedTransaction: String = "signedTransaction"
    ): Pair<BroadcastV2TezosRequest, String> =
        BroadcastV2TezosRequest(version, id, senderId, network, signedTransaction) to """
            {
                "type": "broadcast_request",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "network": ${Json.encodeToString(network)},
                "signedTransaction": "$signedTransaction"
            }
        """.trimIndent()

    // -- response to JSON --

    private fun createOperationResponseJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash"
    ): Pair<OperationV2TezosResponse, String> =
        OperationV2TezosResponse(version, id, senderId, transactionHash) to """
            {
                "type": "operation_response",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "transactionHash": "$transactionHash"
            }
        """.trimIndent()

    private fun createSignPayloadResponseJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        signature: String = "signature",
    ): Pair<SignPayloadV2TezosResponse, String> =
        SignPayloadV2TezosResponse(version, id, senderId, signingType, signature) to """
            {
                "type": "sign_payload_response",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "signingType": ${Json.encodeToString(signingType)},
                "signature": "$signature"
            }
        """.trimIndent()

    private fun createBroadcastResponseJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash"
    ): Pair<BroadcastV2TezosResponse, String> =
        BroadcastV2TezosResponse(version, id, senderId, transactionHash) to """
            {
                "type": "broadcast_response",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "transactionHash": "$transactionHash"
            }
        """.trimIndent()

    // -- request to BeaconMessage --

    private fun createOperationRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: Network = Network.Custom(),
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<OperationV2TezosRequest, ChainBeaconRequest> =
        OperationV2TezosRequest(version, id, senderId, network, tezosOperations, sourceAddress) to
                ChainBeaconRequest(id, senderId, appMetadata, Tezos.Companion.IDENTIFIER, OperationTezosRequest(network, tezosOperations, sourceAddress), origin, version)

    private fun createSignPayloadRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<SignPayloadV2TezosRequest, ChainBeaconRequest> =
        SignPayloadV2TezosRequest(version, id, senderId, signingType, payload, sourceAddress) to
                ChainBeaconRequest(id, senderId, appMetadata, Tezos.Companion.IDENTIFIER, SignPayloadTezosRequest(signingType, payload, sourceAddress), origin, version)

    private fun createBroadcastRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: Network = Network.Custom(),
        signedTransaction: String = "signedTransaction",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<BroadcastV2TezosRequest, ChainBeaconRequest> =
        BroadcastV2TezosRequest(version, id, senderId, network, signedTransaction) to
                ChainBeaconRequest(id, senderId, appMetadata, Tezos.Companion.IDENTIFIER, BroadcastTezosRequest(network, signedTransaction), origin, version)

    // -- response to BeaconMessage --

    private fun createOperationResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<OperationV2TezosResponse, ChainBeaconResponse> =
        OperationV2TezosResponse(version, id, senderId, transactionHash) to
                ChainBeaconResponse(id, Tezos.Companion.IDENTIFIER, OperationTezosResponse(transactionHash), version, origin)

    private fun createSignPayloadResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        signature: String = "signature",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<SignPayloadV2TezosResponse, ChainBeaconResponse> =
        SignPayloadV2TezosResponse(version, id, senderId, signingType, signature) to
                ChainBeaconResponse(id, Tezos.Companion.IDENTIFIER, SignPayloadTezosResponse(signingType, signature), version, origin)

    private fun createBroadcastResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<BroadcastV2TezosResponse, ChainBeaconResponse> =
        BroadcastV2TezosResponse(version, id, senderId, transactionHash) to
                ChainBeaconResponse(id, Tezos.Companion.IDENTIFIER, BroadcastTezosResponse(transactionHash), version, origin)

}