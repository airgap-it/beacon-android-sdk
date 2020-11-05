package it.airgap.beaconsdk.internal.message.beacon.v1

import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Network
import it.airgap.beaconsdk.data.beacon.PermissionScope
import it.airgap.beaconsdk.data.beacon.Threshold
import it.airgap.beaconsdk.data.tezos.TezosEndorsementOperation
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.message.v1.*
import it.airgap.beaconsdk.internal.storage.MockStorage
import it.airgap.beaconsdk.internal.storage.decorator.DecoratedExtendedStorage
import it.airgap.beaconsdk.message.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class V1BeaconMessageTest {

    private lateinit var storage: DecoratedExtendedStorage

    @Before
    fun setup() {
        storage = DecoratedExtendedStorage(MockStorage())
    }

    @Test
    fun `is deserialized from JSON`() {
        messagesWithJsonStrings() + messagesWithJsonStrings(includeNulls = false)
            .map { Json.decodeFromString<V1BeaconMessage>(it.second) to it.first }
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
    fun `checks if pairs with other Beacon message`() {
        val matchingId = "1"
        val otherId = "2"

        val matchingRequest = BroadcastBeaconRequest(matchingId, "senderId", null, Network.Custom(), "tx")
        val otherRequest = BroadcastBeaconRequest(otherId, "senderId", null, Network.Custom(), "tx")

        val response = BroadcastV1BeaconResponse("1", matchingId, "beaconId", "txHash")

        assertTrue(response.pairsWith(matchingRequest), "Expected response matching request")
        assertFalse(response.pairsWith(otherRequest), "Expected response not matching other request")
    }

    @Test
    fun `checks if pairs with other versioned message`() {
        val matchingId = "1"
        val otherId = "2"

        val matchingRequest = BroadcastV1BeaconRequest("1", matchingId, "beaconId", Network.Custom(), "tx")
        val otherRequest = BroadcastV1BeaconRequest("1", otherId, "beaconId", Network.Custom(), "tx")

        val response = BroadcastV1BeaconResponse("1", matchingId, "beaconId", "txHash")

        assertTrue(response.pairsWith(matchingRequest), "Expected response to match request")
        assertFalse(response.pairsWith(otherRequest), "Expected response not to match other request")
    }

    @Test
    fun `checks if comes from app described by metadata`() {
        val matchingSenderId = "senderId"
        val otherSenderId = "otherId"

        val matchingAppMetadata = AppMetadata(matchingSenderId, "v1App")
        val otherAppMetadata = AppMetadata(otherSenderId, "v1OtherApp")

        val message = BroadcastV1BeaconResponse("1", "id", matchingSenderId, "txHash")

        assertTrue(message.comesFrom(matchingAppMetadata), "Expected message to come from app described by metadata")
        assertFalse(message.comesFrom(otherAppMetadata), "Expected message not to come from app described by metadata")
    }

    @Test
    fun `is created from Beacon message`() {
        val version = "1"
        val senderId = "senderId"

        versionedWithBeacon(version, senderId)
            .map { V1BeaconMessage.fromBeaconMessage(version, senderId, it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `converts to Beacon message`() {
        val senderId = "senderId"
        val otherId = "otherId"

        val matchingAppMetadata = AppMetadata(senderId, "v1App")
        val otherAppMetadata = AppMetadata(otherId, "v1OtherApp")

        runBlocking { storage.setAppsMetadata(listOf(otherAppMetadata, matchingAppMetadata)) }

        runBlocking {
            versionedWithBeacon(beaconId = senderId, appMetadata = matchingAppMetadata)
                .map { it.first.toBeaconMessage(storage) to it.second }
                .forEach {
                    assertEquals(it.second, it.first)
                }
        }
    }

    // -- message to JSON --

    private fun messagesWithJsonStrings(includeNulls: Boolean = false) = listOf(
        createPermissionRequestJsonPair(),
        createPermissionRequestJsonPair(scopes = listOf(PermissionScope.Sign)),
        createOperationRequestJsonPair(tezosOperation = TezosEndorsementOperation("level")),
        createSignPayloadRequestJsonPair(),
        createBroadcastRequestJsonPair(),

        createPermissionResponseJsonPair(),
        createPermissionResponseJsonPair(includeNulls = includeNulls),
        createPermissionResponseJsonPair(threshold = Threshold("amount", "timeframe")),
        createOperationResponseJsonPair(),
        createSignPayloadResponseJsonPair(),
        createBroadcastResponseJsonPair(),

        createDisconnectJsonPair(),
        createErrorJsonPair(),
    )

    // -- V1BeaconMessage to BeaconMessage --

    private fun versionedWithBeacon(
        version: String = "1",
        beaconId: String = "beaconId",
        appMetadata: AppMetadata? = null,
    ): List<Pair<V1BeaconMessage, BeaconMessage>> =
        listOf(
            createPermissionRequestPair(version = version, beaconId = beaconId),
            createOperationRequestPair(
                version = version,
                beaconId = beaconId,
                tezosOperation = TezosEndorsementOperation("level"),
                appMetadata = appMetadata
            ),
            createSignPayloadRequestPair(version = version, beaconId = beaconId, appMetadata = appMetadata),
            createBroadcastRequestPair(version = version, beaconId = beaconId, appMetadata = appMetadata),

            createPermissionResponsePair(version = version, beaconId = beaconId),
            createOperationResponsePair(version = version, beaconId = beaconId),
            createSignPayloadResponsePair(version = version, beaconId = beaconId),
            createBroadcastResponsePair(version = version, beaconId = beaconId),

            createDisconnectPair(version = version, beaconId = beaconId),
            createErrorPair(version = version, beaconId = beaconId),
        )

    // -- request to JSON --

    private fun createPermissionRequestJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        appMetadata: V1AppMetadata = V1AppMetadata("beaconId", "v1App"),
        network: Network = Network.Custom(),
        scopes: List<PermissionScope> = emptyList()
    ): Pair<PermissionV1BeaconRequest, String> =
        PermissionV1BeaconRequest(version, id, beaconId, appMetadata, network, scopes) to """
            {
                "type": "permission_request",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "appMetadata": ${Json.encodeToString(appMetadata)},
                "network": ${Json.encodeToString(network)},
                "scopes": ${Json.encodeToString(scopes)}
            }
        """.trimIndent()

    private fun createOperationRequestJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: Network = Network.Custom(),
        tezosOperation: TezosOperation,
        sourceAddress: String = "sourceAddress"
    ): Pair<OperationV1BeaconRequest, String> =
        OperationV1BeaconRequest(version, id, beaconId, network, tezosOperation, sourceAddress) to """
            {
                "type": "operation_request",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "network": ${Json.encodeToString(network)},
                "operationDetails": ${Json.encodeToString(tezosOperation)},
                "sourceAddress": "$sourceAddress"
            }
        """.trimIndent()

    private fun createSignPayloadRequestJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
    ): Pair<SignPayloadV1BeaconRequest, String> =
        SignPayloadV1BeaconRequest(version, id, beaconId, payload, sourceAddress) to """
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
    ): Pair<BroadcastV1BeaconRequest, String> =
        BroadcastV1BeaconRequest(version, id, beaconId, network, signedTransaction) to """
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

    private fun createPermissionResponseJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        publicKey: String = "publicKey",
        network: Network = Network.Custom(),
        scopes: List<PermissionScope> = emptyList(),
        threshold: Threshold? = null,
        includeNulls: Boolean = false,
    ): Pair<PermissionV1BeaconResponse, String> {
        val json = if (threshold != null || includeNulls) {
            """
                {
                    "type": "permission_response",
                    "version": "$version",
                    "id": "$id",
                    "beaconId": "$beaconId",
                    "publicKey": "$publicKey",
                    "network": ${Json.encodeToString(network)},
                    "scopes": ${Json.encodeToString(scopes)},
                    "threshold": ${Json.encodeToString(threshold)}
                }
            """
        } else {
            """
                {
                    "type": "permission_response",
                    "version": "$version",
                    "id": "$id",
                    "beaconId": "$beaconId",
                    "publicKey": "$publicKey",
                    "network": ${Json.encodeToString(network)},
                    "scopes": ${Json.encodeToString(scopes)}
                }
            """
        }

        return PermissionV1BeaconResponse(version, id, beaconId, publicKey, network, scopes, threshold) to json.trimIndent()
    }

    private fun createOperationResponseJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash"
    ): Pair<OperationV1BeaconResponse, String> =
        OperationV1BeaconResponse(version, id, beaconId, transactionHash) to """
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
    ): Pair<SignPayloadV1BeaconResponse, String> =
        SignPayloadV1BeaconResponse(version, id, beaconId, signature) to """
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
    ): Pair<BroadcastV1BeaconResponse, String> =
        BroadcastV1BeaconResponse(version, id, beaconId, transactionHash) to """
            {
                "type": "broadcast_response",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "transactionHash": "$transactionHash"
            }
        """.trimIndent()

    // -- other to JSON --

    private fun createDisconnectJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
    ): Pair<DisconnectV1BeaconMessage, String> =
        DisconnectV1BeaconMessage(version, id, beaconId) to """
            {
                "type": "disconnect",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId"
            }
        """.trimIndent()

    private fun createErrorJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        errorType: BeaconException.Type = BeaconException.Type.Unknown,
    ): Pair<ErrorV1BeaconMessage, String> =
        ErrorV1BeaconMessage(version, id, beaconId, errorType) to """
            {
                "type": "error",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "errorType": ${Json.encodeToString(errorType)}
            }
        """.trimIndent()

    // -- request to BeaconMessage --

    private fun createPermissionRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        appMetadata: V1AppMetadata = V1AppMetadata("beaconId", "v1App"),
        network: Network = Network.Custom(),
        scopes: List<PermissionScope> = emptyList()
    ): Pair<PermissionV1BeaconRequest, PermissionBeaconRequest> =
        PermissionV1BeaconRequest(version, id, beaconId, appMetadata, network, scopes) to
                PermissionBeaconRequest(id, beaconId, appMetadata.toAppMetadata(), network, scopes)

    private fun createOperationRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: Network = Network.Custom(),
        tezosOperation: TezosOperation,
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
    ): Pair<OperationV1BeaconRequest, OperationBeaconRequest> =
        OperationV1BeaconRequest(version, id, beaconId, network, tezosOperation, sourceAddress) to
                OperationBeaconRequest(id, beaconId, appMetadata, network, tezosOperation, sourceAddress)

    private fun createSignPayloadRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
    ): Pair<SignPayloadV1BeaconRequest, SignPayloadBeaconRequest> =
        SignPayloadV1BeaconRequest(version, id, beaconId, payload, sourceAddress) to
                SignPayloadBeaconRequest(id, beaconId, appMetadata, payload, sourceAddress)

    private fun createBroadcastRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: Network = Network.Custom(),
        signedTransaction: String = "signedTransaction",
        appMetadata: AppMetadata? = null,
    ): Pair<BroadcastV1BeaconRequest, BroadcastBeaconRequest> =
        BroadcastV1BeaconRequest(version, id, beaconId, network, signedTransaction) to
                BroadcastBeaconRequest(id, beaconId, appMetadata, network, signedTransaction)

    // -- response to BeaconMessage --

    private fun createPermissionResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        publicKey: String = "publicKey",
        network: Network = Network.Custom(),
        scopes: List<PermissionScope> = emptyList(),
        threshold: Threshold? = null,
    ): Pair<PermissionV1BeaconResponse, PermissionBeaconResponse> =
        PermissionV1BeaconResponse(version, id, beaconId, publicKey, network, scopes, threshold) to
                PermissionBeaconResponse(id, publicKey, network, scopes, threshold)

    private fun createOperationResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash"
    ): Pair<OperationV1BeaconResponse, OperationBeaconResponse> =
        OperationV1BeaconResponse(version, id, beaconId, transactionHash) to OperationBeaconResponse(id, transactionHash)

    private fun createSignPayloadResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        signature: String = "signature",
    ): Pair<SignPayloadV1BeaconResponse, SignPayloadBeaconResponse> =
        SignPayloadV1BeaconResponse(version, id, beaconId, signature) to SignPayloadBeaconResponse(id, signature)

    private fun createBroadcastResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash"
    ): Pair<BroadcastV1BeaconResponse, BroadcastBeaconResponse> =
        BroadcastV1BeaconResponse(version, id, beaconId, transactionHash) to BroadcastBeaconResponse(id, transactionHash)

    // -- other to BeaconMessage --

    private fun createDisconnectPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
    ): Pair<DisconnectV1BeaconMessage, DisconnectBeaconMessage> =
        DisconnectV1BeaconMessage(version, id, beaconId) to DisconnectBeaconMessage(id, beaconId)

    private fun createErrorPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        errorType: BeaconException.Type = BeaconException.Type.Unknown,
    ): Pair<ErrorV1BeaconMessage, ErrorBeaconMessage> =
        ErrorV1BeaconMessage(version, id, beaconId, errorType) to ErrorBeaconMessage(id, beaconId, errorType)
}