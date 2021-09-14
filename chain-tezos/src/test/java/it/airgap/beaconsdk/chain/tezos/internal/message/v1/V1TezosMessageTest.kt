package it.airgap.beaconsdk.chain.tezos.internal.message.v1

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.chain.tezos.Tezos
import it.airgap.beaconsdk.chain.tezos.data.operation.TezosEndorsementOperation
import it.airgap.beaconsdk.chain.tezos.data.operation.TezosOperation
import it.airgap.beaconsdk.chain.tezos.message.*
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.SigningType
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
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

internal class V1TezosMessageTest {
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
        messagesWithJsonStrings() + messagesWithJsonStrings(includeNulls = false)
            .map { Json.decodeFromString<V1TezosMessage>(it.second) to it.first }
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
        val version = "1"
        val senderId = "senderId"

        versionedWithBeacon(version, senderId)
            .map { V1TezosMessage.from(senderId, it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `converts to Beacon message`() {
        val senderId = "senderId"
        val otherId = "otherId"
        val origin = Origin.P2P("v1App")

        val matchingAppMetadata = AppMetadata(senderId, "v1App")
        val otherAppMetadata = AppMetadata(otherId, "v1OtherApp")

        runBlocking { storageManager.setAppMetadata(listOf(otherAppMetadata, matchingAppMetadata)) }

        runBlocking {
            versionedWithBeacon(beaconId = senderId, appMetadata = matchingAppMetadata, origin = origin)
                .map { it.first.toBeaconMessage(origin, storageManager) to it.second }
                .forEach {
                    assertEquals(it.second, it.first)
                }
        }
    }

    // -- message to JSON --

    private fun messagesWithJsonStrings(includeNulls: Boolean = false) = listOf(
        createOperationRequestJsonPair(),
        createOperationRequestJsonPair(tezosOperations = listOf(TezosEndorsementOperation("level"))),
        createSignPayloadRequestJsonPair(),
        createBroadcastRequestJsonPair(),

        createOperationResponseJsonPair(),
        createSignPayloadResponseJsonPair(),
        createBroadcastResponseJsonPair(),
    )

    // -- V1TezosMessage to BeaconMessage --

    private fun versionedWithBeacon(
        version: String = "1",
        beaconId: String = "beaconId",
        origin: Origin = Origin.P2P(beaconId),
        appMetadata: AppMetadata? = null,
    ): List<Pair<V1BeaconMessage, BeaconMessage>> =
        listOf(
            createOperationRequestPair(version = version, beaconId = beaconId, appMetadata = appMetadata, origin = origin),
            createOperationRequestPair(
                version = version,
                beaconId = beaconId,
                tezosOperations = listOf(TezosEndorsementOperation("level")),
                appMetadata = appMetadata,
                origin = origin,
            ),
            createSignPayloadRequestPair(version = version, beaconId = beaconId, appMetadata = appMetadata, origin = origin),
            createBroadcastRequestPair(version = version, beaconId = beaconId, appMetadata = appMetadata, origin = origin),

            createOperationResponsePair(version = version, beaconId = beaconId, origin = origin),
            createSignPayloadResponsePair(version = version, beaconId = beaconId, origin = origin),
            createBroadcastResponsePair(version = version, beaconId = beaconId, origin = origin),
        )

    // -- request to JSON --

    private fun createOperationRequestJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: Network = Network.Custom(),
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress"
    ): Pair<OperationV1TezosRequest, String> =
        OperationV1TezosRequest(version, id, beaconId, network, tezosOperations, sourceAddress) to """
            {
                "type": "operation_request",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "network": ${Json.encodeToString(network)},
                "operationDetails": ${Json.encodeToString(tezosOperations)},
                "sourceAddress": "$sourceAddress"
            }
        """.trimIndent()

    private fun createSignPayloadRequestJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
    ): Pair<SignPayloadV1TezosRequest, String> =
        SignPayloadV1TezosRequest(version, id, beaconId, payload, sourceAddress) to """
            {
                "type": "sign_payload_request",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "payload": "$payload",
                "sourceAddress": "$sourceAddress"
            }
        """.trimIndent()

