package it.airgap.beaconsdk.core.internal.message.beacon.v2

import fromValues
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.internal.chain.ChainRegistry
import it.airgap.beaconsdk.core.internal.chain.MockChain
import it.airgap.beaconsdk.core.internal.chain.MockChainVersionedMessage
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.v2.*
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

internal class V2BeaconMessageTest {

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
        messagesWithJsonStrings() + messagesWithJsonStrings(includeNulls = true)
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
            createPermissionRequestJsonPair(),
            createPermissionRequestJsonPair(scopes = listOf(Permission.Scope.Sign)),
            createOperationRequestJsonPair(),
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
        appMetadata: V2AppMetadata = V2AppMetadata("senderId", "v2App"),
        network: Network = MockNetwork(),
        scopes: List<Permission.Scope> = emptyList()
    ): Pair<PermissionV2BeaconRequest, String> =
        PermissionV2BeaconRequest(version, id, senderId, appMetadata, network, scopes) to """
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
    ): Pair<MockChainVersionedMessage.V2MockBeaconMessage, String> =
        MockChainVersionedMessage.V2MockBeaconMessage.request(
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
    ): Pair<MockChainVersionedMessage.V2MockBeaconMessage, String> =
        MockChainVersionedMessage.V2MockBeaconMessage.request(
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
    ): Pair<MockChainVersionedMessage.V2MockBeaconMessage, String> =
        MockChainVersionedMessage.V2MockBeaconMessage.request(
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
        scopes: List<Permission.Scope> = emptyList(),
        threshold: Threshold? = null,
        includeNulls: Boolean = false,
    ): Pair<PermissionV2BeaconResponse, String> {
        val values = mapOf(
            "type" to "permission_response",
            "version" to version,
            "id" to id,
            "senderId" to senderId,
            "publicKey" to publicKey,
            "network" to Json.encodeToJsonElement(network),
            "scopes" to Json.encodeToJsonElement(scopes),
            "threshold" to threshold?.let { Json.encodeToJsonElement(it) }
        )

        val json = JsonObject.fromValues(values, includeNulls).toString()

        return PermissionV2BeaconResponse(version, id, senderId, publicKey, network, scopes, threshold) to json
    }

    private fun createOperationResponseJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash"
    ): Pair<MockChainVersionedMessage.V2MockBeaconMessage, String> =
        MockChainVersionedMessage.V2MockBeaconMessage.response(
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
    ): Pair<MockChainVersionedMessage.V2MockBeaconMessage, String> =
        MockChainVersionedMessage.V2MockBeaconMessage.response(
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
    ): Pair<MockChainVersionedMessage.V2MockBeaconMessage, String> =
        MockChainVersionedMessage.V2MockBeaconMessage.response(
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
        appMetadata: V2AppMetadata = V2AppMetadata("senderId", "v2App"),
        network: Network = MockNetwork(),
        scopes: List<Permission.Scope> = emptyList(),
        origin: Origin = Origin.P2P(senderId),
    ): Pair<PermissionV2BeaconRequest, PermissionBeaconRequest> =
        PermissionV2BeaconRequest(version, id, senderId, appMetadata, network, scopes) to
                PermissionBeaconRequest(id, senderId, appMetadata.toAppMetadata(), MockChain.IDENTIFIER, network, scopes, origin, version)

    private fun createOperationRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: Network = MockNetwork(),
        tezosOperations: List<Map<String, String>> = emptyList(),
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<MockChainVersionedMessage.V2MockBeaconMessage, ChainBeaconRequest> {
        val type = "operation_request"
        val payload = mapOf(
            "network" to Json.encodeToJsonElement(network),
            "operationDetails" to Json.encodeToJsonElement(tezosOperations),
            "sourceAddress" to JsonPrimitive(sourceAddress),
        )

        return MockChainVersionedMessage.V2MockBeaconMessage.request(
            type,
            version,
            id,
            senderId,
            payload,
        ) to ChainBeaconRequest(
            id,
            senderId,
            appMetadata,
            MockChain.IDENTIFIER,
            MockChainVersionedMessage.RequestPayload(type, payload),
            origin,
            version,
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
    ): Pair<MockChainVersionedMessage.V2MockBeaconMessage, ChainBeaconRequest> {
        val type = "sign_payload_request"
        val payload = mapOf(
            "signingType" to Json.encodeToJsonElement(signingType),
            "payload" to JsonPrimitive(payload),
            "sourceAddress" to JsonPrimitive(sourceAddress),
        )

        return MockChainVersionedMessage.V2MockBeaconMessage.request(
            type,
            version,
            id,
            senderId,
            payload,
        ) to ChainBeaconRequest(
            id,
            senderId,
            appMetadata,
            MockChain.IDENTIFIER,
            MockChainVersionedMessage.RequestPayload(type, payload),
            origin,
            version,
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
    ): Pair<MockChainVersionedMessage.V2MockBeaconMessage, ChainBeaconRequest> {
        val type = "broadcast_request"
        val payload = mapOf(
            "network" to Json.encodeToJsonElement(network),
            "signedTransaction" to JsonPrimitive(signedTransaction),
        )

        return MockChainVersionedMessage.V2MockBeaconMessage.request(
            type,
            version,
            id,
            senderId,
            payload,
        ) to ChainBeaconRequest(
            id,
            senderId,
            appMetadata,
            MockChain.IDENTIFIER,
            MockChainVersionedMessage.RequestPayload(type, payload),
            origin,
            version,
        )
    }

    // -- response to BeaconMessage --

    private fun createPermissionResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        publicKey: String = "publicKey",
        network: Network = MockNetwork(),
        scopes: List<Permission.Scope> = emptyList(),
        threshold: Threshold? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<PermissionV2BeaconResponse, PermissionBeaconResponse> =
        PermissionV2BeaconResponse(version, id, senderId, publicKey, network, scopes, threshold) to
                PermissionBeaconResponse(id, publicKey, MockChain.IDENTIFIER, network, scopes, threshold, version, origin)

    private fun createOperationResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<MockChainVersionedMessage.V2MockBeaconMessage, ChainBeaconResponse> {
        val type = "operation_response"
        val payload = mapOf(
            "transactionHash" to JsonPrimitive(transactionHash),
        )

        return MockChainVersionedMessage.V2MockBeaconMessage.response(
            type,
            version,
            id,
            senderId,
            payload,
        ) to ChainBeaconResponse(
            id,
            MockChain.IDENTIFIER,
            MockChainVersionedMessage.ResponsePayload(type, payload),
            version,
            origin,
        )
    }

    private fun createSignPayloadResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        signature: String = "signature",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<MockChainVersionedMessage.V2MockBeaconMessage, ChainBeaconResponse> {
        val type = "sign_payload_response"
        val payload = mapOf(
            "signingType" to Json.encodeToJsonElement(signingType),
            "signature" to JsonPrimitive(signature),
        )

        return MockChainVersionedMessage.V2MockBeaconMessage.response(
            type,
            version,
            id,
            senderId,
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
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<MockChainVersionedMessage.V2MockBeaconMessage, ChainBeaconResponse> {
        val type = "broadcast_response"
        val payload = mapOf(
            "transactionHash" to JsonPrimitive(transactionHash),
        )

        return MockChainVersionedMessage.V2MockBeaconMessage.response(
            type,
            version,
            id,
            senderId,
            payload,
        ) to ChainBeaconResponse(
            id,
            MockChain.IDENTIFIER,
            MockChainVersionedMessage.ResponsePayload(type, payload),
            version,
            origin,
        )
    }

    private fun createErrorResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        errorType: BeaconError = BeaconError.Unknown,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<ErrorV2BeaconResponse, ErrorBeaconResponse> =
        ErrorV2BeaconResponse(version, id, senderId, errorType) to ErrorBeaconResponse(id, MockChain.IDENTIFIER, errorType, version, origin)

    // -- other to BeaconMessage --

    private fun createDisconnectPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<DisconnectV2BeaconMessage, DisconnectBeaconMessage> =
        DisconnectV2BeaconMessage(version, id, senderId) to DisconnectBeaconMessage(id, senderId, version, origin)
}