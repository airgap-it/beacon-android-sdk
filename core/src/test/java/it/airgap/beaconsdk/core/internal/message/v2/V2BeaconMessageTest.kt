package it.airgap.beaconsdk.core.internal.message.v2

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.blockchain.message.*
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.message.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import mockDependencyRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class V2BeaconMessageTest {

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var storageManager: StorageManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))

        val dependencyRegistry = mockDependencyRegistry()
        every { dependencyRegistry.storageManager } returns storageManager
        every { dependencyRegistry.identifierCreator } returns identifierCreator
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `is deserialized from JSON`() {
        messagesWithJsonStrings()
            .map { Json.decodeFromString<V2BeaconMessage>(it.second) to it.first }
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
            .map { V2BeaconMessage.from(senderId, it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `converts to Beacon message`() {
        val senderId = "senderId"
        val otherId = "otherId"
        val origin = Origin.P2P(senderId)

        val matchingAppMetadata = MockAppMetadata(senderId, "v2App")
        val otherAppMetadata = MockAppMetadata(otherId, "v2OtherApp")

        runBlocking { storageManager.setAppMetadata(listOf(otherAppMetadata, matchingAppMetadata)) }

        runBlocking {
            versionedWithBeacon(senderId = senderId, appMetadata = matchingAppMetadata, origin = origin)
                .map { it.first.toBeaconMessage(origin) to it.second }
                .forEach {
                    assertEquals(it.second, it.first)
                }
        }
    }

    // -- message to JSON --

    private fun messagesWithJsonStrings() =
        listOf(
            createPermissionRequestJsonPair(),
            createPermissionRequestJsonPair(scopes = listOf("sign")),
            createOperationRequestJsonPair(),
            createSignPayloadRequestJsonPair(),
            createBroadcastRequestJsonPair(),

            createPermissionResponseJsonPair(),
            createOperationResponseJsonPair(),
            createSignPayloadResponseJsonPair(),
            createBroadcastResponseJsonPair(),

            createDisconnectJsonPair(),
            createErrorJsonPair(),
        )

    // -- V2BeaconMessage to BeaconMessage --

    private fun versionedWithBeacon(
        version: String = "2",
        senderId: String = "senderId",
        origin: Origin = Origin.P2P(senderId),
        appMetadata: AppMetadata? = null,
    ): List<Pair<V2BeaconMessage, BeaconMessage>> =
        listOf(
            createPermissionRequestPair(version = version, senderId = senderId, origin = origin),
            createOperationRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin),
            createSignPayloadRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin),
            createBroadcastRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin),

            createPermissionResponsePair(version = version, senderId = senderId, origin = origin),
            createOperationResponsePair(version = version, senderId = senderId, origin = origin),
            createSignPayloadResponsePair(version = version, senderId = senderId, origin = origin),
            createBroadcastResponsePair(version = version, senderId = senderId, origin = origin),

            createDisconnectPair(version = version, senderId = senderId, origin = origin),
            createErrorResponsePair(version = version, senderId = senderId, origin = origin),
        )

    // -- request to JSON --

    private fun createPermissionRequestJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        appMetadata: MockAppMetadata = MockAppMetadata("senderId", "v2App"),
        network: Network = MockNetwork(),
        scopes: List<String> = emptyList()
    ): Pair<V2MockPermissionBeaconRequest, String> =
        V2MockPermissionBeaconRequest(
            "permission_request",
            version,
            id,
            senderId,
            appMetadata,
            mapOf(
                "network" to Json.encodeToJsonElement(network),
                "scopes" to Json.encodeToJsonElement(scopes),
            ),
        ) to """
            {
                "type": "permission_request",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "appMetadata": ${Json.encodeToString(appMetadata)},
                "network": ${Json.encodeToString(network)},
                "scopes": ${Json.encodeToString(scopes)}
            }
        """.trimIndent()

    private fun createOperationRequestJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: Network = MockNetwork(),
        tezosOperations: List<Map<String, String>> = emptyList(),
        sourceAddress: String = "sourceAddress"
    ): Pair<V2MockBlockchainBeaconMessage, String> =
        V2MockBlockchainBeaconMessage.request(
            "operation_request",
            version,
            id,
            senderId,
            mapOf(
                "network" to Json.encodeToJsonElement(network),
                "operationDetails" to Json.encodeToJsonElement(tezosOperations),
                "sourceAddress" to JsonPrimitive(sourceAddress),
            ),
        ) to """
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
    ): Pair<V2MockBlockchainBeaconMessage, String> =
        V2MockBlockchainBeaconMessage.request(
            "sign_payload_request",
            version,
            id,
            senderId,
            mapOf(
                "signingType" to Json.encodeToJsonElement(signingType),
                "payload" to JsonPrimitive(payload),
                "sourceAddress" to JsonPrimitive(sourceAddress),
            )
        ) to """
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
        network: Network = MockNetwork(),
        signedTransaction: String = "signedTransaction"
    ): Pair<V2MockBlockchainBeaconMessage, String> =
        V2MockBlockchainBeaconMessage.request(
            "broadcast_request",
            version,
            id,
            senderId,
            mapOf(
                "network" to Json.encodeToJsonElement(network),
                "signedTransaction" to JsonPrimitive(signedTransaction),
            )
        ) to """
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

    private fun createPermissionResponseJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        publicKey: String = "publicKey",
        network: Network = MockNetwork(),
        scopes: List<String> = emptyList(),
    ): Pair<V2MockPermissionBeaconResponse, String> {
        return V2MockPermissionBeaconResponse(
            "permission_response",
            version,
            id,
            senderId,
            mapOf(
                "publicKey" to JsonPrimitive(publicKey),
                "network" to Json.encodeToJsonElement(network),
                "scopes" to Json.encodeToJsonElement(scopes),
            ),
        ) to """
            {
                "type": "permission_response",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "publicKey": "$publicKey",
                "network": ${Json.encodeToString(network)},
                "scopes": ${Json.encodeToString(scopes)}
            }
        """.trimIndent()
    }

    private fun createOperationResponseJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash"
    ): Pair<V2MockBlockchainBeaconMessage, String> =
        V2MockBlockchainBeaconMessage.response(
            "operation_response",
            version,
            id,
            senderId,
            mapOf(
                "transactionHash" to JsonPrimitive(transactionHash),
            ),
        ) to """
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
    ): Pair<V2MockBlockchainBeaconMessage, String> =
        V2MockBlockchainBeaconMessage.response(
            "sign_payload_response",
            version,
            id,
            senderId,
            mapOf(
                "signingType" to Json.encodeToJsonElement(signingType),
                "signature" to JsonPrimitive(signature),
            ),
        ) to """
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
    ): Pair<V2MockBlockchainBeaconMessage, String> =
        V2MockBlockchainBeaconMessage.response(
            "broadcast_response",
            version,
            id,
            senderId,
            mapOf(
                "transactionHash" to JsonPrimitive(transactionHash),
            )
        ) to """
            {
                "type": "broadcast_response",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "transactionHash": "$transactionHash"
            }
        """.trimIndent()

    // -- other to JSON --

    private fun createDisconnectJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
    ): Pair<DisconnectV2BeaconMessage, String> =
        DisconnectV2BeaconMessage(version, id, senderId) to """
            {
                "type": "disconnect",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId"
            }
        """.trimIndent()

    private fun createErrorJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        errorType: BeaconError = BeaconError.Unknown,
    ): Pair<ErrorV2BeaconResponse, String> =
        ErrorV2BeaconResponse(version, id, senderId, errorType) to """
            {
                "type": "error",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "errorType": ${Json.encodeToString(errorType)}
            }
        """.trimIndent()

    // -- request to BeaconMessage --

    private fun createPermissionRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        appMetadata: MockAppMetadata = MockAppMetadata("senderId", "v2App"),
        network: Network = MockNetwork(),
        scopes: List<String> = emptyList(),
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V2MockPermissionBeaconRequest, PermissionBeaconRequest> {
        val type = "permission_request"
        val rest = mapOf(
            "network" to Json.encodeToJsonElement(network),
            "scopes" to Json.encodeToJsonElement(scopes),
        )

        return (
            V2MockPermissionBeaconRequest(type, version, id, senderId, appMetadata, rest)
                to PermissionMockRequest(type, id, version, MockBlockchain.IDENTIFIER, senderId, origin, appMetadata, rest)
        )
    }

    private fun createOperationRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: Network = MockNetwork(),
        tezosOperations: List<Map<String, String>> = emptyList(),
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V2MockBlockchainBeaconMessage, BlockchainBeaconRequest> {
        val type = "operation_request"
        val rest = mapOf(
            "network" to Json.encodeToJsonElement(network),
            "operationDetails" to Json.encodeToJsonElement(tezosOperations),
            "sourceAddress" to JsonPrimitive(sourceAddress),
        )

        return (
            V2MockBlockchainBeaconMessage.request(type, version, id, senderId, rest)
                to BlockchainMockRequest(type, id, version, MockBlockchain.IDENTIFIER, senderId, appMetadata, origin, null, rest)
        )
    }

    private fun createSignPayloadRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V2MockBlockchainBeaconMessage, BlockchainBeaconRequest> {
        val type = "sign_payload_request"
        val rest = mapOf(
            "signingType" to Json.encodeToJsonElement(signingType),
            "payload" to JsonPrimitive(payload),
            "sourceAddress" to JsonPrimitive(sourceAddress),
        )

        return (
            V2MockBlockchainBeaconMessage.request(type, version, id, senderId, rest)
                to BlockchainMockRequest(type, id, version, MockBlockchain.IDENTIFIER, senderId, appMetadata, origin, null, rest)
        )
    }

    private fun createBroadcastRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: Network = MockNetwork(),
        signedTransaction: String = "signedTransaction",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V2MockBlockchainBeaconMessage, BlockchainBeaconRequest> {
        val type = "broadcast_request"
        val rest = mapOf(
            "network" to Json.encodeToJsonElement(network),
            "signedTransaction" to JsonPrimitive(signedTransaction),
        )

        return (
            V2MockBlockchainBeaconMessage.request(type, version, id, senderId, rest)
                to BlockchainMockRequest(type, id, version, MockBlockchain.IDENTIFIER, senderId, appMetadata, origin, null, rest)
        )
    }

    // -- response to BeaconMessage --

    private fun createPermissionResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        publicKey: String = "publicKey",
        network: Network = MockNetwork(),
        scopes: List<String> = emptyList(),
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V2MockPermissionBeaconResponse, PermissionBeaconResponse> {
        val type = "permission_response"
        val rest = mapOf(
            "publicKey" to Json.encodeToJsonElement(publicKey),
            "network" to Json.encodeToJsonElement(network),
            "scopes" to Json.encodeToJsonElement(scopes),
        )

        return (
            V2MockPermissionBeaconResponse(type, version, id, senderId, rest)
                to PermissionMockResponse(type, id, version, origin, MockBlockchain.IDENTIFIER, rest)
        )
    }

    private fun createOperationResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V2MockBlockchainBeaconMessage, BlockchainBeaconResponse> {
        val type = "operation_response"
        val rest = mapOf(
            "transactionHash" to JsonPrimitive(transactionHash),
        )

        return (
            V2MockBlockchainBeaconMessage.response(type, version, id, senderId, rest)
                to BlockchainMockResponse(type, id, version, origin, MockBlockchain.IDENTIFIER, rest)
        )
    }

    private fun createSignPayloadResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        signature: String = "signature",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V2MockBlockchainBeaconMessage, BlockchainBeaconResponse> {
        val type = "sign_payload_response"
        val rest = mapOf(
            "signingType" to Json.encodeToJsonElement(signingType),
            "signature" to JsonPrimitive(signature),
        )

        return (
            V2MockBlockchainBeaconMessage.response(type, version, id, senderId, rest)
                to BlockchainMockResponse(type, id, version, origin, MockBlockchain.IDENTIFIER, rest)
        )
    }

    private fun createBroadcastResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V2MockBlockchainBeaconMessage, BlockchainBeaconResponse> {
        val type = "broadcast_response"
        val rest = mapOf(
            "transactionHash" to JsonPrimitive(transactionHash),
        )

        return (
            V2MockBlockchainBeaconMessage.response(type, version, id, senderId, rest)
                to BlockchainMockResponse(type, id, version, origin, MockBlockchain.IDENTIFIER, rest)
        )
    }

    private fun createErrorResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        errorType: BeaconError = BeaconError.Unknown,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<ErrorV2BeaconResponse, ErrorBeaconResponse> =
        ErrorV2BeaconResponse(version, id, senderId, errorType) to ErrorBeaconResponse(id, version, origin, errorType, null)

    // -- other to BeaconMessage --

    private fun createDisconnectPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<DisconnectV2BeaconMessage, DisconnectBeaconMessage> =
        DisconnectV2BeaconMessage(version, id, senderId) to DisconnectBeaconMessage(id, senderId, version, origin)
}