    private fun createBroadcastRequestJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: Network = Network.Custom(),
        signedTransaction: String = "signedTransaction"
    ): Pair<BroadcastV1TezosRequest, String> =
        BroadcastV1TezosRequest(version, id, beaconId, network, signedTransaction) to """
            {
                "type": "broadcast_request",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "network": ${Json.encodeToString(network)},
                "signedTransaction": "$signedTransaction"
            }
        """.trimIndent()

    // -- response to JSON --

    private fun createOperationResponseJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash"
    ): Pair<OperationV1TezosResponse, String> =
        OperationV1TezosResponse(version, id, beaconId, transactionHash) to """
            {
                "type": "operation_response",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "transactionHash": "$transactionHash"
            }
        """.trimIndent()

    private fun createSignPayloadResponseJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        signature: String = "signature",
    ): Pair<SignPayloadV1TezosResponse, String> =
        SignPayloadV1TezosResponse(version, id, beaconId, signature) to """
            {
                "type": "sign_payload_response",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "signature": "$signature"
            }
        """.trimIndent()

    private fun createBroadcastResponseJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash"
    ): Pair<BroadcastV1TezosResponse, String> =
        BroadcastV1TezosResponse(version, id, beaconId, transactionHash) to """
            {
                "type": "broadcast_response",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "transactionHash": "$transactionHash"
            }
        """.trimIndent()

    // -- request to BeaconMessage --

    private fun createOperationRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: Network = Network.Custom(),
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<OperationV1TezosRequest, ChainBeaconRequest<*>> =
        OperationV1TezosRequest(version, id, beaconId, network, tezosOperations, sourceAddress) to
                ChainBeaconRequest(id, beaconId, appMetadata, Tezos.IDENTIFIER, OperationTezosRequest(network, tezosOperations, sourceAddress), origin, version)

    private fun createSignPayloadRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<SignPayloadV1TezosRequest, ChainBeaconRequest<*>> =
        SignPayloadV1TezosRequest(version, id, beaconId, payload, sourceAddress) to
                ChainBeaconRequest(id, beaconId, appMetadata, Tezos.IDENTIFIER, SignPayloadTezosRequest(
                    SigningType.Raw, payload, sourceAddress), origin, version)

    private fun createBroadcastRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: Network = Network.Custom(),
        signedTransaction: String = "signedTransaction",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<BroadcastV1TezosRequest, ChainBeaconRequest<*>> =
        BroadcastV1TezosRequest(version, id, beaconId, network, signedTransaction) to
                ChainBeaconRequest(id, beaconId, appMetadata, Tezos.IDENTIFIER, BroadcastTezosRequest(network, signedTransaction), origin, version)

    // -- response to BeaconMessage --

    private fun createOperationResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<OperationV1TezosResponse, ChainBeaconResponse<*>> =
        OperationV1TezosResponse(version, id, beaconId, transactionHash) to
                ChainBeaconResponse(id, Tezos.IDENTIFIER, OperationTezosResponse(transactionHash), version, origin)

    private fun createSignPayloadResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        signature: String = "signature",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<SignPayloadV1TezosResponse, ChainBeaconResponse<*>> =
        SignPayloadV1TezosResponse(version, id, beaconId, signature) to
                ChainBeaconResponse(id, Tezos.IDENTIFIER, SignPayloadTezosResponse(SigningType.Raw, signature), version, origin)

    private fun createBroadcastResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<BroadcastV1TezosResponse, ChainBeaconResponse<*>> =
        BroadcastV1TezosResponse(version, id, beaconId, transactionHash) to ChainBeaconResponse(id, Tezos.IDENTIFIER, BroadcastTezosResponse(transactionHash), version, origin)
}