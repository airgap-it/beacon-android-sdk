package it.airgap.beaconsdk.core.internal.message.v3

import fromValues
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
import v3BeaconMessageContext
import kotlin.test.assertEquals

internal class V3BeaconMessageTest {

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

        json = contextualJson(dependencyRegistry.blockchainRegistry, CoreCompat(beaconScope))
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `is deserialized from JSON`() {
        messagesWithJsonStrings()
            .map { json.decodeFromString<V3BeaconMessage>(it.second) to it.first }
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
        val version = "3"
        val senderId = "senderId"

        versionedWithBeacon(version, senderId)
            .map { V3BeaconMessage.from(senderId, it.second, dependencyRegistry.v3BeaconMessageContext) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `converts to Beacon message`() {
        val senderId = "senderId"
        val otherId = "otherId"
        val origin = Origin.P2P(senderId)
        val destination = Origin.P2P(otherId)

        val matchingAppMetadata = MockAppMetadata(senderId, "v3App")
        val otherAppMetadata = MockAppMetadata(otherId, "v3OtherApp")

        runBlocking { storageManager.setAppMetadata(listOf(otherAppMetadata, matchingAppMetadata)) }

        runBlocking {
            versionedWithBeacon(senderId = senderId, appMetadata = matchingAppMetadata, origin = origin, destination = destination)
                .map { it.first.toBeaconMessage(origin, destination, beaconScope) to it.second }
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
            createBlockchainRequestJsonPair(),

            createPermissionResponseJsonPair(),
            createBlockchainResponseJsonPair(),

            createDisconnectJsonPair(),
            createErrorJsonPair(),
            createErrorJsonPair(description = null),
        )

    // -- V3BeaconMessage to BeaconMessage --

    private fun versionedWithBeacon(
        version: String = "3",
        senderId: String = "senderId",
        origin: Origin = Origin.P2P(senderId),
        destination: Origin = Origin.P2P("destination"),
        appMetadata: AppMetadata? = null,
    ): List<Pair<V3BeaconMessage, BeaconMessage>> =
        listOf(
            createPermissionRequestPair(version = version, senderId = senderId, origin = origin, destination = destination),
            createBlockchainRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, destination = destination),

            createPermissionResponsePair(version = version, senderId = senderId, destination = destination),
            createBlockchainResponsePair(version = version, senderId = senderId, destination = destination),

            createDisconnectPair(version = version, senderId = senderId, origin = origin, destination = destination),
            createErrorResponsePair(version = version, senderId = senderId, destination = destination),
        )

    // -- request to JSON --

    private fun createPermissionRequestJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
        appMetadata: MockAppMetadata = MockAppMetadata("senderId", "v3App"),
        network: Network = MockNetwork(),
        scopes: List<String> = emptyList()
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconRequestContent(
                blockchainIdentifier,
                V3MockPermissionBeaconRequestData(
                    appMetadata,
                    mapOf(
                        "network" to json.encodeToJsonElement(network),
                        "scopes" to json.encodeToJsonElement(scopes),
                    ),
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "type": "permission_request",
                    "blockchainIdentifier": "$blockchainIdentifier",
                    "blockchainData": {
                        "appMetadata": ${json.encodeToString(appMetadata)},
                        "network": ${json.encodeToString(network)},
                        "scopes": ${json.encodeToString(scopes)}
                    }
                }
            }
        """.trimIndent()

    private fun createBlockchainRequestJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
        message: String = "message",
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                blockchainIdentifier,
                accountId,
                V3MockBlockchainBeaconRequestData(
                    mapOf(
                        "message" to JsonPrimitive(message),
                    ),
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "type": "blockchain_request",
                    "blockchainIdentifier": "$blockchainIdentifier",
                    "accountId": "$accountId",
                    "blockchainData": {
                        "message": "$message"
                    }
                }
            }
        """.trimIndent()

    // -- response to JSON --

    private fun createPermissionResponseJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
        publicKey: String = "publicKey",
        network: Network = MockNetwork(),
        scopes: List<String> = emptyList(),
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconResponseContent(
                blockchainIdentifier,
                V3MockPermissionBeaconResponseData(
                    mapOf(
                        "publicKey" to JsonPrimitive(publicKey),
                        "network" to json.encodeToJsonElement(network),
                        "scopes" to json.encodeToJsonElement(scopes),
                    ),
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "type": "permission_response",
                    "blockchainIdentifier": "$blockchainIdentifier",
                    "blockchainData": {
                        "publicKey": "$publicKey",
                        "network": ${json.encodeToString(network)},
                        "scopes": ${json.encodeToString(scopes)}
                    }
                }
            }
        """.trimIndent()

    private fun createBlockchainResponseJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
        signature: String = "signature",
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconResponseContent(
                blockchainIdentifier,
                V3MockBlockchainBeaconResponseData(
                    mapOf(
                        "signature" to JsonPrimitive(signature),
                    ),
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "type": "blockchain_response",
                    "blockchainIdentifier": "$blockchainIdentifier",
                    "blockchainData": {
                        "signature": "$signature"
                    }
                }
            }
        """.trimIndent()

    // -- other to JSON --

    private fun createDisconnectJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            DisconnectV3BeaconMessageContent,
        ) to """
            {
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "message": {
                    "type": "disconnect"
                }
            }
        """.trimIndent()

    private fun createErrorJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        description: String? = "description",
        errorType: BeaconError = BeaconError.Unknown,
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            ErrorV3BeaconResponseContent(errorType, description),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": ${JsonObject.fromValues(mapOf(
                    "type" to "error",
                    "blockchainIdentifier" to errorType.blockchainIdentifier,
                    "errorType" to json.encodeToJsonElement(errorType),
                    "description" to description,
                ), includeNulls = false)}
            }
        """.trimIndent()

    // -- request to BeaconMessage --

    private fun createPermissionRequestPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
        appMetadata: MockAppMetadata = MockAppMetadata("senderId", "v3App"),
        network: Network = MockNetwork(),
        scopes: List<String> = emptyList(),
        origin: Origin = Origin.P2P(senderId),
        destination: Origin = Origin.P2P("destination"),
    ): Pair<V3BeaconMessage, PermissionBeaconRequest> {
        val rest = mapOf(
            "network" to json.encodeToJsonElement(network),
            "scopes" to json.encodeToJsonElement(scopes),
        )

        return V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconRequestContent(
                blockchainIdentifier,
                V3MockPermissionBeaconRequestData(
                    appMetadata,
                    rest,
                ),
            ),
        ) to PermissionMockRequest("permission_request", id, version, blockchainIdentifier, senderId, origin, destination, appMetadata, rest)
    }

    private fun createBlockchainRequestPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
        accountId: String = "accountId",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
        destination: Origin = Origin.P2P("destination"),
        message: String = "message",
    ): Pair<V3BeaconMessage, BlockchainMockRequest> {
        val rest = mapOf(
            "message" to JsonPrimitive(message),
        )

        return V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                blockchainIdentifier,
                accountId,
                V3MockBlockchainBeaconRequestData(rest),
            ),
        ) to BlockchainMockRequest("blockchain_request", id, version, blockchainIdentifier, senderId, appMetadata, origin, destination, accountId, rest)
    }

    // -- response to BeaconMessage --

    private fun createPermissionResponsePair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
        publicKey: String = "publicKey",
        network: Network = MockNetwork(),
        scopes: List<String> = emptyList(),
        destination: Origin = Origin.P2P("destination"),
    ): Pair<V3BeaconMessage, PermissionBeaconResponse> {
        val rest = mapOf(
            "publicKey" to json.encodeToJsonElement(publicKey),
            "network" to json.encodeToJsonElement(network),
            "scopes" to json.encodeToJsonElement(scopes),
        )

        return V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconResponseContent(
                blockchainIdentifier,
                V3MockPermissionBeaconResponseData(rest),
            ),
        ) to PermissionMockResponse("permission_response", id, version, destination, blockchainIdentifier, rest)
    }

    private fun createBlockchainResponsePair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
        destination: Origin = Origin.P2P("destination"),
        signature: String = "signature",
    ): Pair<V3BeaconMessage, BlockchainBeaconResponse> {
        val rest = mapOf(
            "signature" to JsonPrimitive(signature),
        )

        return V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconResponseContent(
                blockchainIdentifier,
                V3MockBlockchainBeaconResponseData(rest),
            ),
        ) to BlockchainMockResponse("blockchain_response", id, version, destination, blockchainIdentifier, rest)
    }

    private fun createErrorResponsePair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        errorType: BeaconError = BeaconError.Unknown,
        description: String? = "description",
        destination: Origin = Origin.P2P("destination"),
    ): Pair<V3BeaconMessage, ErrorBeaconResponse> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            ErrorV3BeaconResponseContent(
                errorType,
                description,
            ),
        ) to ErrorBeaconResponse(id, version, destination, errorType, description)

    // -- other to BeaconMessage --

    private fun createDisconnectPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        origin: Origin = Origin.P2P(senderId),
        destination: Origin = Origin.P2P("destination"),
    ): Pair<V3BeaconMessage, DisconnectBeaconMessage> =
        V3BeaconMessage(id, version, senderId, DisconnectV3BeaconMessageContent) to DisconnectBeaconMessage(id, senderId, version, origin, destination)
}