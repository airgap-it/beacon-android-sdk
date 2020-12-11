package it.airgap.beaconsdk.internal.message.beacon.v2

import fromValues
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.data.beacon.*
import it.airgap.beaconsdk.data.tezos.TezosEndorsementOperation
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.internal.message.v2.*
import it.airgap.beaconsdk.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.internal.storage.MockStorage
import it.airgap.beaconsdk.internal.storage.StorageManager
import it.airgap.beaconsdk.internal.utils.AccountUtils
import it.airgap.beaconsdk.message.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class V2BeaconMessageTest {

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
            .map { V2BeaconMessage.fromBeaconMessage(senderId, it.second) to it.first }
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
            createOperationRequestPair(
                version = version,
                senderId = senderId,
                tezosOperations = listOf(TezosEndorsementOperation("level")),
                appMetadata = appMetadata,
                origin = origin,
            ),
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
        network: Network = Network.Custom(),
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
        network: Network = Network.Custom(),
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress"
    ): Pair<OperationV2BeaconRequest, String> =
        OperationV2BeaconRequest(version, id, senderId, network, tezosOperations, sourceAddress) to """
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
    ): Pair<SignPayloadV2BeaconRequest, String> =
        SignPayloadV2BeaconRequest(version, id, senderId, signingType, payload, sourceAddress) to """
            {
                "type": "sign_payload_request",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "signingType": "raw",
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
    ): Pair<BroadcastV2BeaconRequest, String> =
        BroadcastV2BeaconRequest(version, id, senderId, network, signedTransaction) to """
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
        network: Network = Network.Custom(),
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
    ): Pair<OperationV2BeaconResponse, String> =
        OperationV2BeaconResponse(version, id, senderId, transactionHash) to """
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
    ): Pair<SignPayloadV2BeaconResponse, String> =
        SignPayloadV2BeaconResponse(version, id, senderId, signingType, signature) to """
            {
                "type": "sign_payload_response",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "signingType": "raw",
                "signature": "$signature"
            }
        """.trimIndent()

    private fun createBroadcastResponseJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash"
    ): Pair<BroadcastV2BeaconResponse, String> =
        BroadcastV2BeaconResponse(version, id, senderId, transactionHash) to """
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
        network: Network = Network.Custom(),
        scopes: List<Permission.Scope> = emptyList(),
        origin: Origin = Origin.P2P(senderId),
    ): Pair<PermissionV2BeaconRequest, PermissionBeaconRequest> =
        PermissionV2BeaconRequest(version, id, senderId, appMetadata, network, scopes) to
                PermissionBeaconRequest(id, senderId, appMetadata.toAppMetadata(), network, scopes, origin, version)

    private fun createOperationRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: Network = Network.Custom(),
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<OperationV2BeaconRequest, OperationBeaconRequest> =
        OperationV2BeaconRequest(version, id, senderId, network, tezosOperations, sourceAddress) to
                OperationBeaconRequest(id, senderId, appMetadata, network, tezosOperations, sourceAddress, origin, version)

    private fun createSignPayloadRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<SignPayloadV2BeaconRequest, SignPayloadBeaconRequest> =
        SignPayloadV2BeaconRequest(version, id, senderId, signingType, payload, sourceAddress) to
                SignPayloadBeaconRequest(id, senderId, appMetadata, signingType, payload, sourceAddress, origin, version)

    private fun createBroadcastRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: Network = Network.Custom(),
        signedTransaction: String = "signedTransaction",
        appMetadata: AppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<BroadcastV2BeaconRequest, BroadcastBeaconRequest> =
        BroadcastV2BeaconRequest(version, id, senderId, network, signedTransaction) to
                BroadcastBeaconRequest(id, senderId, appMetadata, network, signedTransaction, origin, version)

    // -- response to BeaconMessage --

    private fun createPermissionResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        publicKey: String = "publicKey",
        network: Network = Network.Custom(),
        scopes: List<Permission.Scope> = emptyList(),
        threshold: Threshold? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<PermissionV2BeaconResponse, PermissionBeaconResponse> =
        PermissionV2BeaconResponse(version, id, senderId, publicKey, network, scopes, threshold) to
                PermissionBeaconResponse(id, publicKey, network, scopes, threshold, version, origin)

    private fun createOperationResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<OperationV2BeaconResponse, OperationBeaconResponse> =
        OperationV2BeaconResponse(version, id, senderId, transactionHash) to
                OperationBeaconResponse(id, transactionHash, version, origin)

    private fun createSignPayloadResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        signature: String = "signature",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<SignPayloadV2BeaconResponse, SignPayloadBeaconResponse> =
        SignPayloadV2BeaconResponse(version, id, senderId, signingType, signature) to
                SignPayloadBeaconResponse(id, signingType, signature, version, origin)

    private fun createBroadcastResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<BroadcastV2BeaconResponse, BroadcastBeaconResponse> =
        BroadcastV2BeaconResponse(version, id, senderId, transactionHash) to
                BroadcastBeaconResponse(id, transactionHash, version, origin)

    private fun createErrorResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        errorType: BeaconError = BeaconError.Unknown,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<ErrorV2BeaconResponse, ErrorBeaconResponse> =
        ErrorV2BeaconResponse(version, id, senderId, errorType) to ErrorBeaconResponse(id, errorType, version, origin)

    // -- other to BeaconMessage --

    private fun createDisconnectPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<DisconnectV2BeaconMessage, DisconnectBeaconMessage> =
        DisconnectV2BeaconMessage(version, id, senderId) to DisconnectBeaconMessage(id, senderId, version, origin)
}