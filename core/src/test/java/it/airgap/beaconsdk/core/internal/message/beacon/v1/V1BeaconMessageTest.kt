package it.airgap.beaconsdk.core.internal.message.beacon.v1

import fromValues
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import it.airgap.beaconsdk.core.data.beacon.*
import it.airgap.beaconsdk.core.data.tezos.TezosEndorsementOperation
import it.airgap.beaconsdk.core.data.tezos.TezosOperation
import it.airgap.beaconsdk.core.internal.chain.ChainRegistry
import it.airgap.beaconsdk.core.internal.chain.MockChain
import it.airgap.beaconsdk.core.internal.chain.MockChainVersionedMessage
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.v1.*
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.AccountUtils
import it.airgap.beaconsdk.core.message.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import mockBeaconSdk
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class V1BeaconMessageTest {

    @MockK
    private lateinit var dependencyRegistry: DependencyRegistry

    @MockK
    private lateinit var chainRegistry: ChainRegistry

    @MockK
    private lateinit var accountUtils: AccountUtils

    private lateinit var storageManager: StorageManager

    private val mockChain: MockChain = MockChain()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockBeaconSdk(dependencyRegistry = dependencyRegistry)

        every { dependencyRegistry.chainRegistry } returns chainRegistry
        every { chainRegistry.get("tezos") } returns mockChain

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), accountUtils)
    }

    @After
    fun cleanUp() {
        unmockkAll()
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
    fun `is created from Beacon message`() {
        val version = "1"
        val senderId = "senderId"

        versionedWithBeacon(version, senderId)
            .map { V1BeaconMessage.from(senderId, it.second) to it.first }
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
        createPermissionRequestJsonPair(),
        createPermissionRequestJsonPair(scopes = listOf(Permission.Scope.Sign)),
        createOperationRequestJsonPair(),
        createOperationRequestJsonPair(tezosOperations = listOf(TezosEndorsementOperation("level"))),
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
        origin: Origin = Origin.P2P(beaconId),
        appMetadata: AppMetadata? = null,
    ): List<Pair<V1BeaconMessage, BeaconMessage>> =
        listOf(
            createPermissionRequestPair(version = version, beaconId = beaconId, origin = origin),
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
        appMetadata: V1AppMetadata = V1AppMetadata("beaconId", "v1App"),
        network: Network = Network.Custom(),
        scopes: List<Permission.Scope> = emptyList()
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
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress"
    ): Pair<MockChainVersionedMessage.V1MockBeaconMessage, String> =
        MockChainVersionedMessage.V1MockBeaconMessage.request(
            "operation_request",
            version,
            id,
            beaconId,
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
    ): Pair<MockChainVersionedMessage.V1MockBeaconMessage, String> =
        MockChainVersionedMessage.V1MockBeaconMessage.request(
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
        network: Network = Network.Custom(),
        signedTransaction: String = "signedTransaction"
    ): Pair<MockChainVersionedMessage.V1MockBeaconMessage, String> =
        MockChainVersionedMessage.V1MockBeaconMessage.request(
            "broadcast_request",
            version,
            id,
            beaconId,
            mapOf(
                "network" to Json.encodeToJsonElement(network),
                "signedTransaction" to JsonPrimitive(signedTransaction),
            ),
        ) to """
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
        scopes: List<Permission.Scope> = emptyList(),
        threshold: Threshold? = null,
        includeNulls: Boolean = false,
    ): Pair<PermissionV1BeaconResponse, String> {
        val values = mapOf(
            "type" to "permission_response",
            "version" to version,
            "id" to id,
            "beaconId" to beaconId,
            "publicKey" to publicKey,
            "network" to Json.encodeToJsonElement(network),
            "scopes" to Json.encodeToJsonElement(scopes),
            "threshold" to threshold?.let { Json.encodeToJsonElement(it) }
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return PermissionV1BeaconResponse(version, id, beaconId, publicKey, network, scopes, threshold) to json
    }

    private fun createOperationResponseJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash"
    ): Pair<MockChainVersionedMessage.V1MockBeaconMessage, String> =
        MockChainVersionedMessage.V1MockBeaconMessage.response(
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
    ): Pair<MockChainVersionedMessage.V1MockBeaconMessage, String> =
        MockChainVersionedMessage.V1MockBeaconMessage.response(
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
    ): Pair<MockChainVersionedMessage.V1MockBeaconMessage, String> =
        MockChainVersionedMessage.V1MockBeaconMessage.response(
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
        scopes: List<Permission.Scope> = emptyList(),
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<PermissionV1BeaconRequest, PermissionBeaconRequest> =
        PermissionV1BeaconRequest(version, id, beaconId, appMetadata, network, scopes) to
                PermissionBeaconRequest(id, beaconId, appMetadata.toAppMetadata(), network, scopes, origin, version)

    private fun createOperationRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: Network = Network.Custom(),
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<MockChainVersionedMessage.V1MockBeaconMessage, ChainBeaconRequest> {
        val type = "operation_request"
        val payload = mapOf(
            "network" to Json.encodeToJsonElement(network),
            "operationDetails" to Json.encodeToJsonElement(tezosOperations),
            "sourceAddress" to JsonPrimitive(sourceAddress),
        )

        return MockChainVersionedMessage.V1MockBeaconMessage.request(
            type,
            version,
            id,
            beaconId,
            payload,
        ) to ChainBeaconRequest(
            id,
            beaconId,
            appMetadata,
            MockChain.IDENTIFIER,
            MockChainVersionedMessage.RequestPayload(type, payload),
            origin,
            version,
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
    ): Pair<MockChainVersionedMessage.V1MockBeaconMessage, ChainBeaconRequest> {
        val type = "sign_payload_request"
        val payload = mapOf(
            "payload" to JsonPrimitive(payload),
            "sourceAddress" to JsonPrimitive(sourceAddress),
        )

        return MockChainVersionedMessage.V1MockBeaconMessage.request(
            type,
            version,
            id,
            beaconId,
            payload
        ) to ChainBeaconRequest(
            id,
            beaconId,
            appMetadata,
            MockChain.IDENTIFIER,
            MockChainVersionedMessage.RequestPayload(type, payload),
            origin,
            version,
        )
    }

    private fun createBroadcastRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: Network = Network.Custom(),
        signedTransaction: String = "signedTransaction",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<MockChainVersionedMessage.V1MockBeaconMessage, ChainBeaconRequest> {
        val type = "broadcast_request"
        val payload = mapOf(
            "network" to Json.encodeToJsonElement(network),
            "signedTransaction" to JsonPrimitive(signedTransaction),
        )

        return MockChainVersionedMessage.V1MockBeaconMessage.request(
            type,
            version,
            id,
            beaconId,
            payload,
        ) to ChainBeaconRequest(
            id,
            beaconId,
            appMetadata,
            MockChain.IDENTIFIER,
            MockChainVersionedMessage.RequestPayload(type, payload),
            origin,
            version,
        )
    }

    // -- response to BeaconMessage --

    private fun createPermissionResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        publicKey: String = "publicKey",
        network: Network = Network.Custom(),
        scopes: List<Permission.Scope> = emptyList(),
        threshold: Threshold? = null,
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<PermissionV1BeaconResponse, PermissionBeaconResponse> =
        PermissionV1BeaconResponse(version, id, beaconId, publicKey, network, scopes, threshold) to
                PermissionBeaconResponse(id, publicKey, network, scopes, threshold, version, origin)

    private fun createOperationResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<MockChainVersionedMessage.V1MockBeaconMessage, ChainBeaconResponse> {
        val type = "operation_response"
        val payload = mapOf(
            "transactionHash" to JsonPrimitive(transactionHash),
        )

        return MockChainVersionedMessage.V1MockBeaconMessage.response(
            type,
            version,
            id,
            beaconId,
            payload
        ) to ChainBeaconResponse(
            id,
            MockChain.IDENTIFIER,
            MockChainVersionedMessage.ResponsePayload(type, payload),
            version,
            origin,
        )
    }

    private fun createSignPayloadResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        signature: String = "signature",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<MockChainVersionedMessage.V1MockBeaconMessage, ChainBeaconResponse> {
        val type = "sign_payload_response"
        val payload = mapOf(
            "signature" to JsonPrimitive(signature),
        )

        return MockChainVersionedMessage.V1MockBeaconMessage.response(
            type,
            version,
            id,
            beaconId,
            payload,
        ) to ChainBeaconResponse(
            id,
            MockChain.IDENTIFIER,
            MockChainVersionedMessage.ResponsePayload(type, payload),
            version,
            origin,
        )
    }

    private fun createBroadcastResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<MockChainVersionedMessage.V1MockBeaconMessage, ChainBeaconResponse> {
        val type = "broadcast_response"
        val payload = mapOf(
            "transactionHash" to JsonPrimitive(transactionHash),
        )

        return MockChainVersionedMessage.V1MockBeaconMessage.response(
            type,
            version,
            id,
            beaconId,
            payload
        ) to ChainBeaconResponse(
            id,
            MockChain.IDENTIFIER,
            MockChainVersionedMessage.ResponsePayload(type, payload),
            version,
            origin,
        )
    }

    private fun createErrorResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        errorType: BeaconError = BeaconError.Unknown,
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<ErrorV1BeaconResponse, ErrorBeaconResponse> =
        ErrorV1BeaconResponse(version, id, beaconId, errorType) to ErrorBeaconResponse(id, errorType, version, origin)

    // -- other to BeaconMessage --

    private fun createDisconnectPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<DisconnectV1BeaconMessage, DisconnectBeaconMessage> =
        DisconnectV1BeaconMessage(version, id, beaconId) to DisconnectBeaconMessage(id, beaconId, version, origin)
}