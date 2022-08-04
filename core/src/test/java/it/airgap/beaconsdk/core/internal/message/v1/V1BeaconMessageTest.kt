package it.airgap.beaconsdk.core.internal.message.v1

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.blockchain.message.*
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.serializer.contextualJson
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.message.*
import it.airgap.beaconsdk.core.scope.BeaconScope
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
import v1BeaconMessageContext
import kotlin.test.assertEquals

internal class V1BeaconMessageTest {

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var dependencyRegistry: DependencyRegistry
    private lateinit var storageManager: StorageManager
    private lateinit var json: Json

    private val beaconScope: BeaconScope = BeaconScope.Global

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        storageManager = StorageManager(beaconScope, MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))

        dependencyRegistry = mockDependencyRegistry()
        every { dependencyRegistry.storageManager } returns storageManager
        every { dependencyRegistry.identifierCreator } returns identifierCreator
        every { dependencyRegistry.compat } returns CoreCompat(beaconScope)

        json = contextualJson(dependencyRegistry.blockchainRegistry, dependencyRegistry.compat)
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `is deserialized from JSON`() {
        messagesWithJsonStrings()
            .map { json.decodeFromString<V1BeaconMessage>(it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `serializes to JSON`() {
        messagesWithJsonStrings()
            .map { json.decodeFromString(JsonObject.serializer(), json.encodeToString(it.first)) to
                    json.decodeFromString(JsonObject.serializer(), it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `is created from Beacon message`() {
        val version = "1"
        val senderId = "senderId"

        versionedWithBeacon(version, senderId)
            .map { V1BeaconMessage.from(senderId, it.second, dependencyRegistry.v1BeaconMessageContext) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `converts to Beacon message`() {
        val senderId = "senderId"
        val otherId = "otherId"
        val origin = Origin.P2P("v1App")

        val matchingAppMetadata = MockAppMetadata(senderId, "v1App")
        val otherAppMetadata = MockAppMetadata(otherId, "v1OtherApp")

        runBlocking { storageManager.setAppMetadata(listOf(otherAppMetadata, matchingAppMetadata)) }

        runBlocking {
            versionedWithBeacon(beaconId = senderId, appMetadata = matchingAppMetadata, origin = origin)
                .map { it.first.toBeaconMessage(origin, beaconScope) to it.second }
                .forEach {
                    assertEquals(it.second, it.first)
                }
        }
    }

    // -- message to JSON --

    private fun messagesWithJsonStrings() = listOf(
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

    // -- V1BeaconMessage to BeaconMessage --

    private fun versionedWithBeacon(
        version: String = "1",
        beaconId: String = "beaconId",
        origin: Origin = Origin.P2P(beaconId),
        appMetadata: AppMetadata? = null,
    ): List<Pair<V1BeaconMessage, BeaconMessage>> =
        listOf(
            createPermissionRequestPair(version = version, beaconId = beaconId, origin = origin),
            createOperationRequestPair(version = version, beaconId = beaconId, appMetadata = appMetadata, origin = origin),
            createSignPayloadRequestPair(version = version, beaconId = beaconId, appMetadata = appMetadata, origin = origin),
            createBroadcastRequestPair(version = version, beaconId = beaconId, appMetadata = appMetadata, origin = origin),

            createPermissionResponsePair(version = version, beaconId = beaconId, origin = origin),
            createOperationResponsePair(version = version, beaconId = beaconId, origin = origin),
            createSignPayloadResponsePair(version = version, beaconId = beaconId, origin = origin),
            createBroadcastResponsePair(version = version, beaconId = beaconId, origin = origin),

            createDisconnectPair(version = version, beaconId = beaconId, origin = origin),
            createErrorResponsePair(version = version, beaconId = beaconId, origin = origin),
        )

    // -- request to JSON --

    private fun createPermissionRequestJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        appMetadata: MockAppMetadata = MockAppMetadata("beaconId", "v1App"),
        network: Network = MockNetwork(),
        scopes: List<String> = emptyList()
    ): Pair<V1MockPermissionBeaconRequest, String> =
        V1MockPermissionBeaconRequest(
            "permission_request",
            version,
            id,
            beaconId,
            appMetadata,
            mapOf(
                "network" to json.encodeToJsonElement(network),
                "scopes" to json.encodeToJsonElement(scopes),
            ),
        ) to """
            {
                "type": "permission_request",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "appMetadata": ${json.encodeToString(appMetadata)},
                "network": ${json.encodeToString(network)},
                "scopes": ${json.encodeToString(scopes)}
            }
        """.trimIndent()

    private fun createOperationRequestJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: Network = MockNetwork(),
        tezosOperations: List<Map<String, String>> = emptyList(),
        sourceAddress: String = "sourceAddress"
    ): Pair<V1MockBlockchainBeaconMessage, String> =
        V1MockBlockchainBeaconMessage.request(
            "operation_request",
            version,
            id,
            beaconId,
            mapOf(
                "network" to json.encodeToJsonElement(network),
                "operationDetails" to json.encodeToJsonElement(tezosOperations),
                "sourceAddress" to JsonPrimitive(sourceAddress),
            ),
        ) to """
            {
                "type": "operation_request",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "network": ${json.encodeToString(network)},
                "operationDetails": ${json.encodeToString(tezosOperations)},
                "sourceAddress": "$sourceAddress"
            }
        """.trimIndent()

    private fun createSignPayloadRequestJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
    ): Pair<V1MockBlockchainBeaconMessage, String> =
        V1MockBlockchainBeaconMessage.request(
            "sign_payload_request",
            version,
            id,
            beaconId,
            mapOf(
                "payload" to JsonPrimitive(payload),
                "sourceAddress" to JsonPrimitive(sourceAddress),
            ),
        ) to """
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
        network: Network = MockNetwork(),
        signedTransaction: String = "signedTransaction"
    ): Pair<V1MockBlockchainBeaconMessage, String> =
        V1MockBlockchainBeaconMessage.request(
            "broadcast_request",
            version,
            id,
            beaconId,
            mapOf(
                "network" to json.encodeToJsonElement(network),
                "signedTransaction" to JsonPrimitive(signedTransaction),
            ),
        ) to """
            {
                "type": "broadcast_request",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "network": ${json.encodeToString(network)},
                "signedTransaction": "$signedTransaction"
            }
        """.trimIndent()

    // -- response to JSON --

    private fun createPermissionResponseJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        publicKey: String = "publicKey",
        network: Network = MockNetwork(),
        scopes: List<String> = emptyList(),
    ): Pair<V1MockPermissionBeaconResponse, String> =
        V1MockPermissionBeaconResponse(
            "permission_response",
            version,
            id,
            beaconId,
            mapOf(
                "publicKey" to json.encodeToJsonElement(publicKey),
                "network" to json.encodeToJsonElement(network),
                "scopes" to json.encodeToJsonElement(scopes),
            ),
        ) to """
            {
                "type": "permission_response",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "publicKey": "$publicKey",
                "network": ${json.encodeToString(network)},
                "scopes": ${json.encodeToString(scopes)}
            }
        """.trimIndent()

    private fun createOperationResponseJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash"
    ): Pair<V1MockBlockchainBeaconMessage, String> =
        V1MockBlockchainBeaconMessage.response(
            "operation_response",
            version,
            id,
            beaconId,
            mapOf(
                "transactionHash" to JsonPrimitive(transactionHash)
            ),
        ) to """
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
    ): Pair<V1MockBlockchainBeaconMessage, String> =
        V1MockBlockchainBeaconMessage.response(
            "sign_payload_response",
            version,
            id,
            beaconId,
            mapOf(
                "signature" to JsonPrimitive(signature),
            ),
        ) to """
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
    ): Pair<V1MockBlockchainBeaconMessage, String> =
        V1MockBlockchainBeaconMessage.response(
            "broadcast_response",
            version,
            id,
            beaconId,
            mapOf(
                "transactionHash" to JsonPrimitive(transactionHash),
            ),
        ) to """
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
        errorType: BeaconError = BeaconError.Unknown,
    ): Pair<ErrorV1BeaconResponse, String> =
        ErrorV1BeaconResponse(version, id, beaconId, errorType) to """
            {
                "type": "error",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "errorType": ${json.encodeToString(errorType)}
            }
        """.trimIndent()

    // -- request to BeaconMessage --

    private fun createPermissionRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        appMetadata: MockAppMetadata = MockAppMetadata("beaconId", "v1App"),
        network: Network = MockNetwork(),
        scopes: List<String> = emptyList(),
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<V1MockPermissionBeaconRequest, PermissionBeaconRequest> {
        val type = "permission_request"
        val rest = mapOf(
            "network" to json.encodeToJsonElement(network),
            "scopes" to json.encodeToJsonElement(scopes),
        )

        return (
            V1MockPermissionBeaconRequest(type, version, id, beaconId, appMetadata, rest)
                to PermissionMockRequest(type, id, version, MockBlockchain.IDENTIFIER, beaconId, origin, appMetadata, rest)
        )
    }

    private fun createOperationRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: Network = MockNetwork(),
        tezosOperations: List<Map<String, String>> = emptyList(),
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<V1MockBlockchainBeaconMessage, BlockchainBeaconRequest> {
        val type = "operation_request"
        val rest = mapOf(
            "network" to json.encodeToJsonElement(network),
            "operationDetails" to json.encodeToJsonElement(tezosOperations),
            "sourceAddress" to JsonPrimitive(sourceAddress),
        )

        return (
            V1MockBlockchainBeaconMessage.request(type, version, id, beaconId, rest)
                to BlockchainMockRequest(type, id, version, MockBlockchain.IDENTIFIER, beaconId, appMetadata, origin, null, rest)
        )
    }

    private fun createSignPayloadRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<V1MockBlockchainBeaconMessage, BlockchainBeaconRequest> {
        val type = "sign_payload_request"
        val rest = mapOf(
            "payload" to JsonPrimitive(payload),
            "sourceAddress" to JsonPrimitive(sourceAddress),
        )

        return (
            V1MockBlockchainBeaconMessage.request(type, version, id, beaconId, rest)
                to BlockchainMockRequest(type, id, version, MockBlockchain.IDENTIFIER, beaconId, appMetadata, origin, null, rest)
        )
    }

    private fun createBroadcastRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: Network = MockNetwork(),
        signedTransaction: String = "signedTransaction",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(beaconId),
        accountId: String? = null,
    ): Pair<V1MockBlockchainBeaconMessage, BlockchainBeaconRequest> {
        val type = "broadcast_request"
        val rest = mapOf(
            "network" to json.encodeToJsonElement(network),
            "signedTransaction" to JsonPrimitive(signedTransaction),
        )

        return (
            V1MockBlockchainBeaconMessage.request(type, version, id, beaconId, rest)
                to BlockchainMockRequest(type, id, version, MockBlockchain.IDENTIFIER, beaconId, appMetadata, origin, accountId, rest)
        )
    }

    // -- response to BeaconMessage --

    private fun createPermissionResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        publicKey: String = "publicKey",
        network: Network = MockNetwork(),
        scopes: List<String> = emptyList(),
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<V1MockPermissionBeaconResponse, PermissionBeaconResponse> {
        val type = "permission_response"
        val rest = mapOf(
            "publicKey" to JsonPrimitive(publicKey),
            "network" to json.encodeToJsonElement(network),
            "scopes" to json.encodeToJsonElement(scopes),
        )

        return (
            V1MockPermissionBeaconResponse(type, version, id, beaconId, rest)
                to PermissionMockResponse(type, id, version, origin, MockBlockchain.IDENTIFIER, rest)
        )
    }

    private fun createOperationResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<V1MockBlockchainBeaconMessage, BlockchainBeaconResponse> {
        val type = "operation_response"
        val rest = mapOf(
            "transactionHash" to JsonPrimitive(transactionHash),
        )

        return (
            V1MockBlockchainBeaconMessage.response(type, version, id, beaconId, rest)
                to BlockchainMockResponse(type, id, version, origin, MockBlockchain.IDENTIFIER, rest)
        )
    }

    private fun createSignPayloadResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        signature: String = "signature",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<V1MockBlockchainBeaconMessage, BlockchainBeaconResponse> {
        val type = "sign_payload_response"
        val rest = mapOf(
            "signature" to JsonPrimitive(signature),
        )

        return (
            V1MockBlockchainBeaconMessage.response(type, version, id, beaconId, rest,)
                to BlockchainMockResponse(type, id, version, origin, MockBlockchain.IDENTIFIER, rest)
        )
    }

    private fun createBroadcastResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<V1MockBlockchainBeaconMessage, BlockchainBeaconResponse> {
        val type = "broadcast_response"
        val rest = mapOf(
            "transactionHash" to JsonPrimitive(transactionHash),
        )

        return (
            V1MockBlockchainBeaconMessage.response(type, version, id, beaconId, rest)
                to BlockchainMockResponse(type, id, version, origin, MockBlockchain.IDENTIFIER, rest)
        )
    }

    private fun createErrorResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        errorType: BeaconError = BeaconError.Unknown,
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<ErrorV1BeaconResponse, ErrorBeaconResponse> =
        ErrorV1BeaconResponse(version, id, beaconId, errorType) to ErrorBeaconResponse(id, version, origin, errorType, null)

    // -- other to BeaconMessage --

    private fun createDisconnectPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<DisconnectV1BeaconMessage, DisconnectBeaconMessage> =
        DisconnectV1BeaconMessage(version, id, beaconId) to DisconnectBeaconMessage(id, beaconId, version, origin)
}